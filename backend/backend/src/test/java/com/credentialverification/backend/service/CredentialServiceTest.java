
package com.credentialverification.backend.service;

import com.credentialverification.backend.dto.VerificationResponse;
import com.credentialverification.backend.entity.Credential;
import com.credentialverification.backend.repository.CredentialRepository;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private BlockchainService blockchainService;

    @InjectMocks
    private CredentialService credentialService;

    private Credential createTestCredential(String status) {

        Credential credential = new Credential();

        credential.setCredentialId("TEST-CRED-001");
        credential.setStudentName("Test Student");
        credential.setStudentEmail("test@example.com");
        credential.setUniversityName("Demo University");
        credential.setDegreeName("Bachelor of Technology");
        credential.setDepartment("Computer Science");
        credential.setGraduationYear(2026);
        credential.setStatus(status);

        return credential;
    }

    /*
     * Generates the same SHA-256 fingerprint as CredentialService.
     */
    private String generateTestCertificateHash(Credential credential)
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

    private Map<String, Object> createBlockchainStatus(
            boolean exists,
            boolean revoked,
            String certificateHash) {

        return Map.of(
                "exists", exists,
                "revoked", revoked,
                "certificateHash", certificateHash,
                "credentialId", "TEST-CRED-001",
                "issuedBy", "0x1234567890",
                "issuedAt", "1795006950"
        );
    }

    @Test
    void createCredentialShouldSaveNewCredential() {

        Credential credential = createTestCredential("PENDING");

        when(credentialRepository.existsByCredentialId(
                "TEST-CRED-001")).thenReturn(false);

        when(credentialRepository.save(credential))
                .thenReturn(credential);

        Credential result =
                credentialService.createCredential(credential);

        assertNotNull(result);
        assertEquals("TEST-CRED-001", result.getCredentialId());

        verify(credentialRepository).save(credential);
    }

    @Test
    void createCredentialShouldRejectDuplicateId() {

        Credential credential = createTestCredential("PENDING");

        when(credentialRepository.existsByCredentialId(
                "TEST-CRED-001")).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.createCredential(credential)
        );

        verify(credentialRepository, never()).save(any());
    }

    @Test
    void getAllCredentialsShouldReturnAllRecords() {

        List<Credential> credentials = List.of(
                createTestCredential("ISSUED"),
                createTestCredential("REVOKED")
        );

        when(credentialRepository.findAll()).thenReturn(credentials);

        List<Credential> result =
                credentialService.getAllCredentials();

        assertEquals(2, result.size());
        verify(credentialRepository).findAll();
    }

    @Test
    void getCredentialByIdShouldReturnExistingCredential() {

        Credential credential = createTestCredential("ISSUED");

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        Credential result =
                credentialService.getCredentialById("TEST-CRED-001");

        assertNotNull(result);
        assertEquals("TEST-CRED-001", result.getCredentialId());
    }

    @Test
    void getCredentialByIdShouldThrowWhenNotFound() {

        when(credentialRepository.findByCredentialId(
                "UNKNOWN-ID")).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> credentialService.getCredentialById("UNKNOWN-ID")
        );
    }

    @Test
    void issueCredentialShouldRegisterOnBlockchainAndSave()
            throws Exception {

        Credential credential = createTestCredential("PENDING");

        String transactionHash = "0x" + "a".repeat(64);

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        when(blockchainService.issueCredentialOnBlockchain(
                eq("TEST-CRED-001"), anyString()))
                .thenReturn(transactionHash);

        when(credentialRepository.save(credential))
                .thenReturn(credential);

        Credential result =
                credentialService.issueCredential("TEST-CRED-001");

        assertEquals("ISSUED", result.getStatus());

        assertNotNull(result.getCertificateHash());
        assertTrue(result.getCertificateHash()
                .matches("[0-9a-f]{64}"));

        assertEquals(
                transactionHash,
                result.getBlockchainTransactionHash()
        );

        verify(blockchainService).issueCredentialOnBlockchain(
                eq("TEST-CRED-001"),
                eq(result.getCertificateHash())
        );

        verify(credentialRepository).save(credential);
    }

    @Test
    void issueCredentialShouldRejectAlreadyIssuedCredential()
            throws Exception {

        Credential credential = createTestCredential("ISSUED");

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        assertThrows(
                IllegalStateException.class,
                () -> credentialService.issueCredential("TEST-CRED-001")
        );

        verify(blockchainService, never())
                .issueCredentialOnBlockchain(anyString(), anyString());

        verify(credentialRepository, never()).save(any());
    }

    @Test
    void issueCredentialShouldRejectRevokedCredential()
            throws Exception {

        Credential credential = createTestCredential("REVOKED");

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        assertThrows(
                IllegalStateException.class,
                () -> credentialService.issueCredential("TEST-CRED-001")
        );

        verify(blockchainService, never())
                .issueCredentialOnBlockchain(anyString(), anyString());

        verify(credentialRepository, never()).save(any());
    }

    @Test
    void issueCredentialShouldNotSaveWhenBlockchainFails()
            throws Exception {

        Credential credential = createTestCredential("PENDING");

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        when(blockchainService.issueCredentialOnBlockchain(
                eq("TEST-CRED-001"), anyString()))
                .thenThrow(new Exception("Blockchain unavailable"));

        assertThrows(
                Exception.class,
                () -> credentialService.issueCredential("TEST-CRED-001")
        );

        assertEquals("PENDING", credential.getStatus());

        verify(credentialRepository, never()).save(any());
    }

    @Test
    void revokeCredentialShouldRevokeOnBlockchainAndSave()
            throws Exception {

        Credential credential = createTestCredential("ISSUED");

        String transactionHash = "0x" + "b".repeat(64);

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        when(blockchainService.revokeCredentialOnBlockchain(
                "TEST-CRED-001")).thenReturn(transactionHash);

        when(credentialRepository.save(credential))
                .thenReturn(credential);

        Credential result =
                credentialService.revokeCredential("TEST-CRED-001");

        assertEquals("REVOKED", result.getStatus());

        assertEquals(
                transactionHash,
                result.getBlockchainRevocationTransactionHash()
        );

        verify(blockchainService)
                .revokeCredentialOnBlockchain("TEST-CRED-001");

        verify(credentialRepository).save(credential);
    }

    @Test
    void revokeCredentialShouldRejectPendingCredential()
            throws Exception {

        Credential credential = createTestCredential("PENDING");

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        assertThrows(
                IllegalStateException.class,
                () -> credentialService.revokeCredential("TEST-CRED-001")
        );

        verify(blockchainService, never())
                .revokeCredentialOnBlockchain(anyString());

        verify(credentialRepository, never()).save(any());
    }

    @Test
    void revokeCredentialShouldNotSaveWhenBlockchainFails()
            throws Exception {

        Credential credential = createTestCredential("ISSUED");

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        when(blockchainService.revokeCredentialOnBlockchain(
                "TEST-CRED-001"))
                .thenThrow(new Exception("Blockchain unavailable"));

        assertThrows(
                Exception.class,
                () -> credentialService.revokeCredential("TEST-CRED-001")
        );

        assertEquals("ISSUED", credential.getStatus());

        verify(credentialRepository, never()).save(any());
    }

    @Test
    void verifyCredentialShouldReturnValidForIssuedCredential()
            throws Exception {

        Credential credential = createTestCredential("ISSUED");

        String certificateHash =
                generateTestCertificateHash(credential);

        credential.setCertificateHash(certificateHash);

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        when(blockchainService.getOnChainCredentialStatus(
                "TEST-CRED-001"))
                .thenReturn(createBlockchainStatus(
                        true,
                        false,
                        "0x" + certificateHash
                ));

        VerificationResponse result =
                credentialService.verifyCredential("TEST-CRED-001");

        assertTrue(result.isValid());
        assertEquals("ISSUED", result.getStatus());
        assertEquals("TEST-CRED-001", result.getCredentialId());

        verify(blockchainService)
                .getOnChainCredentialStatus("TEST-CRED-001");
    }

    @Test
    void verifyCredentialShouldReturnInvalidForRevokedCredential()
            throws Exception {

        Credential credential = createTestCredential("REVOKED");

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        VerificationResponse result =
                credentialService.verifyCredential("TEST-CRED-001");

        assertFalse(result.isValid());
        assertEquals("REVOKED", result.getStatus());

        verify(blockchainService, never())
                .getOnChainCredentialStatus(anyString());
    }

    @Test
    void verifyCredentialShouldReturnNotFoundForUnknownId()
            throws Exception {

        when(credentialRepository.findByCredentialId(
                "UNKNOWN-ID")).thenReturn(Optional.empty());

        VerificationResponse result =
                credentialService.verifyCredential("UNKNOWN-ID");

        assertFalse(result.isValid());
        assertEquals("NOT_FOUND", result.getStatus());
        assertEquals("Credential ID not found.", result.getMessage());

        verify(blockchainService, never())
                .getOnChainCredentialStatus(anyString());
    }

    @Test
    void verifyCredentialShouldRejectCredentialNotOnBlockchain()
            throws Exception {

        Credential credential = createTestCredential("ISSUED");

        String certificateHash =
                generateTestCertificateHash(credential);

        credential.setCertificateHash(certificateHash);

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        when(blockchainService.getOnChainCredentialStatus(
                "TEST-CRED-001"))
                .thenReturn(createBlockchainStatus(
                        false,
                        false,
                        "0x" + certificateHash
                ));

        VerificationResponse result =
                credentialService.verifyCredential("TEST-CRED-001");

        assertFalse(result.isValid());
        assertEquals("NOT_ON_BLOCKCHAIN", result.getStatus());
    }

    @Test
    void verifyCredentialShouldRejectBlockchainRevokedCredential()
            throws Exception {

        Credential credential = createTestCredential("ISSUED");

        String certificateHash =
                generateTestCertificateHash(credential);

        credential.setCertificateHash(certificateHash);

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        when(blockchainService.getOnChainCredentialStatus(
                "TEST-CRED-001"))
                .thenReturn(createBlockchainStatus(
                        true,
                        true,
                        "0x" + certificateHash
                ));

        VerificationResponse result =
                credentialService.verifyCredential("TEST-CRED-001");

        assertFalse(result.isValid());
        assertEquals("REVOKED", result.getStatus());
    }

    @Test
    void verifyCredentialShouldRejectDatabaseHashMismatch()
            throws Exception {

        Credential credential = createTestCredential("ISSUED");

        String certificateHash =
                generateTestCertificateHash(credential);

        credential.setCertificateHash("0".repeat(64));

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        when(blockchainService.getOnChainCredentialStatus(
                "TEST-CRED-001"))
                .thenReturn(createBlockchainStatus(
                        true,
                        false,
                        "0x" + certificateHash
                ));

        VerificationResponse result =
                credentialService.verifyCredential("TEST-CRED-001");

        assertFalse(result.isValid());
        assertEquals("HASH_MISMATCH", result.getStatus());
    }

    @Test
    void verifyCredentialShouldRejectBlockchainHashMismatch()
            throws Exception {

        Credential credential = createTestCredential("ISSUED");

        String certificateHash =
                generateTestCertificateHash(credential);

        credential.setCertificateHash(certificateHash);

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        when(blockchainService.getOnChainCredentialStatus(
                "TEST-CRED-001"))
                .thenReturn(createBlockchainStatus(
                        true,
                        false,
                        "0x" + "f".repeat(64)
                ));

        VerificationResponse result =
                credentialService.verifyCredential("TEST-CRED-001");

        assertFalse(result.isValid());
        assertEquals("HASH_MISMATCH", result.getStatus());
    }

    @Test
    void verifyCredentialShouldRejectAlteredCredentialData()
            throws Exception {

        Credential credential = createTestCredential("ISSUED");

        String originalHash =
                generateTestCertificateHash(credential);

        credential.setCertificateHash(originalHash);

        // Simulate someone changing the stored student data.
        credential.setStudentName("Altered Student Name");

        when(credentialRepository.findByCredentialId(
                "TEST-CRED-001")).thenReturn(Optional.of(credential));

        when(blockchainService.getOnChainCredentialStatus(
                "TEST-CRED-001"))
                .thenReturn(createBlockchainStatus(
                        true,
                        false,
                        "0x" + originalHash
                ));

        VerificationResponse result =
                credentialService.verifyCredential("TEST-CRED-001");

        assertFalse(result.isValid());
        assertEquals("HASH_MISMATCH", result.getStatus());
    }
}
