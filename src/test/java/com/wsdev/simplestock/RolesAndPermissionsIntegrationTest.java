package com.wsdev.simplestock;

import com.jayway.jsonpath.JsonPath;
import com.wsdev.simplestock.domain.user.UserRepository;
import com.wsdev.simplestock.domain.user.model.User;
import com.wsdev.simplestock.domain.user.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End to end tests of the roles/permissions scheme: real JWT filter, method security and database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles( "test" )
class RolesAndPermissionsIntegrationTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private String superToken;

    @BeforeEach
    void loginAsSuperUser() throws Exception
    {
        superToken = login( "super@test.local", "SuperPass123" );
    }

    private String login( String email, String password ) throws Exception
    {
        String body = mockMvc.perform( post( "/api/v1/auth/login" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}" ) )
                .andExpect( status().isOk() )
                .andReturn().getResponse().getContentAsString();

        return "Bearer " + JsonPath.read( body, "$.token" );
    }

    /** Registers a new account and returns its email. */
    private String register() throws Exception
    {
        String email = UUID.randomUUID() + "@mail.com";

        mockMvc.perform( post( "/api/v1/auth/register" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"username\":\"user\",\"email\":\"" + email + "\",\"password\":\"pw\"}" ) )
                .andExpect( status().isCreated() );

        return email;
    }

    private Long idOf( String email )
    {
        return userRepository.findByEmail( email ).orElseThrow().getId();
    }

    private ResultActions setPermissions( String token, Long targetId, String permissionsJson ) throws Exception
    {
        return mockMvc.perform( put( "/api/v1/users/" + targetId + "/permissions" )
                .header( "Authorization", token )
                .contentType( MediaType.APPLICATION_JSON )
                .content( "{\"permissions\":" + permissionsJson + "}" ) );
    }

    private ResultActions setRole( String token, Long targetId, String role ) throws Exception
    {
        return mockMvc.perform( put( "/api/v1/users/" + targetId + "/role" )
                .header( "Authorization", token )
                .contentType( MediaType.APPLICATION_JSON )
                .content( "{\"role\":\"" + role + "\"}" ) );
    }

    @Test
    void superUserIsSeededWithEveryPermission() throws Exception
    {
        mockMvc.perform( get( "/api/v1/users/me" ).header( "Authorization", superToken ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.role" ).value( "SUPER_USER" ) )
                .andExpect( jsonPath( "$.effectivePermissions.length()" ).value( com.wsdev.simplestock.domain.user.model.enums.Permission.values().length ) );

        mockMvc.perform( get( "/api/v1/categories" ).header( "Authorization", superToken ) ).andExpect( status().isOk() );
    }

    @Test
    void newUser_hasNoPermissionsUntilGranted_andChangesApplyWithoutNewLogin() throws Exception
    {
        String email = register();
        String token = login( email, "pw" );
        Long id = idOf( email );

        mockMvc.perform( get( "/api/v1/categories" ).header( "Authorization", token ) ).andExpect( status().isForbidden() );
        mockMvc.perform( get( "/api/v1/users/me" ).header( "Authorization", token ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.role" ).value( "USER" ) )
                .andExpect( jsonPath( "$.effectivePermissions.length()" ).value( 0 ) );

        setPermissions( superToken, id, "[\"CATEGORY_READ\"]" ).andExpect( status().isOk() );

        // same token, new permission
        mockMvc.perform( get( "/api/v1/categories" ).header( "Authorization", token ) ).andExpect( status().isOk() );
        // but only the one that was granted
        mockMvc.perform( post( "/api/v1/categories/add" )
                        .header( "Authorization", token )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"categoryName\":\"" + UUID.randomUUID() + "\"}" ) )
                .andExpect( status().isForbidden() );
        mockMvc.perform( get( "/api/v1/product" ).header( "Authorization", token ) ).andExpect( status().isForbidden() );

        setPermissions( superToken, id, "[]" ).andExpect( status().isOk() );

        mockMvc.perform( get( "/api/v1/categories" ).header( "Authorization", token ) ).andExpect( status().isForbidden() );
    }

    @Test
    void regularUser_cannotManageUsers() throws Exception
    {
        String email = register();
        String token = login( email, "pw" );
        Long superId = idOf( "super@test.local" );

        mockMvc.perform( get( "/api/v1/users" ).header( "Authorization", token ) ).andExpect( status().isForbidden() );
        setPermissions( token, idOf( email ), "[\"CATEGORY_READ\"]" ).andExpect( status().isForbidden() );
        setPermissions( token, superId, "[]" ).andExpect( status().isForbidden() );
        setRole( token, idOf( email ), "ADMIN" ).andExpect( status().isForbidden() );
        assertEquals( Role.USER, userRepository.findByEmail( email ).orElseThrow().getRole() );
    }

    @Test
    void registrationCannotChooseARole() throws Exception
    {
        String email = UUID.randomUUID() + "@mail.com";

        mockMvc.perform( post( "/api/v1/auth/register" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"username\":\"sneaky\",\"email\":\"" + email + "\",\"password\":\"pw\",\"role\":\"SUPER_USER\"}" ) )
                .andExpect( status().isCreated() );

        assertEquals( Role.USER, userRepository.findByEmail( email ).orElseThrow().getRole() );
    }

    @Test
    void registration_withTakenEmail_returns409() throws Exception
    {
        mockMvc.perform( post( "/api/v1/auth/register" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"username\":\"x\",\"email\":\"super@test.local\",\"password\":\"pw\"}" ) )
                .andExpect( status().isConflict() );

        assertEquals( 1, userRepository.findAll().stream().filter( u -> "super@test.local".equals( u.getEmail() ) ).count() );
    }

    @Test
    void admin_grantsPermissions_butCannotPromoteOrTouchTheSuperUser() throws Exception
    {
        String adminEmail = register();
        String adminToken = login( adminEmail, "pw" );
        Long adminId = idOf( adminEmail );
        String targetEmail = register();
        Long targetId = idOf( targetEmail );

        // admin has no power before being promoted
        setPermissions( adminToken, targetId, "[\"CATEGORY_READ\"]" ).andExpect( status().isForbidden() );

        setRole( superToken, adminId, "ADMIN" ).andExpect( status().isOk() ).andExpect( jsonPath( "$.role" ).value( "ADMIN" ) );

        // promotion applies to the token that was already issued
        mockMvc.perform( get( "/api/v1/product" ).header( "Authorization", adminToken ) ).andExpect( status().isOk() );
        mockMvc.perform( get( "/api/v1/users" ).header( "Authorization", adminToken ) ).andExpect( status().isOk() );

        setPermissions( adminToken, targetId, "[\"CATEGORY_READ\",\"PRODUCT_READ\"]" )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.permissions.length()" ).value( 2 ) );

        // limits of an admin
        setPermissions( adminToken, targetId, "[\"USER_MANAGE_PERMISSIONS\"]" ).andExpect( status().isForbidden() );
        setPermissions( adminToken, adminId, "[]" ).andExpect( status().isForbidden() );
        setPermissions( adminToken, idOf( "super@test.local" ), "[]" ).andExpect( status().isForbidden() );
        setRole( adminToken, targetId, "ADMIN" ).andExpect( status().isForbidden() );
        assertEquals( Role.USER, userRepository.findByEmail( targetEmail ).orElseThrow().getRole() );

        // demoting removes the powers again
        setRole( superToken, adminId, "USER" ).andExpect( status().isOk() );
        mockMvc.perform( get( "/api/v1/users" ).header( "Authorization", adminToken ) ).andExpect( status().isForbidden() );
    }

    @Test
    void superUser_delegatesPermissionManagement_withinWhatTheDelegateHolds() throws Exception
    {
        String managerEmail = register();
        String managerToken = login( managerEmail, "pw" );
        Long managerId = idOf( managerEmail );
        Long targetId = idOf( register() );

        setPermissions( superToken, managerId, "[\"USER_MANAGE_PERMISSIONS\",\"CATEGORY_READ\"]" ).andExpect( status().isOk() );

        setPermissions( managerToken, targetId, "[\"CATEGORY_READ\"]" ).andExpect( status().isOk() );
        setPermissions( managerToken, targetId, "[\"CATEGORY_READ\",\"PRODUCT_DELETE\"]" ).andExpect( status().isForbidden() );
        setPermissions( managerToken, targetId, "[\"USER_MANAGE_PERMISSIONS\"]" ).andExpect( status().isForbidden() );
        setRole( managerToken, targetId, "ADMIN" ).andExpect( status().isForbidden() );

        assertEquals( 1, userRepository.findById( targetId ).orElseThrow().getPermissions().size() );
    }

    @Test
    void superUser_cannotBeChangedNotEvenBySelf() throws Exception
    {
        Long superId = idOf( "super@test.local" );

        setPermissions( superToken, superId, "[]" ).andExpect( status().isForbidden() );
        setRole( superToken, superId, "USER" ).andExpect( status().isForbidden() );
        assertEquals( Role.SUPER_USER, userRepository.findById( superId ).orElseThrow().getRole() );
    }

    @Test
    void superUserRole_cannotBeAssigned() throws Exception
    {
        Long targetId = idOf( register() );

        setRole( superToken, targetId, "SUPER_USER" ).andExpect( status().isForbidden() );
        assertEquals( Role.USER, userRepository.findById( targetId ).orElseThrow().getRole() );
    }

    @Test
    void unknownTarget_returns404_andUnknownPermission_returns400() throws Exception
    {
        setPermissions( superToken, 999999L, "[]" ).andExpect( status().isNotFound() );
        setPermissions( superToken, idOf( register() ), "[\"NOT_A_PERMISSION\"]" ).andExpect( status().isBadRequest() );
    }

    @Test
    void tokenOfADeletedUser_isNotAuthenticated() throws Exception
    {
        String email = register();
        String token = login( email, "pw" );
        User user = userRepository.findByEmail( email ).orElseThrow();

        userRepository.delete( user );

        mockMvc.perform( get( "/api/v1/users/me" ).header( "Authorization", token ) ).andExpect( status().isForbidden() );
    }

    @Test
    void listsAvailablePermissions() throws Exception
    {
        mockMvc.perform( get( "/api/v1/users/permissions" ).header( "Authorization", login( register(), "pw" ) ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$" ).value( org.hamcrest.Matchers.hasItems( "CATEGORY_READ", "MOVEMENT_CREATE", "USER_MANAGE_PERMISSIONS" ) ) );
    }
}
