package com.soas.currency_conversion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.soas.currency_conversion_service.dto.ConversionResultDto;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import serviceLibrary.dto.bankAccountService.BankAccountDto;
import serviceLibrary.dto.currencyExchangeService.CurrencyExchangeDto;
import serviceLibrary.proxies.bankAccountService.BankAccountProxy;
import serviceLibrary.proxies.currencyExchangeService.CurrencyExchangeProxy;
import serviceLibrary.services.currencyConversionService.CurrencyConversionService;

import java.util.List;
import java.util.Optional;

@RestController
public class CurrencyConversionServiceImplementation implements CurrencyConversionService {

    private static final String ROLE_USER = "USER";
    private static final String SYSTEM_ROLE = "ADMIN";

    @Autowired
    private BankAccountProxy bankAccountProxy;

    @Autowired
    private CurrencyExchangeProxy currencyExchangeProxy;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ResponseEntity<?> convertCurrency(String actorRole, String actorEmail, String from, String to, double quantity) {
        if (!ROLE_USER.equalsIgnoreCase(actorRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only USER can use currency conversion");
        }

        if (quantity <= 0) {
            return ResponseEntity.badRequest().body("Quantity must be greater than zero");
        }

        List<BankAccountDto> accounts;
        try {
            accounts = extractList(bankAccountProxy.getAccountByEmail(SYSTEM_ROLE, actorEmail, actorEmail), BankAccountDto.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Failed to fetch bank account: " + e.getMessage());
        }

        Optional<BankAccountDto> fromAccountOpt = accounts.stream()
                .filter(a -> a.getCurrencyCode().equalsIgnoreCase(from))
                .findFirst();

        if (fromAccountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("You do not have a bank account in currency " + from.toUpperCase());
        }

        BankAccountDto fromAccount = fromAccountOpt.get();
        if (fromAccount.getAmount() < quantity) {
            return ResponseEntity.badRequest().body("Insufficient funds: you have " + fromAccount.getAmount() + " " + from.toUpperCase());
        }

        CurrencyExchangeDto exchange;
        try {
            Object body = currencyExchangeProxy.getExchange(from, to).getBody();
            exchange = objectMapper.convertValue(body, CurrencyExchangeDto.class);
        } catch (FeignException e) {
            HttpStatus status = e.status() == 400 ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
            return ResponseEntity.status(status).body("Failed to fetch exchange rate for " + from + " to " + to);
        }

        double exchangedAmount = quantity * exchange.getRate();

        double newFromAmount = fromAccount.getAmount() - quantity;
        try {
            bankAccountProxy.updateAccount(SYSTEM_ROLE, new BankAccountDto(actorEmail, from.toUpperCase(), newFromAmount));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Failed to update source account: " + e.getMessage());
        }

        Optional<BankAccountDto> toAccountOpt = accounts.stream()
                .filter(a -> a.getCurrencyCode().equalsIgnoreCase(to))
                .findFirst();

        try {
            if (toAccountOpt.isPresent()) {
                double newToAmount = toAccountOpt.get().getAmount() + exchangedAmount;
                bankAccountProxy.updateAccount(SYSTEM_ROLE, new BankAccountDto(actorEmail, to.toUpperCase(), newToAmount));
            } else {
                bankAccountProxy.createAccount(SYSTEM_ROLE, new BankAccountDto(actorEmail, to.toUpperCase(), exchangedAmount));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Failed to update destination account: " + e.getMessage());
        }

        List<BankAccountDto> updatedAccounts;
        try {
            updatedAccounts = extractList(bankAccountProxy.getAccountByEmail(SYSTEM_ROLE, actorEmail, actorEmail), BankAccountDto.class);
        } catch (Exception e) {
            updatedAccounts = List.of();
        }

        String message = String.format("Successfully exchanged %s: %s for %s: %s",
                from.toUpperCase(), formatAmount(quantity), to.toUpperCase(), formatAmount(exchangedAmount));

        return ResponseEntity.ok(new ConversionResultDto(updatedAccounts, message));
    }

    private String formatAmount(double amount) {
        if (amount == Math.floor(amount)) {
            return String.valueOf((long) amount);
        }
        return String.format(java.util.Locale.US, "%.2f", amount);
    }

    private <T> List<T> extractList(ResponseEntity<?> response, Class<T> itemType) {
        Object body = response.getBody();
        if (body == null) {
            return List.of();
        }
        CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
        return objectMapper.convertValue(body, listType);
    }
}
