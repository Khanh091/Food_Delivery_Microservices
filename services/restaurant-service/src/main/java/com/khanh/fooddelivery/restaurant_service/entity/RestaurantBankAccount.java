package com.khanh.fooddelivery.restaurant_service.entity;

import com.khanh.fooddelivery.restaurant_service.common.entity.BaseAuditEntity;
import com.khanh.fooddelivery.restaurant_service.enums.BankAccountVerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurant_bank_accounts")
@Getter
@Setter
@NoArgsConstructor
public class RestaurantBankAccount extends BaseAuditEntity {
    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Column(nullable = false)
    private String bankCode;

    private String bankName;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankAccountVerificationStatus verificationStatus;

    @Version private long version;

    @PrePersist
    void id() {
        if (id == null) id = UUID.randomUUID();
    }
}
