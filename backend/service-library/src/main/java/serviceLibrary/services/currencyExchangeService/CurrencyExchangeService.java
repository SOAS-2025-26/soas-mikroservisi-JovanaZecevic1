package serviceLibrary.services.currencyExchangeService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Service
public interface CurrencyExchangeService {

    @GetMapping("/currency-exchange")
    ResponseEntity<?> getExchange(@RequestParam String from, @RequestParam String to);

}
