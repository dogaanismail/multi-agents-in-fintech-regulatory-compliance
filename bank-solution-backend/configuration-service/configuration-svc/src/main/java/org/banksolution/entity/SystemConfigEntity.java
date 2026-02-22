package org.banksolution.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.banksolution.enums.ConfigCategory;
import org.banksolution.enums.ConfigType;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "system_config")
@Table(name = "system_config")
public class SystemConfigEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 500)
    private String configValue;

    @Column(name = "config_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ConfigType configType;

    @Column(name = "category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ConfigCategory category;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "default_value", nullable = false, length = 500)
    private String defaultValue;
}
