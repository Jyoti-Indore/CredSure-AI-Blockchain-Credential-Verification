
package com.credentialverification.backend.dto;

public class VerificationResponse {

    private String credentialId;
    private boolean valid;
    private String status;
    private String studentName;
    private String universityName;
    private String degreeName;
    private Integer graduationYear;
    private String message;

    public VerificationResponse(
            String credentialId,
            boolean valid,
            String status,
            String studentName,
            String universityName,
            String degreeName,
            Integer graduationYear,
            String message) {

        this.credentialId = credentialId;
        this.valid = valid;
        this.status = status;
        this.studentName = studentName;
        this.universityName = universityName;
        this.degreeName = degreeName;
        this.graduationYear = graduationYear;
        this.message = message;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public boolean isValid() {
        return valid;
    }

    public String getStatus() {
        return status;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getUniversityName() {
        return universityName;
    }

    public String getDegreeName() {
        return degreeName;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public String getMessage() {
        return message;
    }
}
