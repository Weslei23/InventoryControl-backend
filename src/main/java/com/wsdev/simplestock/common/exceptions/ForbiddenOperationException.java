package com.wsdev.simplestock.common.exceptions;

public class ForbiddenOperationException extends RuntimeException
{
    public ForbiddenOperationException( String message )
    {
        super( message );
    }
}
