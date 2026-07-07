package serviceLibrary.dto.currencyConversionService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import serviceLibrary.dto.bankAccountService.BankAccountDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversionResultDto {

    private List<BankAccountDto> accounts;
    private String message;

}
