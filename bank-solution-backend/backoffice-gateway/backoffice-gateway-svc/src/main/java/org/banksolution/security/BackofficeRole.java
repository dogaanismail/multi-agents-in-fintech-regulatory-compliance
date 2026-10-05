package org.banksolution.security;

public enum BackofficeRole {

    VIEWER("viewer"),
    OPERATOR("operator"),
    COMPLIANCE_OFFICER("compliance-officer"),
    ADMIN("admin");

    private final String keycloakRoleName;

    BackofficeRole(String keycloakRoleName) {
        this.keycloakRoleName = keycloakRoleName;
    }

    public String keycloakRoleName() {
        return keycloakRoleName;
    }
}
