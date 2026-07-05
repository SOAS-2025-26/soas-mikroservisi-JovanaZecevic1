package com.soas.bank_account_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soas.bank_account_service.model.BankAccount;
import com.soas.bank_account_service.repository.BankAccountRepository;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import serviceLibrary.dto.bankAccountService.BankAccountDto;
import serviceLibrary.dto.usersService.UserDto;
import serviceLibrary.proxies.usersService.UsersProxy;
import serviceLibrary.services.bankAccountService.BankAccountService;
import util.exceptions.DuplicateResourceException;
import util.exceptions.ResourceNotFoundException;
import util.exceptions.UnauthorizedActionException;

import java.util.List;
import java.util.Optional;

@RestController
public class BankAccountServiceImplementation implements BankAccountService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UsersProxy usersProxy;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ResponseEntity<?> getAllAccounts(String actorRole) {
        if (!ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            throw new UnauthorizedActionException("Only ADMIN can view all bank accounts");
        }

        List<BankAccountDto> accounts = bankAccountRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @Override
    public ResponseEntity<?> getAccountByEmail(String actorRole, String actorEmail, String email) {
        if (!isAuthorizedToView(actorRole, actorEmail, email)) {
            throw new UnauthorizedActionException("You are not authorized to view this bank account");
        }

        List<BankAccountDto> accounts = bankAccountRepository.findByEmail(email).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @Override
    public ResponseEntity<?> createAccount(String actorRole, BankAccountDto body) {
        if (!ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            throw new UnauthorizedActionException("Only ADMIN can create bank accounts");
        }

        assertEmailBelongsToUser(body.getEmail());

        if (bankAccountRepository.existsByEmailAndCurrencyCode(body.getEmail(), body.getCurrencyCode())) {
            throw new DuplicateResourceException("A bank account for this email and currency already exists");
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
            throw new UnauthorizedActionException("Only ADMIN can update bank accounts");
        }

        Optional<BankAccount> existingOpt = bankAccountRepository.findByEmailAndCurrencyCode(body.getEmail(), body.getCurrencyCode());
        if (existingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Bank account for email " + body.getEmail() + " and currency " + body.getCurrencyCode() + " does not exist");
        }

        BankAccount existing = existingOpt.get();
        existing.setAmount(body.getAmount());
        BankAccount saved = bankAccountRepository.save(existing);

        return ResponseEntity.ok(toDto(saved));
    }

    @Override
    public ResponseEntity<?> deleteAccount(String actorRole, String email, String currencyCode) {
        if (!ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            throw new UnauthorizedActionException("Only ADMIN can delete bank accounts");
        }

        Optional<BankAccount> existingOpt = bankAccountRepository.findByEmailAndCurrencyCode(email, currencyCode);
        if (existingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Bank account for email " + email + " and currency " + currencyCode + " does not exist");
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

    private void assertEmailBelongsToUser(String email) {
        UserDto user;
        try {
            Object body = usersProxy.getUserByEmail(ROLE_ADMIN, email).getBody();
            user = objectMapper.convertValue(body, UserDto.class);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("No user exists with email " + email);
        } catch (FeignException e) {
            throw new ResourceNotFoundException("Could not verify user with email " + email);
        }

        if (user == null || !"USER".equalsIgnoreCase(user.getRole())) {
            throw new ResourceNotFoundException(
                    "User with email " + email + " does not have role USER; bank accounts can only be created for USER accounts");
        }
    }
}
