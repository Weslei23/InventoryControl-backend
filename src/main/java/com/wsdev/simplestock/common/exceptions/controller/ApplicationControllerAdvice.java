package com.wsdev.simplestock.common.exceptions.controller;

import com.wsdev.simplestock.common.exceptions.EmailAlreadyInUseException;
import com.wsdev.simplestock.common.exceptions.ForbiddenOperationException;
import com.wsdev.simplestock.common.exceptions.RecordNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApplicationControllerAdvice
{
    @ExceptionHandler( RecordNotFoundException.class )
    @ResponseStatus( HttpStatus.NOT_FOUND )
    public String handleNotFoundException( RecordNotFoundException exception )
    {
        return exception.getMessage();
    }

    @ExceptionHandler( ForbiddenOperationException.class )
    @ResponseStatus( HttpStatus.FORBIDDEN )
    public String handleForbiddenOperation( ForbiddenOperationException exception )
    {
        return exception.getMessage();
    }

    @ExceptionHandler( EmailAlreadyInUseException.class )
    @ResponseStatus( HttpStatus.CONFLICT )
    public String handleEmailAlreadyInUse( EmailAlreadyInUseException exception )
    {
        return exception.getMessage();
    }
}
