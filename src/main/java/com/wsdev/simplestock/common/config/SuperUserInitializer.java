package com.wsdev.simplestock.common.config;

import com.wsdev.simplestock.domain.user.UserRepository;
import com.wsdev.simplestock.domain.user.model.User;
import com.wsdev.simplestock.domain.user.model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SuperUserInitializer implements ApplicationRunner
{
    private static final Logger LOGGER = LoggerFactory.getLogger( SuperUserInitializer.class );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value( "${app.superuser.username:superuser}" )
    private String username;

    @Value( "${app.superuser.email:superuser@simplestock.local}" )
    private String email;

    @Value( "${app.superuser.password:}" )
    private String password;

    @Override
    public void run( ApplicationArguments args )
    {
        Optional<User> existing = userRepository.findByEmail( email );

        if ( existing.isPresent() )
        {
            User user = existing.get();

            if ( user.getRole() != Role.SUPER_USER )
            {
                user.setRole( Role.SUPER_USER );
                userRepository.save( user );
                LOGGER.info( "Existing account {} promoted to super user.", email );
            }

            return;
        }

        if ( password == null || password.isBlank() )
        {
            LOGGER.warn( "No super user exists and 'app.superuser.password' (env SUPERUSER_PASSWORD) is not set: no super user was created." );
            return;
        }

        User superUser = new User();
        superUser.setUsername( username );
        superUser.setEmail( email );
        superUser.setPassword( passwordEncoder.encode( password ) );
        superUser.setRole( Role.SUPER_USER );

        userRepository.save( superUser );

        LOGGER.info( "Super user {} created.", email );
    }
}
