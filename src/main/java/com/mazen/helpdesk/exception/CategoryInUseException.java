package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException() {
        super("Category is used by existing tickets and cannot be deleted");
    }
}
