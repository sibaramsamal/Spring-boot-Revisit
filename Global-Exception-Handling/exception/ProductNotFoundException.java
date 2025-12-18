package com.microworkspace.productservice.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class ProductNotFoundException extends RuntimeException {
    private HttpStatus errorCode;

    public ProductNotFoundException(String message, HttpStatus errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
