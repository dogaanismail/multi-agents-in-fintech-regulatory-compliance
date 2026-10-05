package org.banksolution.security;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.StaffRealmFixtures.readPermissionCatalog;
import static org.banksolution.fixtures.StaffRealmFixtures.readRolePermissions;

class PermissionCatalogTest {

    @Test
    void shouldDefineEveryPermissionTheGatewayChecksInTheStaffRealm() {
        Set<String> gatewayPermissions = Arrays.stream(Permission.values())
                .map(Permission::authority)
                .collect(Collectors.toSet());

        assertThat(readPermissionCatalog()).containsExactlyInAnyOrderElementsOf(gatewayPermissions);
    }

    @Test
    void shouldKeepIdentityManagementOutOfEveryBusinessRole() {
        assertThat(readRolePermissions("admin")).doesNotContain(Permission.IAM_MANAGE.authority());
        assertThat(readRolePermissions("super-admin")).containsExactly(Permission.IAM_MANAGE.authority());
    }
}
