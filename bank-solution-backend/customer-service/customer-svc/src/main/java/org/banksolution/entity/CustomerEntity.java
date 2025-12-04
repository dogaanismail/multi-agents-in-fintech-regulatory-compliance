package org.banksolution.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.banksolution.entity.enums.CustomerStatus;
import org.banksolution.entity.enums.CustomerType;

import java.time.LocalDate;
import java.util.UUID;

import static org.banksolution.entity.enums.CustomerStatus.ACTIVE;
import static org.banksolution.entity.enums.CustomerType.INDIVIDUAL;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "customer")
@Table(name = "customer")
public class CustomerEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "nationality", nullable = false, length = 2)
    private String nationality;

    @Column(name = "type", nullable = false, length = 50)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private CustomerType customerType = INDIVIDUAL;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private CustomerStatus customerStatus = ACTIVE;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id", unique = true)
    private CustomerAddress address;

}
