package com.soas.bank_account_service.service;

import com.soas.bank_account_service.model.BankAccount;
import com.soas.bank_account_service.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import serviceLibrary.dto.bankAccountService.BankAccountDto;
import serviceLibrary.services.bankAccountService.BankAccountService;

import java.util.List;
import java.util.Optional;

@RestController
public class BankAccountServiceImplementation implements BankAccountService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Override
    public ResponseEntity<?> getAllAccounts(String actorRole) {
        if (!ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only ADMIN can view all bank accounts");
        }

        List<BankAccountDto> accounts = bankAccountRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @Override
    public ResponseEntity<?> getAccountByEmail(String actorRole, String actorEmail, String email) {
        if (!isAuthorizedToView(actorRole, actorEmail, email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to view this bank account");
        }

        List<BankAccountDto> accounts = bankAccountRepository.findByEmail(email).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @Override
    public ResponseEntity<?> createAccount(String actorRole, BankAccountDto body) {
        if (!ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only ADMIN can create bank accounts");
        }

        if (bankAccountRepository.existsByEmailAndCurrencyCode(body.getEmail(), body.getCurrencyCode())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A bank account for this email and currency already exists");
        }

        BankAccount account = new BankAccount();
        account.setEmail(body.getEmail());
        account.setCurrencyCode(body.getCurrencyCode());
        account.setAmount(body.getAmount());
        BankAccount saved = bankAccountRepository.save(account);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @Override
    public ResponseEntity<?> updateAccount(String actorRole, BankAccountDto body) {
        if (!ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only ADMIN can update bank accounts");
        }

        Optional<BankAccount> existingOpt = bankAccountRepository.findByEmailAndCurrencyCode(body.getEmail(), body.getCurrencyCode());
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Bank account for email " + body.getEmail() + " and currency " + body.getCurrencyCode() + " does not exist");
        }

        BankAccount existing = existingOpt.get();
        existing.setAmount(body.getAmount());
        BankAccount saved = bankAccountRepository.save(existing);

        return ResponseEntity.ok(toDto(saved));
    }

    @Override
    public ResponseEntity<?> deleteAccount(String actorRole, String email, String currencyCode) {
        if (!ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only ADMIN can delete bank accounts");
        }

        Optional<BankAccount> existingOpt = bankAccountRepository.findByEmailAndCurrencyCode(email, currencyCode);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Bank account for email " + email + " and currency " + currencyCode + " does not exist");
        }

        bankAccountRepository.delete(existingOpt.get());
        return ResponseEntity.noContent().build();
    }

    private boolean isAuthorizedToView(String actorRole, String actorEmail, String targetEmail) {
        if (ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            return true;
        }
        if (ROLE_USER.equalsIgnoreCase(actorRole)) {
            return actorEmail != null && actorEmail.equalsIgnoreCase(targetEmail);
        }
        return false;
    }

    private BankAccountDto toDto(BankAccount account) {
        return new BankAccountDto(account.getEmail(), account.getCurrencyCode(), account.getAmount());
    }
}
