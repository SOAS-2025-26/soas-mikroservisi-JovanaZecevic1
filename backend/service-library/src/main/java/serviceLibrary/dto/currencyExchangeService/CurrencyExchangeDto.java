package serviceLibrary.dto.currencyExchangeService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyExchangeDto {

    private String fromCurrency;
    private String toCurrencyCode;
    private String toCurrencyName;
    private double rate;

}
