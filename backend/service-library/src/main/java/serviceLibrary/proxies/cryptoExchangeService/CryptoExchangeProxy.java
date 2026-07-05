package serviceLibrary.proxies.cryptoExchangeService;

import org.springframework.cloud.openfeign.FeignClient;
import serviceLibrary.services.cryptoExchangeService.CryptoExchangeService;

@FeignClient(name = "crypto-exchange-service")
public interface CryptoExchangeProxy extends CryptoExchangeService {
}
