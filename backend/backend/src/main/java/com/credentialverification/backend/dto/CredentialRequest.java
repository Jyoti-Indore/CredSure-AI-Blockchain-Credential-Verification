
package com.credentialverification.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CredentialRequest {

    @NotBlank(message = "Credential ID is required.")
    private String credentialId;

    @NotBlank(message = "Student name is required.")
    private String studentName;

    @NotBlank(message = "Student email is required.")
    @Email(message = "Please provide a valid email address.")
    private String studentEmail;

    @NotBlank(message = "University name is required.")
    private String universityName;

    @NotBlank(message = "Degree name is required.")
    private String degreeName;

    private String department;

    @NotNull(message = "Graduation year is required.")
    @Min(value = 1900, message = "Graduation year must be 1900 or later.")
    @Max(value = 2100, message = "Graduation year must be 2100 or earlier.")
    private Integer graduationYear;

    private LocalDate issueDate;

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
}
