package com.soas.bank_account_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bank_accounts", uniqueConstraints = @UniqueConstraint(columnNames = {"email", "currency_code"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(nullable = false)
    private Double amount;

}
