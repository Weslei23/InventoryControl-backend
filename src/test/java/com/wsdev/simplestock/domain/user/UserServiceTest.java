package com.wsdev.simplestock.domain.user;

import com.wsdev.simplestock.common.config.JWTUserData;
import com.wsdev.simplestock.common.exceptions.ForbiddenOperationException;
import com.wsdev.simplestock.common.exceptions.RecordNotFoundException;
import com.wsdev.simplestock.domain.user.dto.response.UserResponseDTO;
import com.wsdev.simplestock.domain.user.model.User;
import com.wsdev.simplestock.domain.user.model.enums.Permission;
import com.wsdev.simplestock.domain.user.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith( MockitoExtension.class )
class UserServiceTest
{
    private static final Long SUPER_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long USER_ID = 3L;
    private static final Long OTHER_USER_ID = 4L;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User superUser;
    private User admin;
    private User user;
    private User otherUser;

    @BeforeEach
    void setUp()
    {
        superUser = account( SUPER_ID, Role.SUPER_USER );
        admin = account( ADMIN_ID, Role.ADMIN );
        user = account( USER_ID, Role.USER );
        otherUser = account( OTHER_USER_ID, Role.USER );

        lenient().when( userRepository.findById( any() ) ).thenAnswer( invocation ->
        {
            Long id = invocation.getArgument( 0 );
            return java.util.stream.Stream.of( superUser, admin, user, otherUser ).filter( u -> u.getId().equals( id ) ).findFirst();
        } );
        lenient().when( userRepository.save( any( User.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
    }

    private User account( Long id, Role role )
    {
        User account = new User();
        account.setId( id );
        account.setUsername( "user" + id );
        account.setEmail( "user" + id + "@mail.com" );
        account.setRole( role );
        account.setPermissions( new HashSet<>() );
        return account;
    }

    private Authentication actor( User account )
    {
        JWTUserData principal = JWTUserData.builder().userId( account.getId() ).email( account.getEmail() ).build();

        return new UsernamePasswordAuthenticationToken( principal, null, account.getAuthorities() );
    }

    // ---- updatePermissions ----

    @Test
    void admin_grantsPermissionsToRegularUser()
    {
        UserResponseDTO result = userService.updatePermissions( actor( admin ), USER_ID, Set.of( Permission.CATEGORY_READ, Permission.PRODUCT_READ ) );

        assertEquals( Set.of( Permission.CATEGORY_READ, Permission.PRODUCT_READ ), user.getPermissions() );
        assertEquals( Set.of( Permission.CATEGORY_READ, Permission.PRODUCT_READ ), result.getEffectivePermissions() );
    }

    @Test
    void updatePermissions_replacesTheWholeSet()
    {
        user.setPermissions( new HashSet<>( Set.of( Permission.CATEGORY_READ, Permission.CATEGORY_DELETE ) ) );

        userService.updatePermissions( actor( admin ), USER_ID, Set.of( Permission.CATEGORY_READ ) );

        assertEquals( Set.of( Permission.CATEGORY_READ ), user.getPermissions() );
    }

    @Test
    void updatePermissions_emptySet_revokesEverything()
    {
        user.setPermissions( new HashSet<>( Set.of( Permission.CATEGORY_READ ) ) );

        userService.updatePermissions( actor( admin ), USER_ID, Set.of() );

        assertTrue( user.getPermissions().isEmpty() );
    }

    @Test
    void superUser_canDelegatePermissionManagement()
    {
        userService.updatePermissions( actor( superUser ), USER_ID, Set.of( Permission.USER_MANAGE_PERMISSIONS, Permission.USER_READ ) );

        assertTrue( user.getPermissions().contains( Permission.USER_MANAGE_PERMISSIONS ) );
    }

    @Test
    void admin_cannotDelegatePermissionManagement()
    {
        assertThrows( ForbiddenOperationException.class,
                () -> userService.updatePermissions( actor( admin ), USER_ID, Set.of( Permission.USER_MANAGE_PERMISSIONS ) ) );

        assertTrue( user.getPermissions().isEmpty() );
        verify( userRepository, never() ).save( any() );
    }

    @Test
    void admin_cannotRevokePermissionManagementDelegation()
    {
        user.setPermissions( new HashSet<>( Set.of( Permission.USER_MANAGE_PERMISSIONS ) ) );

        assertThrows( ForbiddenOperationException.class,
                () -> userService.updatePermissions( actor( admin ), USER_ID, Set.of() ) );

        assertTrue( user.getPermissions().contains( Permission.USER_MANAGE_PERMISSIONS ) );
    }

    @Test
    void delegatedManager_cannotGrantWhatTheyDoNotHave()
    {
        user.setPermissions( new HashSet<>( Set.of( Permission.USER_MANAGE_PERMISSIONS, Permission.CATEGORY_READ ) ) );

        assertThrows( ForbiddenOperationException.class,
                () -> userService.updatePermissions( actor( user ), OTHER_USER_ID, Set.of( Permission.CATEGORY_READ, Permission.PRODUCT_DELETE ) ) );

        assertTrue( otherUser.getPermissions().isEmpty() );
    }

    @Test
    void delegatedManager_canGrantWhatTheyHave()
    {
        user.setPermissions( new HashSet<>( Set.of( Permission.USER_MANAGE_PERMISSIONS, Permission.CATEGORY_READ ) ) );

        userService.updatePermissions( actor( user ), OTHER_USER_ID, Set.of( Permission.CATEGORY_READ ) );

        assertEquals( Set.of( Permission.CATEGORY_READ ), otherUser.getPermissions() );
    }

    @Test
    void nobodyChangesTheirOwnPermissions()
    {
        assertThrows( ForbiddenOperationException.class,
                () -> userService.updatePermissions( actor( admin ), ADMIN_ID, Set.of( Permission.CATEGORY_READ ) ) );
        assertThrows( ForbiddenOperationException.class,
                () -> userService.updatePermissions( actor( user ), USER_ID, Set.of( Permission.CATEGORY_READ ) ) );
    }

    @Test
    void superUserAccount_isImmutable()
    {
        assertThrows( ForbiddenOperationException.class,
                () -> userService.updatePermissions( actor( admin ), SUPER_ID, Set.of() ) );
        assertThrows( ForbiddenOperationException.class,
                () -> userService.updatePermissions( actor( superUser ), SUPER_ID, Set.of() ) );
    }

    @Test
    void admin_cannotChangeAnotherAdmin_butSuperUserCan()
    {
        User otherAdmin = account( 5L, Role.ADMIN );
        when( userRepository.findById( 5L ) ).thenReturn( Optional.of( otherAdmin ) );

        assertThrows( ForbiddenOperationException.class,
                () -> userService.updatePermissions( actor( admin ), 5L, Set.of( Permission.USER_READ ) ) );

        userService.updatePermissions( actor( superUser ), 5L, Set.of( Permission.USER_READ ) );
        assertEquals( Set.of( Permission.USER_READ ), otherAdmin.getPermissions() );
    }

    @Test
    void updatePermissions_unknownUser_throwsNotFound()
    {
        assertThrows( RecordNotFoundException.class,
                () -> userService.updatePermissions( actor( admin ), 99L, Set.of() ) );
    }

    // ---- updateRole ----

    @Test
    void superUser_promotesAndDemotes()
    {
        assertEquals( Role.ADMIN, userService.updateRole( actor( superUser ), USER_ID, Role.ADMIN ).getRole() );
        assertEquals( Role.USER, userService.updateRole( actor( superUser ), USER_ID, Role.USER ).getRole() );
    }

    @Test
    void updateRole_superUserRoleCannotBeAssigned()
    {
        assertThrows( ForbiddenOperationException.class, () -> userService.updateRole( actor( superUser ), USER_ID, Role.SUPER_USER ) );
        assertEquals( Role.USER, user.getRole() );
    }

    @Test
    void updateRole_superUserAccountCannotBeChanged()
    {
        User anotherSuper = account( 6L, Role.SUPER_USER );
        when( userRepository.findById( 6L ) ).thenReturn( Optional.of( anotherSuper ) );

        assertThrows( ForbiddenOperationException.class, () -> userService.updateRole( actor( superUser ), 6L, Role.USER ) );
        assertEquals( Role.SUPER_USER, anotherSuper.getRole() );
    }

    @Test
    void updateRole_ownRoleCannotBeChanged()
    {
        assertThrows( ForbiddenOperationException.class, () -> userService.updateRole( actor( admin ), ADMIN_ID, Role.USER ) );
    }

    @Test
    void updateRole_unknownUser_throwsNotFound()
    {
        assertThrows( RecordNotFoundException.class, () -> userService.updateRole( actor( superUser ), 99L, Role.ADMIN ) );
    }

    // ---- queries ----

    @Test
    void getUserById_returnsRoleAndEffectivePermissions()
    {
        user.setPermissions( new HashSet<>( Set.of( Permission.CATEGORY_READ ) ) );

        UserResponseDTO dto = userService.getUserById( USER_ID );

        assertEquals( Role.USER, dto.getRole() );
        assertEquals( Set.of( Permission.CATEGORY_READ ), dto.getPermissions() );
        assertEquals( Set.of( Permission.CATEGORY_READ ), dto.getEffectivePermissions() );
    }

    @Test
    void getAvailablePermissions_listsAll()
    {
        assertEquals( Permission.values().length, userService.getAvailablePermissions().size() );
    }
}
