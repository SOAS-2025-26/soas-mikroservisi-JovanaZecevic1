package serviceLibrary.dto.cryptoWalletService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CryptoWalletDto {

    private String email;
    private String cryptoCurrencyCode;
    private Double amount;

}
