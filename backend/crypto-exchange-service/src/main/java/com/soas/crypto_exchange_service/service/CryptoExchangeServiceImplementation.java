package com.soas.crypto_exchange_service.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import serviceLibrary.dto.cryptoExchangeService.CryptoExchangeDto;
import serviceLibrary.services.cryptoExchangeService.CryptoExchangeService;

import java.util.Map;

@RestController
public class CryptoExchangeServiceImplementation implements CryptoExchangeService {

    private static final Map<String, String> SYMBOL_TO_COINGECKO_ID = Map.ofEntries(
            Map.entry("BTC", "bitcoin"),
            Map.entry("ETH", "ethereum"),
            Map.entry("USDT", "tether"),
            Map.entry("BNB", "binancecoin"),
            Map.entry("SOL", "solana"),
            Map.entry("XRP", "ripple"),
            Map.entry("USDC", "usd-coin"),
            Map.entry("ADA", "cardano"),
            Map.entry("DOGE", "dogecoin"),
            Map.entry("TRX", "tron"),
            Map.entry("TON", "the-open-network"),
            Map.entry("DOT", "polkadot"),
            Map.entry("MATIC", "matic-network"),
            Map.entry("LTC", "litecoin"),
            Map.entry("AVAX", "avalanche-2"),
            Map.entry("SHIB", "shiba-inu"),
            Map.entry("LINK", "chainlink"),
            Map.entry("ATOM", "cosmos"),
            Map.entry("XLM", "stellar"),
            Map.entry("BCH", "bitcoin-cash"),
            Map.entry("UNI", "uniswap"),
            Map.entry("ETC", "ethereum-classic"),
            Map.entry("NEAR", "near"),
            Map.entry("ALGO", "algorand"),
            Map.entry("VET", "vechain"),
            Map.entry("ICP", "internet-computer"),
            Map.entry("FIL", "filecoin"),
            Map.entry("APT", "aptos"),
            Map.entry("ARB", "arbitrum"),
            Map.entry("OP", "optimism")
    );

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ResponseEntity<?> getExchange(String from, String to) {
        String fromId = SYMBOL_TO_COINGECKO_ID.get(from.toUpperCase());
        if (fromId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unsupported cryptocurrency: " + from);
        }

        String vsCurrency = to.toLowerCase();
        String apiUrl = String.format(
                "https://api.coingecko.com/api/v3/simple/price?ids=%s&vs_currencies=%s", fromId, vsCurrency);

        Map<String, Map<String, Double>> response;
        try {
            ResponseEntity<Map<String, Map<String, Double>>> result = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            response = result.getBody();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Failed to fetch exchange rate for " + from + ": " + e.getMessage());
        }

        Map<String, Double> rates = response != null ? response.get(fromId) : null;
        Double rate = rates != null ? rates.get(vsCurrency) : null;
        if (rate == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unsupported target currency: " + to);
        }

        CryptoExchangeDto dto = new CryptoExchangeDto(from.toUpperCase(), to.toUpperCase(), rate);
        return ResponseEntity.ok(dto);
    }

}
