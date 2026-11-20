
package com.credentialverification.backend.controller;

import com.credentialverification.backend.dto.CredentialRequest;
import com.credentialverification.backend.dto.CredentialResponse;
import com.credentialverification.backend.dto.VerificationResponse;
import com.credentialverification.backend.entity.Credential;
import com.credentialverification.backend.service.BlockchainService;
import com.credentialverification.backend.service.CredentialService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credentials")
@Tag(
    name = "Credential Management",
    description = "APIs for creating, issuing, revoking, retrieving, and verifying academic credentials."
)
public class CredentialController {

    private final CredentialService credentialService;
    private final BlockchainService blockchainService;

    public CredentialController(
            CredentialService credentialService,
            BlockchainService blockchainService) {

        this.credentialService = credentialService;
        this.blockchainService = blockchainService;
    }

    @GetMapping("/status")
    @Operation(
        summary = "Check backend status",
        description = "Checks whether the credential verification backend is running."
    )
    public String getStatus() {
        return "Credential Verification Backend is running!";
    }

    @GetMapping("/blockchain/status")
    @Operation(
        summary = "Check blockchain connection",
        description = "Checks the connection to the local Hardhat blockchain and returns the deployed contract address."
    )
    public Map<String, String> getBlockchainStatus() throws Exception {

        String version = blockchainService.getBlockchainVersion();

        return Map.of(
            "status", "Connected",
            "blockchainVersion", version,
            "contractAddress", blockchainService.getContractAddress()
        );
    }

    @GetMapping("/blockchain/{credentialId}")
    @Operation(
        summary = "Get credential status from blockchain",
        description = "Reads the existence, revocation status, certificate hash, issuer address, and issue timestamp of a credential directly from the smart contract."
    )
    public Map<String, Object> getOnChainCredentialStatus(
            @PathVariable String credentialId) throws Exception {

        return blockchainService.getOnChainCredentialStatus(credentialId);
    }

    @PostMapping
    @Operation(
        summary = "Create a new credential",
        description = "Creates a new academic credential using student, university, and degree information."
    )
    public CredentialResponse createCredential(
            @Valid @RequestBody CredentialRequest request) {

        Credential credential = new Credential();

        credential.setCredentialId(request.getCredentialId());
        credential.setStudentName(request.getStudentName());
        credential.setStudentEmail(request.getStudentEmail());
        credential.setUniversityName(request.getUniversityName());
        credential.setDegreeName(request.getDegreeName());
        credential.setDepartment(request.getDepartment());
        credential.setGraduationYear(request.getGraduationYear());
        credential.setIssueDate(request.getIssueDate());

        Credential savedCredential =
                credentialService.createCredential(credential);

        return convertToResponse(savedCredential);
    }

    @GetMapping
    @Operation(
        summary = "Get all credentials",
        description = "Retrieves the list of all academic credentials stored in the database. University admins only."
    )
    public List<CredentialResponse> getAllCredentials() {

        return credentialService.getAllCredentials()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @GetMapping("/my")
    @Operation(
        summary = "Get my credentials",
        description = "Retrieves only the credentials belonging to the authenticated student."
    )
    public List<CredentialResponse> getMyCredentials(
            Authentication authentication) {

        return credentialService.getStudentCredentials(
                        authentication.getName()
                )
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @GetMapping("/{credentialId}")
    @Operation(
        summary = "Get credential by ID",
        description = "University admins can retrieve any credential. Students can retrieve only their own credentials."
    )
    public CredentialResponse getCredentialById(
            @PathVariable String credentialId,
            Authentication authentication) {

        boolean isUniversityAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_UNIVERSITY_ADMIN")
                );

        Credential credential;

        if (isUniversityAdmin) {
            credential = credentialService.getCredentialById(credentialId);
        } else {
            credential = credentialService.getCredentialByIdForStudent(
                    credentialId,
                    authentication.getName()
            );
        }

        return convertToResponse(credential);
    }

    @PutMapping("/{credentialId}/issue")
    @Operation(
        summary = "Issue an academic credential",
        description = "Registers the credential on the blockchain and updates its status to ISSUED."
    )
    public CredentialResponse issueCredential(
            @PathVariable String credentialId) throws Exception {

        Credential credential =
                credentialService.issueCredential(credentialId);

        return convertToResponse(credential);
    }

    @PutMapping("/{credentialId}/revoke")
    @Operation(
        summary = "Revoke an academic credential",
        description = "Revokes the credential on the blockchain and updates its status to REVOKED."
    )
    public CredentialResponse revokeCredential(
            @PathVariable String credentialId) throws Exception {

        Credential credential =
                credentialService.revokeCredential(credentialId);

        return convertToResponse(credential);
    }

    @GetMapping("/verify/{credentialId}")
    @Operation(
        summary = "Verify an academic credential",
        description = "Checks whether a credential exists and is currently issued and active."
    )
    public VerificationResponse verifyCredential(
            @PathVariable String credentialId) {

        return credentialService.verifyCredential(credentialId);
    }

    private CredentialResponse convertToResponse(Credential credential) {

        return new CredentialResponse(
                credential.getCredentialId(),
                credential.getStudentName(),
                credential.getStudentEmail(),
                credential.getUniversityName(),
                credential.getDegreeName(),
                credential.getDepartment(),
                credential.getGraduationYear(),
                credential.getIssueDate(),
                credential.getStatus(),
                credential.getCertificateHash(),
                credential.getBlockchainTransactionHash(),
                credential.getBlockchainRevocationTransactionHash()
        );
    }
}
