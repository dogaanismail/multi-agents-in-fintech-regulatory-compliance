package org.banksolution.fixtures;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class StaffRealmFixtures {

    public static final String PERMISSIONS_CLIENT_ID = "bank-platform";
    private static final Path STAFF_REALM_FILE = Path.of("../../../infrastructure/keycloak/bank-staff-realm.json");

    private StaffRealmFixtures() {
    }

    public static Set<String> readPermissionCatalog() {
        JsonNode permissionRoles = readStaffRealm().path("roles").path("client").path(PERMISSIONS_CLIENT_ID);
        return toNames(permissionRoles);
    }

    public static Set<String> readRolePermissions(String realmRole) {
        JsonNode realmRoleDefinition = StreamSupport.stream(readStaffRealm().path("roles").path("realm").spliterator(), false)
                .filter(role -> role.path("name").asString().equals(realmRole))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No realm role " + realmRole));
        JsonNode permissions = realmRoleDefinition.path("composites").path("client").path(PERMISSIONS_CLIENT_ID);
        return StreamSupport.stream(permissions.spliterator(), false)
                .map(JsonNode::asString)
                .collect(Collectors.toSet());
    }

    private static Set<String> toNames(JsonNode roleDefinitions) {
        return StreamSupport.stream(roleDefinitions.spliterator(), false)
                .map(roleDefinition -> roleDefinition.path("name").asString())
                .collect(Collectors.toSet());
    }

    private static JsonNode readStaffRealm() {
        try {
            return new ObjectMapper().readTree(Files.readString(STAFF_REALM_FILE));
        } catch (IOException readFailure) {
            throw new UncheckedIOException(readFailure);
        }
    }
}
