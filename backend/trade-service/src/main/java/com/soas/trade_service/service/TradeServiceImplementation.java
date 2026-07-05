package com.soas.trade_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.soas.trade_service.dto.BankAccountTradeResultDto;
import com.soas.trade_service.dto.WalletTradeResultDto;
import com.soas.trade_service.exception.ExternalServiceUnavailableException;
import com.soas.trade_service.exception.TradeBusinessException;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import serviceLibrary.dto.bankAccountService.BankAccountDto;
import serviceLibrary.dto.cryptoExchangeService.CryptoExchangeDto;
import serviceLibrary.dto.cryptoWalletService.CryptoWalletDto;
import serviceLibrary.proxies.bankAccountService.BankAccountProxy;
import serviceLibrary.proxies.cryptoExchangeService.CryptoExchangeProxy;
import serviceLibrary.proxies.cryptoWalletService.CryptoWalletProxy;
import serviceLibrary.proxies.currencyConversionService.CurrencyConversionProxy;
import serviceLibrary.services.tradeService.TradeService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
public class TradeServiceImplementation implements TradeService {

    private static final Set<String> CRYPTO_SYMBOLS = Set.of(
            "BTC", "ETH", "USDT", "BNB", "SOL", "XRP", "USDC", "ADA", "DOGE", "TRX", "TON",
            "DOT", "MATIC", "LTC", "AVAX", "SHIB", "LINK", "ATOM", "XLM", "BCH", "UNI", "ETC",
            "NEAR", "ALGO", "VET", "ICP", "FIL", "APT", "ARB", "OP"
    );
    private static final Set<String> FIAT_ANCHORS = Set.of("USD", "EUR");
    private static final String DEFAULT_ANCHOR = "USD";
    private static final String ROLE_USER = "USER";
    private static final String SYSTEM_ROLE = "ADMIN";

    @Autowired
    private BankAccountProxy bankAccountProxy;

    @Autowired
    private CryptoWalletProxy cryptoWalletProxy;

    @Autowired
    private CryptoExchangeProxy cryptoExchangeProxy;

    @Autowired
    private CurrencyConversionProxy currencyConversionProxy;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CircuitBreakerFactory circuitBreakerFactory;

    @Override
    public ResponseEntity<?> executeTrade(String actorRole, String actorEmail, String from, String to, double quantity) {
        if (!ROLE_USER.equalsIgnoreCase(actorRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only USER can use trade-service");
        }
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body("Quantity must be greater than zero");
        }

        String fromCode = from.toUpperCase();
        String toCode = to.toUpperCase();
        if (fromCode.equals(toCode)) {
            return ResponseEntity.badRequest().body("Cannot exchange a currency for itself");
        }

        boolean fromCrypto = CRYPTO_SYMBOLS.contains(fromCode);
        boolean toCrypto = CRYPTO_SYMBOLS.contains(toCode);

        try {
            if (fromCrypto && toCrypto) {
                return cryptoToCrypto(actorEmail, fromCode, toCode, quantity);
            }
            if (!fromCrypto && toCrypto) {
                return fiatToCrypto(actorEmail, fromCode, toCode, quantity);
            }
            if (fromCrypto) {
                return cryptoToFiat(actorEmail, fromCode, toCode, quantity);
            }
            return ResponseEntity.badRequest().body("Both currencies are fiat; use currency-conversion for fiat-to-fiat exchange");
        } catch (TradeBusinessException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (ExternalServiceUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Trade failed: " + e.getMessage());
        }
    }

    private ResponseEntity<?> cryptoToCrypto(String actorEmail, String from, String to, double quantity) {
        List<CryptoWalletDto> wallets = extractList(
                cryptoWalletProxy.getWalletByEmail(SYSTEM_ROLE, actorEmail, actorEmail), CryptoWalletDto.class);

        Optional<CryptoWalletDto> fromWalletOpt = findWallet(wallets, from);
        if (fromWalletOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("You do not have a crypto wallet in currency " + from);
        }
        CryptoWalletDto fromWallet = fromWalletOpt.get();
        if (fromWallet.getAmount() < quantity) {
            return ResponseEntity.badRequest().body("Insufficient funds: you have " + fromWallet.getAmount() + " " + from);
        }

        CryptoExchangeDto rate = fetchCryptoExchange(from, to);
        double convertedAmount = quantity * rate.getRate();

        cryptoWalletProxy.updateWallet(SYSTEM_ROLE, new CryptoWalletDto(actorEmail, from, fromWallet.getAmount() - quantity));
        creditWallet(actorEmail, wallets, to, convertedAmount);

        List<CryptoWalletDto> finalWallets = extractList(
                cryptoWalletProxy.getWalletByEmail(SYSTEM_ROLE, actorEmail, actorEmail), CryptoWalletDto.class);
        String message = buildMessage(from, quantity, to, convertedAmount);
        return ResponseEntity.ok(new WalletTradeResultDto(finalWallets, message));
    }

    private ResponseEntity<?> fiatToCrypto(String actorEmail, String from, String to, double quantity) {
        List<BankAccountDto> accounts = extractList(
                bankAccountProxy.getAccountByEmail(SYSTEM_ROLE, actorEmail, actorEmail), BankAccountDto.class);

        Optional<BankAccountDto> fromAccountOpt = findAccount(accounts, from);
        if (fromAccountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("You do not have a bank account in currency " + from);
        }
        BankAccountDto fromAccount = fromAccountOpt.get();
        if (fromAccount.getAmount() < quantity) {
            return ResponseEntity.badRequest().body("Insufficient funds: you have " + fromAccount.getAmount() + " " + from);
        }

        String anchor;
        double anchorAmount;
        if (FIAT_ANCHORS.contains(from)) {
            anchor = from;
            anchorAmount = quantity;
            bankAccountProxy.updateAccount(SYSTEM_ROLE, new BankAccountDto(actorEmail, anchor, fromAccount.getAmount() - quantity));
        } else {
            anchor = DEFAULT_ANCHOR;
            double priorAnchorAmount = findAccount(accounts, anchor).map(BankAccountDto::getAmount).orElse(0.0);
            List<BankAccountDto> afterConversion = callCurrencyConversion(actorEmail, from, anchor, quantity);
            double afterAnchorAmount = findAccount(afterConversion, anchor).map(BankAccountDto::getAmount).orElse(priorAnchorAmount);
            anchorAmount = afterAnchorAmount - priorAnchorAmount;
            bankAccountProxy.updateAccount(SYSTEM_ROLE, new BankAccountDto(actorEmail, anchor, priorAnchorAmount));
        }

        // crypto-exchange-service only quotes crypto->(fiat|crypto); fetch BTC-per-anchor and invert
        CryptoExchangeDto rate = fetchCryptoExchange(to, anchor);
        double cryptoAmount = anchorAmount / rate.getRate();

        List<CryptoWalletDto> wallets = extractList(
                cryptoWalletProxy.getWalletByEmail(SYSTEM_ROLE, actorEmail, actorEmail), CryptoWalletDto.class);
        creditWallet(actorEmail, wallets, to, cryptoAmount);

        List<CryptoWalletDto> finalWallets = extractList(
                cryptoWalletProxy.getWalletByEmail(SYSTEM_ROLE, actorEmail, actorEmail), CryptoWalletDto.class);
        String message = buildMessage(from, quantity, to, cryptoAmount);
        return ResponseEntity.ok(new WalletTradeResultDto(finalWallets, message));
    }

    private ResponseEntity<?> cryptoToFiat(String actorEmail, String from, String to, double quantity) {
        List<CryptoWalletDto> wallets = extractList(
                cryptoWalletProxy.getWalletByEmail(SYSTEM_ROLE, actorEmail, actorEmail), CryptoWalletDto.class);

        Optional<CryptoWalletDto> fromWalletOpt = findWallet(wallets, from);
        if (fromWalletOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("You do not have a crypto wallet in currency " + from);
        }
        CryptoWalletDto fromWallet = fromWalletOpt.get();
        if (fromWallet.getAmount() < quantity) {
            return ResponseEntity.badRequest().body("Insufficient funds: you have " + fromWallet.getAmount() + " " + from);
        }

        String anchor = FIAT_ANCHORS.contains(to) ? to : DEFAULT_ANCHOR;

        CryptoExchangeDto rate = fetchCryptoExchange(from, anchor);
        double anchorAmount = quantity * rate.getRate();

        cryptoWalletProxy.updateWallet(SYSTEM_ROLE, new CryptoWalletDto(actorEmail, from, fromWallet.getAmount() - quantity));

        List<BankAccountDto> accountsBefore = extractList(
                bankAccountProxy.getAccountByEmail(SYSTEM_ROLE, actorEmail, actorEmail), BankAccountDto.class);

        double priorAnchorAmount = findAccount(accountsBefore, anchor).map(BankAccountDto::getAmount).orElse(0.0);
        creditAccount(actorEmail, accountsBefore, anchor, anchorAmount);

        List<BankAccountDto> finalAccounts;
        double amountReceived;

        if (anchor.equals(to)) {
            finalAccounts = extractList(
                    bankAccountProxy.getAccountByEmail(SYSTEM_ROLE, actorEmail, actorEmail), BankAccountDto.class);
            amountReceived = anchorAmount;
        } else {
            double priorToAmount = findAccount(accountsBefore, to).map(BankAccountDto::getAmount).orElse(0.0);
            List<BankAccountDto> afterConversion = callCurrencyConversion(actorEmail, anchor, to, anchorAmount);
            double afterToAmount = findAccount(afterConversion, to).map(BankAccountDto::getAmount).orElse(priorToAmount);
            amountReceived = afterToAmount - priorToAmount;
            finalAccounts = afterConversion;
        }

        String message = buildMessage(from, quantity, to, amountReceived);
        return ResponseEntity.ok(new BankAccountTradeResultDto(finalAccounts, message));
    }

    private void creditWallet(String actorEmail, List<CryptoWalletDto> existingWallets, String currency, double amountToAdd) {
        Optional<CryptoWalletDto> existing = existingWallets.stream()
                .filter(w -> w.getCryptoCurrencyCode().equalsIgnoreCase(currency)).findFirst();
        if (existing.isPresent()) {
            cryptoWalletProxy.updateWallet(SYSTEM_ROLE, new CryptoWalletDto(actorEmail, currency, existing.get().getAmount() + amountToAdd));
        } else {
            cryptoWalletProxy.createWallet(SYSTEM_ROLE, new CryptoWalletDto(actorEmail, currency, amountToAdd));
        }
    }

    private void creditAccount(String actorEmail, List<BankAccountDto> existingAccounts, String currency, double amountToAdd) {
        Optional<BankAccountDto> existing = findAccount(existingAccounts, currency);
        if (existing.isPresent()) {
            bankAccountProxy.updateAccount(SYSTEM_ROLE, new BankAccountDto(actorEmail, currency, existing.get().getAmount() + amountToAdd));
        } else {
            bankAccountProxy.createAccount(SYSTEM_ROLE, new BankAccountDto(actorEmail, currency, amountToAdd));
        }
    }

    private Optional<BankAccountDto> findAccount(List<BankAccountDto> accounts, String currency) {
        return accounts.stream().filter(a -> a.getCurrencyCode().equalsIgnoreCase(currency)).findFirst();
    }

    private Optional<CryptoWalletDto> findWallet(List<CryptoWalletDto> wallets, String currency) {
        return wallets.stream().filter(w -> w.getCryptoCurrencyCode().equalsIgnoreCase(currency)).findFirst();
    }

    private CryptoExchangeDto fetchCryptoExchange(String from, String to) {
        CircuitBreaker cb = circuitBreakerFactory.create("cryptoExchange");
        return cb.run(
                () -> {
                    Object body = cryptoExchangeProxy.getExchange(from, to).getBody();
                    return objectMapper.convertValue(body, CryptoExchangeDto.class);
                },
                throwable -> {
                    throw translate(throwable, "Crypto exchange rate service is currently unavailable, please try again later");
                }
        );
    }

    private List<BankAccountDto> callCurrencyConversion(String actorEmail, String from, String to, double quantity) {
        CircuitBreaker cb = circuitBreakerFactory.create("currencyConversion");
        return cb.run(
                () -> {
                    Object body = currencyConversionProxy.convertCurrency(ROLE_USER, actorEmail, from, to, quantity).getBody();
                    return extractAccountsFromConversionResult(body);
                },
                throwable -> {
                    throw translate(throwable, "Currency conversion service is currently unavailable, please try again later");
                }
        );
    }

    private RuntimeException translate(Throwable throwable, String unavailableMessage) {
        if (throwable instanceof FeignException fe && fe.status() / 100 == 4) {
            String message = fe.contentUTF8();
            if (message == null || message.isBlank()) {
                message = "Request rejected by downstream service (status " + fe.status() + ")";
            }
            return new TradeBusinessException(HttpStatus.valueOf(fe.status()), message);
        }
        return new ExternalServiceUnavailableException(unavailableMessage);
    }

    @SuppressWarnings("unchecked")
    private List<BankAccountDto> extractAccountsFromConversionResult(Object body) {
        if (!(body instanceof Map<?, ?> map)) {
            return List.of();
        }
        Object accountsRaw = map.get("accounts");
        if (accountsRaw == null) {
            return List.of();
        }
        CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, BankAccountDto.class);
        return objectMapper.convertValue(accountsRaw, listType);
    }

    private <T> List<T> extractList(ResponseEntity<?> response, Class<T> itemType) {
        Object body = response.getBody();
        if (body == null) {
            return List.of();
        }
        CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
        return objectMapper.convertValue(body, listType);
    }

    private String buildMessage(String from, double fromAmount, String to, double toAmount) {
        return String.format("Successfully exchanged %s: %s for %s: %s",
                from, formatAmount(fromAmount), to, formatAmount(toAmount));
    }

    private String formatAmount(double amount) {
        if (amount == Math.floor(amount) && !Double.isInfinite(amount)) {
            return String.valueOf((long) amount);
        }
        String formatted = String.format(java.util.Locale.US, "%.8f", amount);
        formatted = formatted.replaceAll("0+$", "");
        if (formatted.endsWith(".")) {
            formatted = formatted + "0";
        }
        return formatted;
    }
}
