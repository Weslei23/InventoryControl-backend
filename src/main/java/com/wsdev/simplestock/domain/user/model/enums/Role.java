package com.wsdev.simplestock.domain.user.model.enums;

import java.util.EnumSet;
import java.util.Set;

public enum Role
{
    SUPER_USER,
    ADMIN,
    USER;

    public static final String AUTHORITY_PREFIX = "ROLE_";

    public Set<Permission> defaultPermissions()
    {
        return this == USER ? EnumSet.noneOf( Permission.class ) : EnumSet.allOf( Permission.class );
    }

    public String authority()
    {
        return AUTHORITY_PREFIX + name();
    }
}
