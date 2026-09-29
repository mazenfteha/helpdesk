package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;


public class CategoryNotFoundException extends ApiException {
    public CategoryNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Category not found");
    }
}
