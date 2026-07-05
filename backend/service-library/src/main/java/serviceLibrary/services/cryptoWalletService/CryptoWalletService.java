package serviceLibrary.services.cryptoWalletService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import serviceLibrary.dto.cryptoWalletService.CryptoWalletDto;

@Service
public interface CryptoWalletService {

    @GetMapping("/wallets")
    ResponseEntity<?> getAllWallets(@RequestHeader("X-Actor-Role") String actorRole);

    @GetMapping("/wallets/email")
    ResponseEntity<?> getWalletByEmail(@RequestHeader("X-Actor-Role") String actorRole,
                                       @RequestHeader("X-Actor-Email") String actorEmail,
                                       @RequestParam String email);

    @PostMapping("/wallets")
    ResponseEntity<?> createWallet(@RequestHeader("X-Actor-Role") String actorRole, @RequestBody CryptoWalletDto body);

    @PutMapping("/wallets")
    ResponseEntity<?> updateWallet(@RequestHeader("X-Actor-Role") String actorRole, @RequestBody CryptoWalletDto body);

    @DeleteMapping("/wallets")
    ResponseEntity<?> deleteWallet(@RequestHeader("X-Actor-Role") String actorRole,
                                   @RequestParam String email,
                                   @RequestParam String cryptoCurrencyCode);

}
