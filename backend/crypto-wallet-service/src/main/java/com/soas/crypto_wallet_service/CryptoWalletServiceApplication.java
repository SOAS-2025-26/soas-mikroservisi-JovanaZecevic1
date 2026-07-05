package com.soas.crypto_wallet_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.soas.crypto_wallet_service", "util"})
@EnableFeignClients(basePackages = "serviceLibrary.proxies")
public class CryptoWalletServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CryptoWalletServiceApplication.class, args);
	}

}
