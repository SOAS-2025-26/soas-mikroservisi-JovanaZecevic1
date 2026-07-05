package serviceLibrary.services.bankAccountService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import serviceLibrary.dto.bankAccountService.BankAccountDto;

@Service
public interface BankAccountService {

    @GetMapping("/accounts")
    ResponseEntity<?> getAllAccounts(@RequestHeader("X-Actor-Role") String actorRole);

    @GetMapping("/accounts/email")
    ResponseEntity<?> getAccountByEmail(@RequestHeader("X-Actor-Role") String actorRole,
                                        @RequestHeader("X-Actor-Email") String actorEmail,
                                        @RequestParam String email);

    @PostMapping("/accounts")
    ResponseEntity<?> createAccount(@RequestHeader("X-Actor-Role") String actorRole, @RequestBody BankAccountDto body);

    @PutMapping("/accounts")
    ResponseEntity<?> updateAccount(@RequestHeader("X-Actor-Role") String actorRole, @RequestBody BankAccountDto body);

    @DeleteMapping("/accounts")
    ResponseEntity<?> deleteAccount(@RequestHeader("X-Actor-Role") String actorRole,
                                    @RequestParam String email,
                                    @RequestParam String currencyCode);

}
