package org.banksolution.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.banksolution.enums.BeneficiaryStatus;
import org.banksolution.enums.BeneficiaryType;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "beneficiary")
@Table(name = "beneficiary")
public class BeneficiaryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BeneficiaryType beneficiaryType;

    @Column(name = "alias")
    private String alias;

    @Column(name = "company_name", length = 140)
    private String companyName;

    @Column(name = "first_name", length = 70)
    private String firstName;

    @Column(name = "last_name", length = 70)
    private String lastName;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BeneficiaryStatus beneficiaryStatus = BeneficiaryStatus.ACTIVE;

    @Builder.Default
    @EqualsAndHashCode.Exclude
    @OrderBy("createdAt ASC")
    @OneToMany(mappedBy = "beneficiary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BeneficiaryCoordinateEntity> coordinates = new ArrayList<>();

    public void addBeneficiaryCoordinate(BeneficiaryCoordinateEntity beneficiaryCoordinateEntity) {
        beneficiaryCoordinateEntity.setBeneficiary(this);
        coordinates.add(beneficiaryCoordinateEntity);
    }
}
