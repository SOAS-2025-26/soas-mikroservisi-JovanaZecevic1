package serviceLibrary.dto.bankAccountService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountDto {

    private String email;
    private String currencyCode;
    private Double amount;

}
