package com.wsdev.simplestock.domain.user;

import com.wsdev.simplestock.common.config.TokenConfig;
import com.wsdev.simplestock.common.exceptions.EmailAlreadyInUseException;
import com.wsdev.simplestock.domain.user.model.enums.Role;
import com.wsdev.simplestock.domain.user.dto.request.LoginRequestDTO;
import com.wsdev.simplestock.domain.user.dto.request.RegisterUserRequestDTO;
import com.wsdev.simplestock.domain.user.dto.response.LoginResponseDTO;
import com.wsdev.simplestock.domain.user.dto.response.RegisterUserResponseDTO;
import com.wsdev.simplestock.domain.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith( MockitoExtension.class )
class AuthServiceTest
{
    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenConfig tokenConfig;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_validCredentials_returnsToken()
    {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail( "a@b.com" );
        request.setPassword( "pw" );
        User user = new User();
        user.setEmail( "a@b.com" );
        Authentication authentication = mock( Authentication.class );
        when( authentication.getPrincipal() ).thenReturn( user );
        when( authenticationManager.authenticate( any( UsernamePasswordAuthenticationToken.class ) ) ).thenReturn( authentication );
        when( tokenConfig.getSecretKey( user ) ).thenReturn( "jwt-token" );

        LoginResponseDTO response = authService.login( request );

        assertEquals( "jwt-token", response.getToken() );
        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass( UsernamePasswordAuthenticationToken.class );
        verify( authenticationManager ).authenticate( captor.capture() );
        assertEquals( "a@b.com", captor.getValue().getPrincipal() );
        assertEquals( "pw", captor.getValue().getCredentials() );
    }

    @Test
    void login_badCredentials_propagatesException()
    {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail( "a@b.com" );
        request.setPassword( "wrong" );
        when( authenticationManager.authenticate( any() ) ).thenThrow( new BadCredentialsException( "bad" ) );

        assertThrows( BadCredentialsException.class, () -> authService.login( request ) );
        verifyNoInteractions( tokenConfig );
    }

    @Test
    void register_encodesPasswordAndSavesUser()
    {
        RegisterUserRequestDTO request = new RegisterUserRequestDTO();
        request.setUsername( "john" );
        request.setEmail( "john@mail.com" );
        request.setPassword( "plain" );
        when( passwordEncoder.encode( "plain" ) ).thenReturn( "encoded" );

        RegisterUserResponseDTO response = authService.register( request );

        assertEquals( "john", response.getUsername() );
        assertEquals( "john@mail.com", response.getEmail() );
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass( User.class );
        verify( userRepository ).save( captor.capture() );
        assertEquals( "encoded", captor.getValue().getPassword() );
    }

    @Test
    void register_createsRegularUserWithoutPermissions()
    {
        RegisterUserRequestDTO request = new RegisterUserRequestDTO();
        request.setUsername( "john" );
        request.setEmail( "john@mail.com" );
        request.setPassword( "plain" );
        when( passwordEncoder.encode( "plain" ) ).thenReturn( "encoded" );

        authService.register( request );

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass( User.class );
        verify( userRepository ).save( captor.capture() );
        assertEquals( Role.USER, captor.getValue().getRole() );
        assertTrue( captor.getValue().getEffectivePermissions().isEmpty() );
    }

    @Test
    void register_duplicatedEmail_throwsAndDoesNotSave()
    {
        RegisterUserRequestDTO request = new RegisterUserRequestDTO();
        request.setUsername( "john" );
        request.setEmail( "john@mail.com" );
        request.setPassword( "plain" );
        when( userRepository.existsByEmail( "john@mail.com" ) ).thenReturn( true );

        assertThrows( EmailAlreadyInUseException.class, () -> authService.register( request ) );
        verify( userRepository, never() ).save( any() );
    }
}
