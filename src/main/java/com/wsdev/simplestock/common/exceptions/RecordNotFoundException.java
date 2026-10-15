package com.wsdev.simplestock.common.exceptions;

public class RecordNotFoundException extends RuntimeException
{
    public RecordNotFoundException( Long id )
    {
        super( "Record not found with id: " + id );
    }

    public RecordNotFoundException( String message )
    {
        super( message );
    }
}
