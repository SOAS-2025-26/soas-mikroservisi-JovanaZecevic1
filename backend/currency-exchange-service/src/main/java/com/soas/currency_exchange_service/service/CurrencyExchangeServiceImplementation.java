package com.soas.currency_exchange_service.service;

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

    @Override
    public ResponseEntity<?> getExchange(String from, String to) {
        String apiUrl = String.format("https://www.floatrates.com/daily/%s.json", from.toLowerCase());

        MultipleCurrenciesStructure response;
        try {
            response = restTemplate.getForEntity(apiUrl, MultipleCurrenciesStructure.class).getBody();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Failed to fetch exchange rates for " + from + ": " + e.getMessage());
        }

        if (response == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Empty response from exchange rate provider");
        }

        SingleCurrencyStructure target = response.getCurrencies().get(to.toLowerCase());
        if (target == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unsupported target currency: " + to);
        }

        CurrencyExchangeDto dto = new CurrencyExchangeDto(from.toUpperCase(), target.getCode(), target.getName(), target.getRate());
        return ResponseEntity.ok(dto);
    }

}
