package com.soas.users_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soas.users_service.model.User;
import com.soas.users_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RestController;
import serviceLibrary.dto.bankAccountService.BankAccountDto;
import serviceLibrary.dto.cryptoWalletService.CryptoWalletDto;
import serviceLibrary.dto.usersService.UserDto;
import serviceLibrary.proxies.bankAccountService.BankAccountProxy;
import serviceLibrary.proxies.cryptoWalletService.CryptoWalletProxy;
import serviceLibrary.services.usersService.UsersService;
import util.exceptions.DuplicateResourceException;
import util.exceptions.ResourceNotFoundException;
import util.exceptions.UnauthorizedActionException;

import java.util.List;
import java.util.Optional;

@RestController
public class UsersServiceImplementation implements UsersService {

    private static final Logger log = LoggerFactory.getLogger(UsersServiceImplementation.class);
    private static final String DEFAULT_FIAT_CURRENCY = "EUR";
    private static final String DEFAULT_CRYPTO_CURRENCY = "ETH";

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountProxy bankAccountProxy;

    @Autowired
    private CryptoWalletProxy cryptoWalletProxy;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ResponseEntity<?> getAllUsers(String actorRole) {
        User.Role actor = parseRole(actorRole);
        if (actor == null || actor == User.Role.USER) {
            throw new UnauthorizedActionException("You are not authorized to view users");
        }

        List<UserDto> users = userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    @Override
    public ResponseEntity<?> getUserByEmail(String actorRole, String email) {
        User.Role actor = parseRole(actorRole);
        if (actor == null || actor == User.Role.USER) {
            throw new UnauthorizedActionException("You are not authorized to view users");
        }

        Optional<User> user = userRepository.findByEmailIgnoreCase(email);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User with email " + email + " does not exist");
        }
        return ResponseEntity.ok(toDto(user.get()));
    }

    @Override
    public ResponseEntity<?> validateCredentials(String email, String password) {
        Optional<User> user = userRepository.findByEmailIgnoreCase(email);
        if (user.isEmpty() || !passwordEncoder.matches(password, user.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }
        return ResponseEntity.ok(toDto(user.get()));
    }

    @Override
    public ResponseEntity<?> createUser(String actorRole, UserDto body) {
        boolean bootstrap = userRepository.count() == 0;

        User.Role actor = parseRole(actorRole);
        if (!bootstrap && (actor == null || actor == User.Role.USER)) {
            throw new UnauthorizedActionException("You are not authorized to create users");
        }

        User.Role newRole = parseRole(body.getRole());
        if (newRole == null) {
            return ResponseEntity.badRequest().body("Unknown role: " + body.getRole());
        }

        if (bootstrap && newRole != User.Role.OWNER) {
            throw new UnauthorizedActionException("The first user in the system must be created with role OWNER");
        }

        if (userRepository.existsByEmail(body.getEmail())) {
            throw new DuplicateResourceException("A user with this email already exists");
        }

        if (newRole == User.Role.OWNER && userRepository.existsByRole(User.Role.OWNER)) {
            throw new DuplicateResourceException("An OWNER already exists in the system");
        }

        if (!bootstrap && actor == User.Role.ADMIN && newRole != User.Role.USER) {
            throw new UnauthorizedActionException("ADMIN can only add users with role USER");
        }

        User user = new User();
        user.setEmail(body.getEmail());
        user.setPassword(passwordEncoder.encode(body.getPassword()));
        user.setRole(newRole);
        User saved = userRepository.save(user);

        if (newRole == User.Role.USER) {
            provisionAccounts(saved.getEmail());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @Override
    public ResponseEntity<?> updateUser(String actorRole, UserDto body) {
        User.Role actor = parseRole(actorRole);
        if (actor == null || actor == User.Role.USER) {
            throw new UnauthorizedActionException("You are not authorized to update users");
        }

        Optional<User> existingOpt = userRepository.findByEmail(body.getEmail());
        if (existingOpt.isEmpty()) {
            throw new ResourceNotFoundException("User with email " + body.getEmail() + " does not exist");
        }
        User existing = existingOpt.get();

        if (actor == User.Role.ADMIN && existing.getRole() != User.Role.USER) {
            throw new UnauthorizedActionException("ADMIN can only update users with role USER");
        }

        User.Role newRole = parseRole(body.getRole());
        if (newRole == null) {
            return ResponseEntity.badRequest().body("Unknown role: " + body.getRole());
        }

        if (actor == User.Role.ADMIN && newRole != User.Role.USER) {
            throw new UnauthorizedActionException("ADMIN can only update users to role USER");
        }

        if (newRole == User.Role.OWNER && existing.getRole() != User.Role.OWNER && userRepository.existsByRole(User.Role.OWNER)) {
            throw new DuplicateResourceException("An OWNER already exists in the system");
        }

        if (body.getPassword() != null && !body.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(body.getPassword()));
        }
        existing.setRole(newRole);
        User saved = userRepository.save(existing);

        return ResponseEntity.ok(toDto(saved));
    }

    @Override
    public ResponseEntity<?> deleteUser(String actorRole, String actorEmail, String email) {
        User.Role actor = parseRole(actorRole);
        if (actor == null || actor == User.Role.USER) {
            throw new UnauthorizedActionException("You are not authorized to delete users");
        }

        if (actorEmail != null && actorEmail.equalsIgnoreCase(email)) {
            throw new UnauthorizedActionException("You cannot delete your own account");
        }

        Optional<User> existingOpt = userRepository.findByEmail(email);
        if (existingOpt.isEmpty()) {
            throw new ResourceNotFoundException("User with email " + email + " does not exist");
        }
        User existing = existingOpt.get();

        if (actor == User.Role.ADMIN && existing.getRole() != User.Role.USER) {
            throw new UnauthorizedActionException("ADMIN can only delete users with role USER");
        }

        userRepository.delete(existing);

        if (existing.getRole() == User.Role.USER) {
            deprovisionAccounts(existing.getEmail());
        }

        return ResponseEntity.noContent().build();
    }

    private void provisionAccounts(String email) {
        try {
            bankAccountProxy.createAccount("ADMIN", new BankAccountDto(email, DEFAULT_FIAT_CURRENCY, 0.0));
        } catch (Exception e) {
            log.warn("Failed to auto-create bank account for {}: {}", email, e.getMessage());
        }
        try {
            cryptoWalletProxy.createWallet("ADMIN", new CryptoWalletDto(email, DEFAULT_CRYPTO_CURRENCY, 0.0));
        } catch (Exception e) {
            log.warn("Failed to auto-create crypto wallet for {}: {}", email, e.getMessage());
        }
    }

    private void deprovisionAccounts(String email) {
        try {
            List<BankAccountDto> accounts = extractBankAccounts(bankAccountProxy.getAccountByEmail("ADMIN", email, email));
            for (BankAccountDto account : accounts) {
                try {
                    bankAccountProxy.deleteAccount("ADMIN", email, account.getCurrencyCode());
                } catch (Exception e) {
                    log.warn("Failed to delete bank account {} for {}: {}", account.getCurrencyCode(), email, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch bank accounts for {}: {}", email, e.getMessage());
        }

        try {
            List<CryptoWalletDto> wallets = extractCryptoWallets(cryptoWalletProxy.getWalletByEmail("ADMIN", email, email));
            for (CryptoWalletDto wallet : wallets) {
                try {
                    cryptoWalletProxy.deleteWallet("ADMIN", email, wallet.getCryptoCurrencyCode());
                } catch (Exception e) {
                    log.warn("Failed to delete crypto wallet {} for {}: {}", wallet.getCryptoCurrencyCode(), email, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch crypto wallets for {}: {}", email, e.getMessage());
        }
    }

    private List<BankAccountDto> extractBankAccounts(ResponseEntity<?> response) {
        Object body = response.getBody();
        if (body == null) {
            return List.of();
        }
        return objectMapper.convertValue(body, new TypeReference<>() {});
    }

    private List<CryptoWalletDto> extractCryptoWallets(ResponseEntity<?> response) {
        Object body = response.getBody();
        if (body == null) {
            return List.of();
        }
        return objectMapper.convertValue(body, new TypeReference<>() {});
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getEmail(), user.getPassword(), user.getRole().name());
    }

    private User.Role parseRole(String role) {
        if (role == null) {
            return null;
        }
        try {
            return User.Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
