package com.soas.trade_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import serviceLibrary.dto.bankAccountService.BankAccountDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountTradeResultDto {

    private List<BankAccountDto> accounts;
    private String message;

}
