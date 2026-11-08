package com.bucketly.api.exception;

public class InvalidPhotoException extends RuntimeException {

    public InvalidPhotoException(String message) {
        super(message);
    }

    public InvalidPhotoException(String message, Throwable cause) {
        super(message, cause);
    }
}

