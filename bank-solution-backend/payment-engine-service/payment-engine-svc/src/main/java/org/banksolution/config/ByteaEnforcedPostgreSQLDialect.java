package org.banksolution.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

/**
 * Custom PostgreSQL dialect to enforce BYTEA type for @Lob columns instead of OID.
 * This prevents PostgreSQL from using Large Object Storage which has performance overhead.
 *
 * @see <a href="https://docs.axoniq.io/reference-guide/axon-framework/tuning/event-processing">Axon Framework Event Processing Tuning</a>
 */
public class ByteaEnforcedPostgreSQLDialect extends PostgreSQLDialect {

    public ByteaEnforcedPostgreSQLDialect() {
        super();
    }

    @Override
    protected String columnType(int sqlTypeCode) {
        // Force BLOB types to use BYTEA instead of OID
        return sqlTypeCode == SqlTypes.BLOB ? "bytea" : super.columnType(sqlTypeCode);
    }

    @Override
    protected String castType(int sqlTypeCode) {
        return sqlTypeCode == SqlTypes.BLOB ? "bytea" : super.castType(sqlTypeCode);
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);

        // Register BLOB type descriptor as BYTEA
        JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
        jdbcTypeRegistry.addDescriptor(Types.BLOB, BinaryJdbcType.INSTANCE);
    }
}
