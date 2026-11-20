
package com.credentialverification.backend.controller;

import com.credentialverification.backend.service.BlockchainService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {

    private final BlockchainService blockchainService;

    public BlockchainController(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBlockchainStatus() {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            String version = blockchainService.getBlockchainVersion();
            String contractAddress = blockchainService.getContractAddress();

            response.put("connected", true);
            response.put("network", "Local Hardhat Network");
            response.put("clientVersion", version);
            response.put("contractAddress", contractAddress);
            response.put("message", "Blockchain node is reachable.");

            return ResponseEntity.ok(response);

        } catch (Exception exception) {

            response.put("connected", false);
            response.put("network", "Local Hardhat Network");
            response.put("message", "Blockchain node is unavailable.");

            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(response);
        }
    }
}
