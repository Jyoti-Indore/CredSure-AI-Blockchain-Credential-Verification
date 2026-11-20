
package com.credentialverification.backend.repository;

import com.credentialverification.backend.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    Optional<Credential> findByCredentialId(String credentialId);

    boolean existsByCredentialId(String credentialId);

    // Retrieve only credentials belonging to a particular student.
    List<Credential> findAllByStudentEmailIgnoreCase(String studentEmail);

    // Retrieve a credential only when both its ID and student email match.
    Optional<Credential> findByCredentialIdAndStudentEmailIgnoreCase(
            String credentialId,
            String studentEmail
    );
}
