package com.wsdev.simplestock.domain.user;

import com.wsdev.simplestock.common.ControllerTestSupport;
import com.wsdev.simplestock.common.exceptions.ForbiddenOperationException;
import com.wsdev.simplestock.domain.user.dto.response.UserResponseDTO;
import com.wsdev.simplestock.domain.user.model.enums.Permission;
import com.wsdev.simplestock.domain.user.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest( UserController.class )
class UserControllerTest extends ControllerTestSupport
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserResponseDTO dto()
    {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId( 3L );
        dto.setUsername( "john" );
        dto.setEmail( "john@mail.com" );
        dto.setRole( Role.USER );
        dto.setPermissions( Set.of( Permission.CATEGORY_READ ) );
        dto.setEffectivePermissions( Set.of( Permission.CATEGORY_READ ) );
        return dto;
    }

    @Test
    void endpoints_withoutToken_return403() throws Exception
    {
        mockMvc.perform( get( "/api/v1/users" ) ).andExpect( status().isForbidden() );
        mockMvc.perform( get( "/api/v1/users/me" ) ).andExpect( status().isForbidden() );
    }

    @Test
    void me_returnsAuthenticatedUser() throws Exception
    {
        when( userService.getUserById( 3L ) ).thenReturn( dto() );

        mockMvc.perform( get( "/api/v1/users/me" ).with( authAs( 3L ) ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.username" ).value( "john" ) )
                .andExpect( jsonPath( "$.role" ).value( "USER" ) )
                .andExpect( jsonPath( "$.effectivePermissions[0]" ).value( "CATEGORY_READ" ) );
    }

    @Test
    void permissions_listIsAvailableToAnyAuthenticatedUser() throws Exception
    {
        when( userService.getAvailablePermissions() ).thenReturn( List.of( Permission.values() ) );

        mockMvc.perform( get( "/api/v1/users/permissions" ).with( auth( Permission.CATEGORY_READ ) ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.length()" ).value( Permission.values().length ) );
    }

    @Test
    void list_requiresUserReadPermission() throws Exception
    {
        when( userService.getUsers( any() ) ).thenReturn( new PageImpl<>( List.of( dto() ) ) );

        mockMvc.perform( get( "/api/v1/users" ).with( auth( Permission.CATEGORY_READ ) ) )
                .andExpect( status().isForbidden() );

        mockMvc.perform( get( "/api/v1/users" ).with( auth( Permission.USER_READ ) ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content[0].email" ).value( "john@mail.com" ) );
    }

    @Test
    void getById_requiresUserReadPermission() throws Exception
    {
        when( userService.getUserById( 3L ) ).thenReturn( dto() );

        mockMvc.perform( get( "/api/v1/users/3" ).with( auth( Permission.CATEGORY_READ ) ) )
                .andExpect( status().isForbidden() );

        mockMvc.perform( get( "/api/v1/users/3" ).with( auth( Permission.USER_READ ) ) )
                .andExpect( status().isOk() );
    }

    @Test
    void updatePermissions_requiresManagePermission() throws Exception
    {
        mockMvc.perform( put( "/api/v1/users/3/permissions" )
                        .with( auth( Permission.USER_READ ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"permissions\":[\"CATEGORY_READ\"]}" ) )
                .andExpect( status().isForbidden() );

        verifyNoInteractions( userService );
    }

    @Test
    void updatePermissions_delegatesToService() throws Exception
    {
        when( userService.updatePermissions( any( Authentication.class ), eq( 3L ), eq( Set.of( Permission.CATEGORY_READ ) ) ) ).thenReturn( dto() );

        mockMvc.perform( put( "/api/v1/users/3/permissions" )
                        .with( auth( Permission.USER_MANAGE_PERMISSIONS ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"permissions\":[\"CATEGORY_READ\"]}" ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.permissions[0]" ).value( "CATEGORY_READ" ) );
    }

    @Test
    void updatePermissions_invalidBody_returns400() throws Exception
    {
        mockMvc.perform( put( "/api/v1/users/3/permissions" )
                        .with( auth( Permission.USER_MANAGE_PERMISSIONS ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{}" ) )
                .andExpect( status().isBadRequest() );

        mockMvc.perform( put( "/api/v1/users/3/permissions" )
                        .with( auth( Permission.USER_MANAGE_PERMISSIONS ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"permissions\":[\"NOT_A_PERMISSION\"]}" ) )
                .andExpect( status().isBadRequest() );

        verifyNoInteractions( userService );
    }

    @Test
    void updatePermissions_businessRuleViolation_returns403WithMessage() throws Exception
    {
        when( userService.updatePermissions( any(), eq( 3L ), any() ) ).thenThrow( new ForbiddenOperationException( "nope" ) );

        mockMvc.perform( put( "/api/v1/users/3/permissions" )
                        .with( auth( Permission.USER_MANAGE_PERMISSIONS ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"permissions\":[]}" ) )
                .andExpect( status().isForbidden() )
                .andExpect( content().string( "nope" ) );
    }

    @Test
    void updateRole_isReservedToSuperUser() throws Exception
    {
        // even holding every permission, without the SUPER_USER role it is denied
        mockMvc.perform( put( "/api/v1/users/3/role" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"role\":\"ADMIN\"}" ) )
                .andExpect( status().isForbidden() );

        verifyNoInteractions( userService );
    }

    @Test
    void updateRole_superUser_delegatesToService() throws Exception
    {
        UserResponseDTO admin = dto();
        admin.setRole( Role.ADMIN );
        when( userService.updateRole( any( Authentication.class ), eq( 3L ), eq( Role.ADMIN ) ) ).thenReturn( admin );

        mockMvc.perform( put( "/api/v1/users/3/role" )
                        .with( superUser() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"role\":\"ADMIN\"}" ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.role" ).value( "ADMIN" ) );
    }
}
