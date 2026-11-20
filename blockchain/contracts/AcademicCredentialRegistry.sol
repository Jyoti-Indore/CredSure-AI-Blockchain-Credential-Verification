
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.34;

contract AcademicCredentialRegistry {

    address public universityAdmin;

    struct Credential {
        bytes32 certificateHash;
        address issuedBy;
        uint256 issuedAt;
        bool exists;
        bool revoked;
    }

    mapping(bytes32 => Credential) private credentials;

    event CredentialIssued(
        bytes32 indexed credentialIdHash,
        bytes32 certificateHash,
        address indexed issuedBy,
        uint256 issuedAt
    );

    event CredentialRevoked(
        bytes32 indexed credentialIdHash,
        address indexed revokedBy
    );

    modifier onlyUniversityAdmin() {
        require(
            msg.sender == universityAdmin,
            "Only university admin can perform this action"
        );
        _;
    }

    constructor() {
        universityAdmin = msg.sender;
    }

    function issueCredential(
        bytes32 credentialIdHash,
        bytes32 certificateHash
    ) external onlyUniversityAdmin {

        require(
            credentialIdHash != bytes32(0),
            "Invalid credential ID hash"
        );

        require(
            certificateHash != bytes32(0),
            "Invalid certificate hash"
        );

        require(
            !credentials[credentialIdHash].exists,
            "Credential already registered"
        );

        credentials[credentialIdHash] = Credential({
            certificateHash: certificateHash,
            issuedBy: msg.sender,
            issuedAt: block.timestamp,
            exists: true,
            revoked: false
        });

        emit CredentialIssued(
            credentialIdHash,
            certificateHash,
            msg.sender,
            block.timestamp
        );
    }

    function revokeCredential(
        bytes32 credentialIdHash
    ) external onlyUniversityAdmin {

        require(
            credentials[credentialIdHash].exists,
            "Credential does not exist"
        );

        require(
            !credentials[credentialIdHash].revoked,
            "Credential already revoked"
        );

        credentials[credentialIdHash].revoked = true;

        emit CredentialRevoked(
            credentialIdHash,
            msg.sender
        );
    }

    function verifyCredential(
        bytes32 credentialIdHash,
        bytes32 certificateHash
    ) external view returns (bool) {

        Credential storage credential = credentials[credentialIdHash];

        return (
            credential.exists &&
            !credential.revoked &&
            credential.certificateHash == certificateHash
        );
    }

    function getCredentialStatus(
        bytes32 credentialIdHash
    )
        external
        view
        returns (
            bool exists,
            bool revoked,
            bytes32 certificateHash,
            address issuedBy,
            uint256 issuedAt
        )
    {
        Credential storage credential = credentials[credentialIdHash];

        return (
            credential.exists,
            credential.revoked,
            credential.certificateHash,
            credential.issuedBy,
            credential.issuedAt
        );
    }
}
