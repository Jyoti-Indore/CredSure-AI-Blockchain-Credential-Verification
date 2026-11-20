
package com.credentialverification.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;

import org.web3j.crypto.Hash;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;

import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class BlockchainService {

    private final Web3j web3j;

    @Value("${blockchain.contract-address}")
    private String contractAddress;

    private static final BigInteger GAS_LIMIT =
            BigInteger.valueOf(500000);

    public BlockchainService(Web3j web3j) {
        this.web3j = web3j;
    }

    public String getBlockchainVersion() throws Exception {

        Web3ClientVersion version =
                web3j.web3ClientVersion().send();

        return version.getWeb3ClientVersion();
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public Map<String, Object> getOnChainCredentialStatus(
            String credentialId) throws Exception {

        byte[] credentialIdHash =
                Hash.sha3(
                        credentialId.getBytes(StandardCharsets.UTF_8)
                );

        Function function = new Function(
                "getCredentialStatus",
                Arrays.asList(new Bytes32(credentialIdHash)),
                Arrays.asList(
                        new TypeReference<Bool>() {},
                        new TypeReference<Bool>() {},
                        new TypeReference<Bytes32>() {},
                        new TypeReference<Address>() {},
                        new TypeReference<Uint256>() {}
                )
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        null,
                        contractAddress,
                        encodedFunction
                ),
                DefaultBlockParameterName.LATEST
        ).send();

        if (response.hasError()) {
            throw new IllegalStateException(
                    "Blockchain query failed: "
                            + response.getError().getMessage()
            );
        }

        List<Type> results =
                FunctionReturnDecoder.decode(
                        response.getValue(),
                        function.getOutputParameters()
                );

        if (results.size() != 5) {
            throw new IllegalStateException(
                    "Unexpected response from the blockchain contract."
            );
        }

        boolean exists =
                (Boolean) results.get(0).getValue();

        boolean revoked =
                (Boolean) results.get(1).getValue();

        String certificateHash =
                Numeric.toHexString(
                        (byte[]) results.get(2).getValue()
                );

        String issuedBy =
                results.get(3).getValue().toString();

        String issuedAt =
                results.get(4).getValue().toString();

        return Map.of(
                "credentialId", credentialId,
                "exists", exists,
                "revoked", revoked,
                "certificateHash", certificateHash,
                "issuedBy", issuedBy,
                "issuedAt", issuedAt
        );
    }

    public String issueCredentialOnBlockchain(
            String credentialId,
            String certificateHash) throws Exception {

        if (certificateHash == null ||
                !certificateHash.matches("(?i)[0-9a-f]{64}")) {

            throw new IllegalArgumentException(
                    "Certificate hash must contain exactly 64 hexadecimal characters."
            );
        }

        byte[] credentialIdHash =
                Hash.sha3(
                        credentialId.getBytes(StandardCharsets.UTF_8)
                );

        byte[] certificateHashBytes =
                Numeric.hexStringToByteArray(certificateHash);

        Function function = new Function(
                "issueCredential",
                Arrays.asList(
                        new Bytes32(credentialIdHash),
                        new Bytes32(certificateHashBytes)
                ),
                Collections.emptyList()
        );

        return sendBlockchainTransaction(function);
    }

    public String revokeCredentialOnBlockchain(
            String credentialId) throws Exception {

        byte[] credentialIdHash =
                Hash.sha3(
                        credentialId.getBytes(StandardCharsets.UTF_8)
                );

        Function function = new Function(
                "revokeCredential",
                Collections.singletonList(
                        new Bytes32(credentialIdHash)
                ),
                Collections.emptyList()
        );

        return sendBlockchainTransaction(function);
    }

    private String sendBlockchainTransaction(
            Function function) throws Exception {

        EthAccounts accountsResponse =
                web3j.ethAccounts().send();

        if (accountsResponse.hasError() ||
                accountsResponse.getAccounts() == null ||
                accountsResponse.getAccounts().isEmpty()) {

            throw new IllegalStateException(
                    "No unlocked Hardhat account is available. Check that the local Hardhat node is running."
            );
        }

        // The first Hardhat account is the contract deployer.
        String fromAddress =
                accountsResponse.getAccounts().get(0);

        EthGetTransactionCount nonceResponse =
                web3j.ethGetTransactionCount(
                        fromAddress,
                        DefaultBlockParameterName.PENDING
                ).send();

        EthGasPrice gasPriceResponse =
                web3j.ethGasPrice().send();

        if (nonceResponse.hasError() ||
                gasPriceResponse.hasError()) {

            throw new IllegalStateException(
                    "Unable to retrieve blockchain transaction details."
            );
        }

        String encodedFunction =
                FunctionEncoder.encode(function);

        Transaction transaction =
                Transaction.createFunctionCallTransaction(
                        fromAddress,
                        nonceResponse.getTransactionCount(),
                        gasPriceResponse.getGasPrice(),
                        GAS_LIMIT,
                        contractAddress,
                        BigInteger.ZERO,
                        encodedFunction
                );

        EthSendTransaction transactionResponse =
                web3j.ethSendTransaction(transaction).send();

        if (transactionResponse.hasError()) {
            throw new IllegalStateException(
                    "Blockchain transaction failed: "
                            + transactionResponse.getError().getMessage()
            );
        }

        String transactionHash =
                transactionResponse.getTransactionHash();

        if (transactionHash == null ||
                transactionHash.isBlank()) {

            throw new IllegalStateException(
                    "Blockchain did not return a transaction hash."
            );
        }

        // Wait briefly for the local Hardhat node to mine the transaction.
        TransactionReceipt receipt = null;

        for (int attempt = 0; attempt < 20; attempt++) {

            EthGetTransactionReceipt receiptResponse =
                    web3j.ethGetTransactionReceipt(
                            transactionHash
                    ).send();

            if (receiptResponse.getTransactionReceipt().isPresent()) {

                receipt = receiptResponse
                        .getTransactionReceipt()
                        .get();

                break;
            }

            Thread.sleep(250);
        }

        if (receipt == null) {
            throw new IllegalStateException(
                    "Transaction was submitted, but its receipt was not available in time. Transaction hash: "
                            + transactionHash
            );
        }

        if (!"0x1".equalsIgnoreCase(receipt.getStatus())) {
            throw new IllegalStateException(
                    "Blockchain transaction was mined but failed. Transaction hash: "
                            + transactionHash
            );
        }

        return transactionHash;
    }
}
