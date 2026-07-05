package serviceLibrary.proxies.cryptoWalletService;

import org.springframework.cloud.openfeign.FeignClient;
import serviceLibrary.services.cryptoWalletService.CryptoWalletService;

@FeignClient(name = "crypto-wallet-service")
public interface CryptoWalletProxy extends CryptoWalletService {
}
