package serviceLibrary.services.tradeService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Service
public interface TradeService {

    @GetMapping("/trade-service")
    ResponseEntity<?> executeTrade(@RequestHeader("X-Actor-Role") String actorRole,
                                    @RequestHeader("X-Actor-Email") String actorEmail,
                                    @RequestParam String from,
                                    @RequestParam String to,
                                    @RequestParam double quantity);

}
