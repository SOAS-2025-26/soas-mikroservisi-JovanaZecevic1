package com.soas.trade_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import serviceLibrary.dto.cryptoWalletService.CryptoWalletDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletTradeResultDto {

    private List<CryptoWalletDto> wallets;
    private String message;

}
