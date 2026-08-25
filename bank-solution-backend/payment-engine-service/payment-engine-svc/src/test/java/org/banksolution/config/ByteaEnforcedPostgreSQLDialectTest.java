package org.banksolution.config;

import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code contributeTypes} needs Hibernate's bootstrap registries, so it is covered by the
 * integration context (the dialect is the configured {@code hibernate.dialect}).
 */
class ByteaEnforcedPostgreSQLDialectTest {

    private final ByteaEnforcedPostgreSQLDialect byteaEnforcedPostgreSQLDialect = new ByteaEnforcedPostgreSQLDialect() {
        String columnTypeOf(int sqlTypeCode) {
            return columnType(sqlTypeCode);
        }

        String castTypeOf(int sqlTypeCode) {
            return castType(sqlTypeCode);
        }
    };

    @Test
    void shouldMapBlobsToByteaInsteadOfLargeObjects() throws Exception {
        assertThat(invoke("columnTypeOf", SqlTypes.BLOB)).isEqualTo("bytea");
        assertThat(invoke("castTypeOf", SqlTypes.BLOB)).isEqualTo("bytea");
    }

    @Test
    void shouldLeaveEveryOtherTypeToThePostgresDialect() throws Exception {
        assertThat(invoke("columnTypeOf", SqlTypes.VARCHAR)).isEqualTo("varchar($l)");
        assertThat(invoke("castTypeOf", SqlTypes.INTEGER)).isEqualTo("integer");
    }

    private String invoke(String methodName, int sqlTypeCode) throws Exception {
        var method = byteaEnforcedPostgreSQLDialect.getClass().getDeclaredMethod(methodName, int.class);
        method.setAccessible(true);
        return (String) method.invoke(byteaEnforcedPostgreSQLDialect, sqlTypeCode);
    }
}
