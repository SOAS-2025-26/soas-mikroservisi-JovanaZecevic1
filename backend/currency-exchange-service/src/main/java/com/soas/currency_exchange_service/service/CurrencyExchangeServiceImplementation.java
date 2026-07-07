package com.soas.currency_exchange_service.service;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import serviceLibrary.dto.currencyExchangeService.CurrencyExchangeDto;
import serviceLibrary.dto.currencyExchangeService.MultipleCurrenciesStructure;
import serviceLibrary.dto.currencyExchangeService.SingleCurrencyStructure;
import serviceLibrary.services.currencyExchangeService.CurrencyExchangeService;

@RestController
public class CurrencyExchangeServiceImplementation implements CurrencyExchangeService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Retry retry;

    public CurrencyExchangeServiceImplementation(RetryRegistry retryRegistry) {
        this.retry = retryRegistry.retry("floatrates");
    }

    @Override
    public ResponseEntity<?> getExchange(String from, String to) {
        String apiUrl = String.format("https://www.floatrates.com/daily/%s.json", from.toLowerCase());

        MultipleCurrenciesStructure response;
        try {
            response = retry.executeSupplier(() ->
                    restTemplate.getForEntity(apiUrl, MultipleCurrenciesStructure.class).getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Currency exchange rate service is temporarily unavailable");
        }

        if (response == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Currency exchange rate service is temporarily unavailable");
        }

        SingleCurrencyStructure target = response.getCurrencies().get(to.toLowerCase());
        if (target == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unsupported target currency: " + to);
        }

        CurrencyExchangeDto dto = new CurrencyExchangeDto(from.toUpperCase(), target.getCode(), target.getName(), target.getRate());
        return ResponseEntity.ok(dto);
    }

}
