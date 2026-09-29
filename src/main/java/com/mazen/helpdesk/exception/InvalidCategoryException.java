package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;

public class InvalidCategoryException extends ApiException {

    public InvalidCategoryException() {
        super(HttpStatus.BAD_REQUEST, "Category does not exist");
    }
}
