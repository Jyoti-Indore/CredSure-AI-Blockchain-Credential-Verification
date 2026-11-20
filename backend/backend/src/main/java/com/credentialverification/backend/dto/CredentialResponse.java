
package com.credentialverification.backend.dto;

import java.time.LocalDate;

public class CredentialResponse {

    private String credentialId;
    private String studentName;
    private String studentEmail;
    private String universityName;
    private String degreeName;
    private String department;
    private Integer graduationYear;
    private LocalDate issueDate;
    private String status;
    private String certificateHash;
    private String blockchainTransactionHash;
    private String blockchainRevocationTransactionHash;

    public CredentialResponse(
            String credentialId,
            String studentName,
            String studentEmail,
            String universityName,
            String degreeName,
            String department,
            Integer graduationYear,
            LocalDate issueDate,
            String status,
            String certificateHash,
            String blockchainTransactionHash,
            String blockchainRevocationTransactionHash) {

        this.credentialId = credentialId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.universityName = universityName;
        this.degreeName = degreeName;
        this.department = department;
        this.graduationYear = graduationYear;
        this.issueDate = issueDate;
        this.status = status;
        this.certificateHash = certificateHash;
        this.blockchainTransactionHash = blockchainTransactionHash;
        this.blockchainRevocationTransactionHash =
                blockchainRevocationTransactionHash;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public String getUniversityName() {
        return universityName;
    }

    public String getDegreeName() {
        return degreeName;
    }

    public String getDepartment() {
        return department;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public String getStatus() {
        return status;
    }

    public String getCertificateHash() {
        return certificateHash;
    }

    public String getBlockchainTransactionHash() {
        return blockchainTransactionHash;
    }

    public String getBlockchainRevocationTransactionHash() {
        return blockchainRevocationTransactionHash;
    }
}
