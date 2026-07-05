package serviceLibrary.services.currencyConversionService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Service
public interface CurrencyConversionService {

    @GetMapping("/currency-conversion")
    ResponseEntity<?> convertCurrency(@RequestHeader("X-Actor-Role") String actorRole,
                                       @RequestHeader("X-Actor-Email") String actorEmail,
                                       @RequestParam String from,
                                       @RequestParam String to,
                                       @RequestParam double quantity);

}
