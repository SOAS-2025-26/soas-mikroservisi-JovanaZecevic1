package com.soas.crypto_wallet_service.repository;

import com.soas.crypto_wallet_service.model.CryptoWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CryptoWalletRepository extends JpaRepository<CryptoWallet, Long> {

    List<CryptoWallet> findByEmail(String email);

    Optional<CryptoWallet> findByEmailAndCryptoCurrencyCode(String email, String cryptoCurrencyCode);

    boolean existsByEmailAndCryptoCurrencyCode(String email, String cryptoCurrencyCode);

}
