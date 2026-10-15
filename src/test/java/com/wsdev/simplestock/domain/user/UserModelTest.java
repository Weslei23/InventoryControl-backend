package com.wsdev.simplestock.domain.user;

import com.wsdev.simplestock.domain.user.model.User;
import com.wsdev.simplestock.domain.user.model.enums.Permission;
import com.wsdev.simplestock.domain.user.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class UserModelTest
{
    private Set<String> authorities( User user )
    {
        return user.getAuthorities().stream().map( GrantedAuthority::getAuthority ).collect( Collectors.toSet() );
    }

    @Test
    void newUser_isRegularUserWithoutPermissions()
    {
        User user = new User();

        assertEquals( Role.USER, user.getRole() );
        assertTrue( user.getEffectivePermissions().isEmpty() );
        assertEquals( Set.of( "ROLE_USER" ), authorities( user ) );
    }

    @Test
    void user_hasOnlyGrantedPermissions()
    {
        User user = new User();
        user.setPermissions( Set.of( Permission.CATEGORY_READ, Permission.PRODUCT_READ ) );

        assertEquals( Set.of( "ROLE_USER", "CATEGORY_READ", "PRODUCT_READ" ), authorities( user ) );
    }

    @Test
    void superUserAndAdmin_haveEveryPermission()
    {
        for ( Role role : new Role[]{ Role.SUPER_USER, Role.ADMIN } )
        {
            User user = new User();
            user.setRole( role );

            assertEquals( Set.of( Permission.values() ), user.getEffectivePermissions() );
            assertTrue( authorities( user ).contains( role.authority() ) );
        }
    }

    @Test
    void accountWithoutStoredRole_isTreatedAsRegularUser()
    {
        User legacy = new User();
        legacy.setRole( null );

        assertEquals( Role.USER, legacy.getRole() );
        assertTrue( legacy.getEffectivePermissions().isEmpty() );
    }

    @Test
    void onlyPermissionManagementIsSuperUserOnly()
    {
        for ( Permission permission : Permission.values() )
        {
            assertEquals( permission == Permission.USER_MANAGE_PERMISSIONS, permission.isSuperUserOnly() );
        }
    }
}
