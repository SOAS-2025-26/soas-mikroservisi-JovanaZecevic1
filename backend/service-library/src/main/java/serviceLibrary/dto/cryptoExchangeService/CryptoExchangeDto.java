package serviceLibrary.dto.cryptoExchangeService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CryptoExchangeDto {

    private String fromCurrency;
    private String toCurrency;
    private double rate;

}
