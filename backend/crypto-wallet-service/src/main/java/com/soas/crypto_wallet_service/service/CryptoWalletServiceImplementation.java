package com.soas.crypto_wallet_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soas.crypto_wallet_service.model.CryptoWallet;
import com.soas.crypto_wallet_service.repository.CryptoWalletRepository;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import serviceLibrary.dto.cryptoWalletService.CryptoWalletDto;
import serviceLibrary.dto.usersService.UserDto;
import serviceLibrary.proxies.usersService.UsersProxy;
import serviceLibrary.services.cryptoWalletService.CryptoWalletService;
import util.exceptions.DuplicateResourceException;
import util.exceptions.ResourceNotFoundException;
import util.exceptions.UnauthorizedActionException;

import java.util.List;
import java.util.Optional;

@RestController
public class CryptoWalletServiceImplementation implements CryptoWalletService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";

    @Autowired
    private CryptoWalletRepository cryptoWalletRepository;

    @Autowired
    private UsersProxy usersProxy;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ResponseEntity<?> getAllWallets(String actorRole) {
        if (!ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            throw new UnauthorizedActionException("Only ADMIN can view all crypto wallets");
        }

        List<CryptoWalletDto> wallets = cryptoWalletRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(wallets);
    }

    @Override
    public ResponseEntity<?> getWalletByEmail(String actorRole, String actorEmail, String email) {
        if (!isAuthorizedToView(actorRole, actorEmail, email)) {
            throw new UnauthorizedActionException("You are not authorized to view this crypto wallet");
        }

        List<CryptoWalletDto> wallets = cryptoWalletRepository.findByEmail(email).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(wallets);
    }

    @Override
    public ResponseEntity<?> createWallet(String actorRole, CryptoWalletDto body) {
        if (!ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            throw new UnauthorizedActionException("Only ADMIN can create crypto wallets");
        }

        assertEmailBelongsToUser(body.getEmail());

        if (cryptoWalletRepository.existsByEmailAndCryptoCurrencyCode(body.getEmail(), body.getCryptoCurrencyCode())) {
            throw new DuplicateResourceException("A crypto wallet for this email and currency already exists");
        }

        CryptoWallet wallet = new CryptoWallet();
        wallet.setEmail(body.getEmail());
        wallet.setCryptoCurrencyCode(body.getCryptoCurrencyCode());
        wallet.setAmount(body.getAmount());
        CryptoWallet saved = cryptoWalletRepository.save(wallet);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @Override
    public ResponseEntity<?> updateWallet(String actorRole, CryptoWalletDto body) {
        if (!ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            throw new UnauthorizedActionException("Only ADMIN can update crypto wallets");
        }

        Optional<CryptoWallet> existingOpt = cryptoWalletRepository.findByEmailAndCryptoCurrencyCode(body.getEmail(), body.getCryptoCurrencyCode());
        if (existingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Crypto wallet for email " + body.getEmail() + " and currency " + body.getCryptoCurrencyCode() + " does not exist");
        }

        CryptoWallet existing = existingOpt.get();
        existing.setAmount(body.getAmount());
        CryptoWallet saved = cryptoWalletRepository.save(existing);

        return ResponseEntity.ok(toDto(saved));
    }

    @Override
    public ResponseEntity<?> deleteWallet(String actorRole, String email, String cryptoCurrencyCode) {
        if (!ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            throw new UnauthorizedActionException("Only ADMIN can delete crypto wallets");
        }

        Optional<CryptoWallet> existingOpt = cryptoWalletRepository.findByEmailAndCryptoCurrencyCode(email, cryptoCurrencyCode);
        if (existingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Crypto wallet for email " + email + " and currency " + cryptoCurrencyCode + " does not exist");
        }

        cryptoWalletRepository.delete(existingOpt.get());
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

    private CryptoWalletDto toDto(CryptoWallet wallet) {
        return new CryptoWalletDto(wallet.getEmail(), wallet.getCryptoCurrencyCode(), wallet.getAmount());
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
                    "User with email " + email + " does not have role USER; crypto wallets can only be created for USER accounts");
        }
    }
}
