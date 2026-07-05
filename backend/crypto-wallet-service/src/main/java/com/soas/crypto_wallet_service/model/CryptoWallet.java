package com.soas.crypto_wallet_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crypto_wallets", uniqueConstraints = @UniqueConstraint(columnNames = {"email", "crypto_currency_code"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CryptoWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "crypto_currency_code", nullable = false)
    private String cryptoCurrencyCode;

    @Column(nullable = false)
    private Double amount;

}
