package serviceLibrary.dto.currencyExchangeService;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class MultipleCurrenciesStructure {

    @JsonAnySetter
    private Map<String, SingleCurrencyStructure> currencies = new HashMap<>();

}
