package serviceLibrary.proxies.currencyExchangeService;

import org.springframework.cloud.openfeign.FeignClient;
import serviceLibrary.services.currencyExchangeService.CurrencyExchangeService;

@FeignClient(name = "currency-exchange-service")
public interface CurrencyExchangeProxy extends CurrencyExchangeService {
}
