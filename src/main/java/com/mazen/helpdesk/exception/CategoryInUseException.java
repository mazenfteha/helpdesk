package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;

public class CategoryInUseException extends ApiException {
    public CategoryInUseException() {
        super(HttpStatus.CONFLICT, "Category is used by existing tickets and cannot be deleted");
    }
}
