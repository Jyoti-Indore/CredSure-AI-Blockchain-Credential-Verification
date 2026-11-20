
package com.credentialverification.backend;

import com.credentialverification.backend.entity.User;
import com.credentialverification.backend.entity.UserRole;
import com.credentialverification.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner createInitialAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword) {

        return args -> {

            String email = adminEmail.trim().toLowerCase();

            if (userRepository.existsByEmailIgnoreCase(email)) {
                System.out.println(
                        "University Admin account already exists."
                );
                return;
            }

            User admin = new User();
            admin.setEmail(email);
            admin.setPasswordHash(
                    passwordEncoder.encode(adminPassword)
            );
            admin.setRole(UserRole.UNIVERSITY_ADMIN);
            admin.setEnabled(true);

            userRepository.save(admin);

            System.out.println(
                    "Initial University Admin account created successfully."
            );
        };
    }
}
