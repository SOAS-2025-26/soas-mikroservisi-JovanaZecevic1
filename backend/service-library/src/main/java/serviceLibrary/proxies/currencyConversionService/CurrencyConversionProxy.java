package serviceLibrary.proxies.currencyConversionService;

import org.springframework.cloud.openfeign.FeignClient;
import serviceLibrary.services.currencyConversionService.CurrencyConversionService;

@FeignClient(name = "currency-conversion-service")
public interface CurrencyConversionProxy extends CurrencyConversionService {
}
