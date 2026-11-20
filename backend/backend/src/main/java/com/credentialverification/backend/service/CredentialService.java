
package com.credentialverification.backend.service;

import com.credentialverification.backend.dto.VerificationResponse;
import com.credentialverification.backend.entity.Credential;
import com.credentialverification.backend.repository.CredentialRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final BlockchainService blockchainService;

    public CredentialService(
            CredentialRepository credentialRepository,
            BlockchainService blockchainService) {

        this.credentialRepository = credentialRepository;
        this.blockchainService = blockchainService;
    }

    public Credential createCredential(Credential credential) {

        if (credentialRepository.existsByCredentialId(
                credential.getCredentialId())) {

            throw new IllegalArgumentException(
                    "Credential ID already exists.");
        }

        return credentialRepository.save(credential);
    }

    public List<Credential> getAllCredentials() {
        return credentialRepository.findAll();
    }

    public List<Credential> getStudentCredentials(String studentEmail) {

        return credentialRepository.findAllByStudentEmailIgnoreCase(
                studentEmail
        );
    }

    public Credential getCredentialById(String credentialId) {

        return credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Credential not found: " + credentialId));
    }

    public Credential getCredentialByIdForStudent(
            String credentialId,
            String studentEmail) {

        return credentialRepository
                .findByCredentialIdAndStudentEmailIgnoreCase(
                        credentialId,
                        studentEmail
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "Credential not found: " + credentialId));
    }

    public Credential issueCredential(String credentialId)
            throws Exception {

        Credential credential = getCredentialById(credentialId);

        String currentStatus = credential.getStatus();

        if ("REVOKED".equals(currentStatus)) {
            throw new IllegalStateException(
                    "A revoked credential cannot be issued again.");
        }

        if (!"PENDING".equals(currentStatus)
                && !"ISSUED".equals(currentStatus)) {

            throw new IllegalStateException(
                    "Only pending credentials or issued credentials missing from the blockchain can be issued.");
        }

        String certificateHash = generateCertificateHash(credential);

        // Protect against changes to previously issued credential data.
        if ("ISSUED".equals(currentStatus)) {

            String existingDatabaseHash = credential.getCertificateHash();

            if (existingDatabaseHash == null
                    || !certificateHash.equalsIgnoreCase(
                            existingDatabaseHash)) {

                throw new IllegalStateException(
                        "Credential data does not match its previously stored certificate hash. Re-issuance was stopped.");
            }
        }

        // Always check the current blockchain before submitting a transaction.
        Map<String, Object> blockchainStatus =
                blockchainService.getOnChainCredentialStatus(
                        credential.getCredentialId()
                );

        boolean exists =
                Boolean.TRUE.equals(blockchainStatus.get("exists"));

        boolean revoked =
                Boolean.TRUE.equals(blockchainStatus.get("revoked"));

        if (exists) {

            if (revoked) {
                throw new IllegalStateException(
                        "This credential is already revoked on the blockchain.");
            }

            Object blockchainHashObject =
                    blockchainStatus.get("certificateHash");

            String blockchainHash =
                    blockchainHashObject == null
                            ? null
                            : blockchainHashObject.toString();

            if (blockchainHash != null
                    && blockchainHash.startsWith("0x")) {

                blockchainHash = blockchainHash.substring(2);
            }

            if (blockchainHash == null
                    || !certificateHash.equalsIgnoreCase(blockchainHash)) {

                throw new IllegalStateException(
                        "The existing blockchain certificate hash does not match the credential data. Re-issuance was stopped.");
            }

            if ("PENDING".equals(currentStatus)) {
                throw new IllegalStateException(
                        "This credential already exists on the blockchain, but its database status is PENDING. Please investigate the inconsistent records.");
            }

            // The credential is already correctly registered.
            // Do not create another blockchain transaction.
            return credential;
        }

        // The credential is missing from the current blockchain.
        // This also handles recovery after a local Hardhat reset.
        String transactionHash =
                blockchainService.issueCredentialOnBlockchain(
                        credential.getCredentialId(),
                        certificateHash
                );

        credential.setCertificateHash(certificateHash);
        credential.setBlockchainTransactionHash(transactionHash);
        credential.setBlockchainRevocationTransactionHash(null);
        credential.setStatus("ISSUED");

        return credentialRepository.save(credential);
    }

    public Credential revokeCredential(String credentialId)
            throws Exception {

        Credential credential = getCredentialById(credentialId);

        if (!"ISSUED".equals(credential.getStatus())) {

            throw new IllegalStateException(
                    "Only issued credentials can be revoked.");
        }

        String revocationTransactionHash =
                blockchainService.revokeCredentialOnBlockchain(
                        credential.getCredentialId()
                );

        credential.setBlockchainRevocationTransactionHash(
                revocationTransactionHash
        );

        credential.setStatus("REVOKED");

        return credentialRepository.save(credential);
    }

    private String generateCertificateHash(Credential credential)
            throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        updateDigest(digest, credential.getCredentialId());
        updateDigest(digest, credential.getStudentName());
        updateDigest(digest, credential.getStudentEmail());
        updateDigest(digest, credential.getUniversityName());
        updateDigest(digest, credential.getDegreeName());
        updateDigest(digest, credential.getDepartment());

        updateDigest(
                digest,
                credential.getGraduationYear() == null
                        ? null
                        : credential.getGraduationYear().toString()
        );

        updateDigest(
                digest,
                credential.getIssueDate() == null
                        ? null
                        : credential.getIssueDate().toString()
        );

        return HexFormat.of().formatHex(digest.digest());
    }

    private void updateDigest(MessageDigest digest, String value) {

        if (value == null) {
            digest.update(ByteBuffer.allocate(4).putInt(-1).array());
            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
        digest.update(bytes);
    }

    public VerificationResponse verifyCredential(String credentialId) {

        Credential credential = credentialRepository
                .findByCredentialId(credentialId)
                .orElse(null);

        if (credential == null) {

            return new VerificationResponse(
                    credentialId,
                    false,
                    "NOT_FOUND",
                    null,
                    null,
                    null,
                    null,
                    "Credential ID not found."
            );
        }

        if (!"ISSUED".equals(credential.getStatus())) {

            String message;

            if ("REVOKED".equals(credential.getStatus())) {
                message = "This credential has been revoked.";
            } else {
                message = "This credential has not been issued yet.";
            }

            return createVerificationResponse(
                    credential,
                    false,
                    credential.getStatus(),
                    message
            );
        }

        try {

            Map<String, Object> blockchainStatus =
                    blockchainService.getOnChainCredentialStatus(
                            credentialId
                    );

            boolean exists =
                    Boolean.TRUE.equals(blockchainStatus.get("exists"));

            boolean revoked =
                    Boolean.TRUE.equals(blockchainStatus.get("revoked"));

            if (!exists) {

                return createVerificationResponse(
                        credential,
                        false,
                        "NOT_ON_BLOCKCHAIN",
                        "Credential was not found on the blockchain."
                );
            }

            if (revoked) {

                return createVerificationResponse(
                        credential,
                        false,
                        "REVOKED",
                        "This credential has been revoked on the blockchain."
                );
            }

            String recalculatedHash =
                    generateCertificateHash(credential);

            String databaseHash =
                    credential.getCertificateHash();

            Object blockchainHashObject =
                    blockchainStatus.get("certificateHash");

            String blockchainHash =
                    blockchainHashObject == null
                            ? null
                            : blockchainHashObject.toString();

            if (blockchainHash != null
                    && blockchainHash.startsWith("0x")) {

                blockchainHash = blockchainHash.substring(2);
            }

            boolean databaseHashMatches =
                    databaseHash != null
                            && recalculatedHash.equalsIgnoreCase(databaseHash);

            boolean blockchainHashMatches =
                    blockchainHash != null
                            && recalculatedHash.equalsIgnoreCase(blockchainHash);

            if (!databaseHashMatches || !blockchainHashMatches) {

                return createVerificationResponse(
                        credential,
                        false,
                        "HASH_MISMATCH",
                        "Credential fingerprint mismatch. The credential data may have been altered."
                );
            }

            return createVerificationResponse(
                    credential,
                    true,
                    "ISSUED",
                    "Credential verified successfully. Database and blockchain records match, and the credential is active."
            );

        } catch (Exception exception) {

            return createVerificationResponse(
                    credential,
                    false,
                    "BLOCKCHAIN_UNAVAILABLE",
                    "Unable to verify this credential because the blockchain could not be checked."
            );
        }
    }

    private VerificationResponse createVerificationResponse(
            Credential credential,
            boolean valid,
            String status,
            String message) {

        return new VerificationResponse(
                credential.getCredentialId(),
                valid,
                status,
                credential.getStudentName(),
                credential.getUniversityName(),
                credential.getDegreeName(),
                credential.getGraduationYear(),
                message
        );
    }
}
