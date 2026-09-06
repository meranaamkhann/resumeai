package com.resumeai.exception;

import org.springframework.http.HttpStatus;

public class InvalidFileException extends ApiException {
    public InvalidFileException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_FILE", message);
    }
}

