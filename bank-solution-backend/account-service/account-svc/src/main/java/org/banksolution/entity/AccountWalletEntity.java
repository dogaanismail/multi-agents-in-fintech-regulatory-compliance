package org.banksolution.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.banksolution.enums.Currency;
import org.banksolution.enums.WalletStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static org.banksolution.enums.WalletStatus.ACTIVE;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "account_wallet")
@Table(name = "account_wallet",
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "currency"}))
public class AccountWalletEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(name = "ledger_account_id", nullable = false, updatable = false)
    private UUID ledgerAccountId;

    @Column(name = "wallet_status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WalletStatus walletStatus = ACTIVE;

    @Column(name = "currency", nullable = false, length = 3)
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

}
