package com.wsdev.simplestock.common;

import com.wsdev.simplestock.common.config.SuperUserInitializer;
import com.wsdev.simplestock.domain.user.UserRepository;
import com.wsdev.simplestock.domain.user.model.User;
import com.wsdev.simplestock.domain.user.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith( MockitoExtension.class )
class SuperUserInitializerTest
{
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SuperUserInitializer initializer;

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField( initializer, "username", "root" );
        ReflectionTestUtils.setField( initializer, "email", "root@mail.com" );
        ReflectionTestUtils.setField( initializer, "password", "pw" );
    }

    @Test
    void createsSuperUserWhenMissing()
    {
        when( userRepository.findByEmail( "root@mail.com" ) ).thenReturn( Optional.empty() );
        when( passwordEncoder.encode( "pw" ) ).thenReturn( "encoded" );

        initializer.run( null );

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass( User.class );
        verify( userRepository ).save( captor.capture() );
        assertEquals( Role.SUPER_USER, captor.getValue().getRole() );
        assertEquals( "root@mail.com", captor.getValue().getEmail() );
        assertEquals( "encoded", captor.getValue().getPassword() );
    }

    @Test
    void doesNothingWhenPasswordIsNotConfigured()
    {
        ReflectionTestUtils.setField( initializer, "password", "" );
        when( userRepository.findByEmail( "root@mail.com" ) ).thenReturn( Optional.empty() );

        initializer.run( null );

        verify( userRepository, never() ).save( any() );
    }

    @Test
    void promotesExistingAccountWithoutTouchingItsPassword()
    {
        User existing = new User();
        existing.setEmail( "root@mail.com" );
        existing.setPassword( "original" );
        when( userRepository.findByEmail( "root@mail.com" ) ).thenReturn( Optional.of( existing ) );

        initializer.run( null );

        assertEquals( Role.SUPER_USER, existing.getRole() );
        assertEquals( "original", existing.getPassword() );
        verify( userRepository ).save( existing );
        verifyNoInteractions( passwordEncoder );
    }

    @Test
    void leavesAnExistingSuperUserAlone()
    {
        User existing = new User();
        existing.setRole( Role.SUPER_USER );
        when( userRepository.findByEmail( "root@mail.com" ) ).thenReturn( Optional.of( existing ) );

        initializer.run( null );

        verify( userRepository, never() ).save( any() );
    }
}
