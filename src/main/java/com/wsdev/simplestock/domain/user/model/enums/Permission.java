package com.wsdev.simplestock.domain.user.model.enums;

import java.util.Set;

public enum Permission
{
    CATEGORY_READ,
    CATEGORY_CREATE,
    CATEGORY_UPDATE,
    CATEGORY_DELETE,

    SUPPLIER_READ,
    SUPPLIER_CREATE,
    SUPPLIER_UPDATE,
    SUPPLIER_DELETE,

    PRODUCT_READ,
    PRODUCT_CREATE,
    PRODUCT_UPDATE,
    PRODUCT_DELETE,
    PRODUCT_EXPORT,

    MOVEMENT_READ,
    MOVEMENT_CREATE,
    MOVEMENT_UPDATE,
    MOVEMENT_DELETE,

    USER_READ,
    USER_MANAGE_PERMISSIONS;

    private static final Set<Permission> SUPER_USER_ONLY = Set.of( USER_MANAGE_PERMISSIONS );

    public boolean isSuperUserOnly()
    {
        return SUPER_USER_ONLY.contains( this );
    }
}
