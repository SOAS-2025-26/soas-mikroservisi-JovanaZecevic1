package com.soas.trade_service.exception;

import org.springframework.http.HttpStatus;

public class TradeBusinessException extends RuntimeException {

    private final HttpStatus status;

    public TradeBusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

}
