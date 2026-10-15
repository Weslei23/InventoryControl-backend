package com.wsdev.simplestock.domain.user;

import com.wsdev.simplestock.common.config.JWTUserData;
import com.wsdev.simplestock.domain.user.dto.request.UpdatePermissionsRequestDTO;
import com.wsdev.simplestock.domain.user.dto.request.UpdateRoleRequestDTO;
import com.wsdev.simplestock.domain.user.dto.response.UserResponseDTO;
import com.wsdev.simplestock.domain.user.model.enums.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin( origins = "http://localhost:5173" )
@RequestMapping( "/api/v1/users" )
@Tag( name = "Users", description = "Users, roles and permissions" )
public class UserController
{
    @Autowired
    private UserService userService;

    @GetMapping( "/me" )
    @Operation( summary = "Authenticated user with role and effective permissions" )
    public UserResponseDTO getMe( @AuthenticationPrincipal JWTUserData principal )
    {
        return userService.getUserById( principal.getUserId() );
    }

    @GetMapping( "/permissions" )
    @Operation( summary = "Every permission that can be granted" )
    public List<Permission> getAvailablePermissions()
    {
        return userService.getAvailablePermissions();
    }

    @GetMapping()
    @PreAuthorize( "hasAuthority('USER_READ')" )
    @Operation( summary = "List users" )
    public Page<UserResponseDTO> getUsers( Pageable pageable )
    {
        return userService.getUsers( pageable );
    }

    @GetMapping( "/{id}" )
    @PreAuthorize( "hasAuthority('USER_READ')" )
    @Operation( summary = "Get a user" )
    public UserResponseDTO getUserById( @PathVariable Long id )
    {
        return userService.getUserById( id );
    }

    @PutMapping( "/{id}/permissions" )
    @PreAuthorize( "hasAuthority('USER_MANAGE_PERMISSIONS')" )
    @Operation( summary = "Replace the permissions granted to a user" )
    public UserResponseDTO updatePermissions( @PathVariable Long id, @Valid @RequestBody UpdatePermissionsRequestDTO request, Authentication authentication )
    {
        return userService.updatePermissions( authentication, id, request.getPermissions() );
    }

    @PutMapping( "/{id}/role" )
    @PreAuthorize( "hasRole('SUPER_USER')" )
    @Operation( summary = "Change a user role between ADMIN and USER (super user only)" )
    public UserResponseDTO updateRole( @PathVariable Long id, @Valid @RequestBody UpdateRoleRequestDTO request, Authentication authentication )
    {
        return userService.updateRole( authentication, id, request.getRole() );
    }
}
