package serviceLibrary.proxies.bankAccountService;

import org.springframework.cloud.openfeign.FeignClient;
import serviceLibrary.services.bankAccountService.BankAccountService;

@FeignClient(name = "bank-account-service")
public interface BankAccountProxy extends BankAccountService {
}
