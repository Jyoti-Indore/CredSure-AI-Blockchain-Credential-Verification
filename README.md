# CredSure – AI-Powered Blockchain-Based Academic Credential Verification System

CredSure is a full-stack web application designed to simplify and secure academic credential verification using blockchain technology, cryptographic hashing and role-based access control.

The platform allows university administrators to issue and revoke academic credentials, students to access their credentials, and employers to verify academic qualifications.

**Project Status:** In Progress – Core credential management and blockchain verification features implemented.

## Key Features

- Role-based authentication for University Administrators, Students and Employers.
- JWT-based authentication and authorization.
- Secure password hashing using BCrypt.
- Academic credential creation, issuance and revocation.
- SHA-256 hashing for credential integrity verification.
- Blockchain integration using Solidity, Hardhat and Web3j.
- Employer-facing credential verification.
- Student credential dashboard.
- Downloadable digital certificate PDF.
- Live local blockchain connection status.
- REST API documentation using Swagger UI.

## Technology Stack

| Category | Technologies |
|---|---|
| Frontend | React.js, JavaScript, HTML5, CSS3, Vite |
| Backend | Java 21, Spring Boot 4.1.2 |
| Database | PostgreSQL 17, Spring Data JPA |
| Security | Spring Security, JWT, BCrypt |
| Blockchain | Solidity, Hardhat, Web3j |
| Cryptography | SHA-256 |
| API Documentation | Swagger UI / OpenAPI |
| Build Tools | Maven, npm |
| Development | Visual Studio Code, Git |

## System Architecture

The application follows a layered architecture:

1. **Frontend:** React.js provides role-based dashboards and user interfaces.
2. **Backend:** Spring Boot handles authentication, credential management and verification APIs.
3. **Database:** PostgreSQL stores user accounts and academic credential records.
4. **Blockchain:** A Solidity smart contract stores credential hashes and issuance/revocation information.
5. **Blockchain Integration:** Web3j enables communication between the Java backend and the local Hardhat network.
6. **Planned AI Module:** OCR-based document extraction and suspicious-document analysis are planned for a future development phase.

## User Roles

### University Administrator
- Manage academic credentials.
- Create and issue credentials.
- Revoke issued credentials.
- Monitor blockchain connection status.

### Student
- Register and log in.
- View associated academic credentials.
- Access credential details.
- Download a certificate PDF.

### Employer / Recruiter
- Log in to the verification portal.
- Submit a credential ID.
- View credential verification results.
- Identify active, revoked and nonexistent credentials.

## Blockchain Verification Workflow

1. The university administrator creates a credential.
2. Credential details are stored in PostgreSQL.
3. The backend generates a SHA-256 hash.
4. The credential is issued through a Solidity smart contract.
5. The blockchain records the credential hash and status.
6. An employer submits a credential ID for verification.
7. The backend checks the database and blockchain records.
8. The system checks the revocation status and displays the result.

Personal academic information is maintained in PostgreSQL rather than being stored directly on the blockchain.

## Project Structure

```text
CredSure/
├── backend/
│   └── backend/
│       ├── src/
│       │   ├── main/
│       │   └── test/
│       ├── pom.xml
│       └── mvnw.cmd
│
├── blockchain/
│   ├── contracts/
│   ├── ignition/
│   ├── test/
│   ├── hardhat.config.ts
│   ├── package.json
│   └── package-lock.json
│
├── frontend/
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── package-lock.json
│
├── screenshots/
├── diagrams/
├── .gitignore
└── README.md
```

## Local Development Setup

### Prerequisites

- Java 21
- Node.js and npm
- PostgreSQL
- Git
- Hardhat development environment

### 1. Clone the Repository

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
```

### 2. Configure PostgreSQL

Create a local PostgreSQL database and configure the database connection in your local Spring Boot configuration.

Use your own local credentials. Never commit passwords or secret values.

### 3. Start the Local Blockchain

Open a terminal in the blockchain directory and install the dependencies:

```bash
npm install
```

Start the local Hardhat network:

```bash
npx hardhat node
```

Deploy the smart contract using the project's configured deployment module. Update the local backend configuration with the deployed contract address.

### 4. Start the Backend

Open a terminal in:

```text
backend/backend
```

Run:

```bash
mvnw.cmd spring-boot:run
```

Backend:

```text
http://localhost:8080
```

Swagger documentation:

```text
http://localhost:8080/swagger-ui.html
```

### 5. Start the Frontend

Open a terminal in the frontend directory:

```bash
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

**Note:** The local Hardhat node must remain running while using blockchain-dependent backend functionality. Restarting the local node resets its in-memory blockchain, so the smart contract must be deployed again.

## API Documentation

The backend provides REST APIs for authentication, credential management, credential verification and blockchain status.

Swagger UI provides interactive API documentation and testing.

## Testing

The backend currently has 21 successful automated tests:

- 1 application context test.
- 20 credential service tests.
- 0 failures.
- 0 errors.
- 0 skipped.

Additional manual testing has covered registration, login, authorization, credential issuance, revocation and employer verification.

## Current Implementation Status

### Implemented
- React frontend and role-based dashboards.
- Spring Boot REST APIs.
- PostgreSQL integration.
- JWT authentication and role-based authorization.
- Credential creation, issuance and revocation.
- Solidity smart contract and local Hardhat integration.
- SHA-256 hashing.
- Blockchain-backed credential verification.
- Student credential viewing and PDF generation.
- Swagger API documentation.

### Planned
- AI-powered OCR document extraction.
- Suspicious document inconsistency analysis.
- QR code generation.
- Shareable credential verification links.
- Additional end-to-end testing and documentation.

## Limitations

- The current blockchain implementation uses a local Hardhat development network.
- The application uses demonstration data and is not integrated with actual university databases.
- The AI/OCR and QR-sharing features are planned, not yet implemented.
- This is an academic prototype, not a production-ready university credential platform.

## Future Enhancements

- AI-powered academic document analysis.
- QR-based credential verification.
- Shareable verification links.
- Multi-university support.
- Integration with university information systems.
- Further security and performance testing.

## Project Purpose

This project demonstrates the practical integration of full-stack development, Java backend engineering, database management, application security, cryptographic hashing and blockchain technology.
