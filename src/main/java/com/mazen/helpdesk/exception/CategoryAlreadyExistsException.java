package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;


public class CategoryAlreadyExistsException extends ApiException {
    public CategoryAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "Category already exists");
    }

}
