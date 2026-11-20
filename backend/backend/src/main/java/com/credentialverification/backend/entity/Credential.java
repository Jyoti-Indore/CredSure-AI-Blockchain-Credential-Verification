
package com.credentialverification.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "credentials")
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Credential ID is required.")
    @Column(nullable = false, unique = true)
    private String credentialId;

    @NotBlank(message = "Student name is required.")
    @Column(nullable = false)
    private String studentName;

    @NotBlank(message = "Student email is required.")
    @Email(message = "Please provide a valid email address.")
    @Column(nullable = false)
    private String studentEmail;

    @NotBlank(message = "University name is required.")
    @Column(nullable = false)
    private String universityName;

    @NotBlank(message = "Degree name is required.")
    @Column(nullable = false)
    private String degreeName;

    private String department;

    @NotNull(message = "Graduation year is required.")
    @Min(value = 1900, message = "Graduation year must be 1900 or later.")
    @Max(value = 2100, message = "Graduation year must be 2100 or earlier.")
    private Integer graduationYear;

    private LocalDate issueDate;

    @Column(length = 64)
    private String certificateHash;

    // Original blockchain credential issuance transaction
    @Column(length = 100)
    private String blockchainTransactionHash;

    // Separate blockchain credential revocation transaction
    @Column(length = 100)
    private String blockchainRevocationTransactionHash;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Credential() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = "PENDING";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public String getDegreeName() {
        return degreeName;
    }

    public void setDegreeName(String degreeName) {
        this.degreeName = degreeName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public String getCertificateHash() {
        return certificateHash;
    }

    public void setCertificateHash(String certificateHash) {
        this.certificateHash = certificateHash;
    }

    public String getBlockchainTransactionHash() {
        return blockchainTransactionHash;
    }

    public void setBlockchainTransactionHash(String blockchainTransactionHash) {
        this.blockchainTransactionHash = blockchainTransactionHash;
    }

    public String getBlockchainRevocationTransactionHash() {
        return blockchainRevocationTransactionHash;
    }

    public void setBlockchainRevocationTransactionHash(
            String blockchainRevocationTransactionHash) {
        this.blockchainRevocationTransactionHash =
                blockchainRevocationTransactionHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
