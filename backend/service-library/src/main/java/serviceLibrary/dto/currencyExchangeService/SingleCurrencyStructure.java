package serviceLibrary.dto.currencyExchangeService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SingleCurrencyStructure {

    private String code;
    private String alphaCode;
    private String numericCode;
    private String name;
    private double rate;
    private Date date;
    private double inverseRate;

}
