package com.soas.bank_account_service.repository;

import com.soas.bank_account_service.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByEmail(String email);

    Optional<BankAccount> findByEmailAndCurrencyCode(String email, String currencyCode);

    boolean existsByEmailAndCurrencyCode(String email, String currencyCode);

}
