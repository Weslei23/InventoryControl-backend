package com.wsdev.simplestock.common.exceptions;

public class EmailAlreadyInUseException extends RuntimeException
{
    public EmailAlreadyInUseException( String email )
    {
        super( "Email already in use: " + email );
    }
}
