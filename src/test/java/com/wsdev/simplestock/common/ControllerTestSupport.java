package com.wsdev.simplestock.common;

import com.wsdev.simplestock.common.config.JWTUserData;
import com.wsdev.simplestock.common.config.SecurityConfig;
import com.wsdev.simplestock.common.config.SecurityFilter;
import com.wsdev.simplestock.common.config.TokenConfig;
import com.wsdev.simplestock.domain.user.UserRepository;
import com.wsdev.simplestock.domain.user.model.enums.Permission;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Base for @WebMvcTest controller tests: loads the real SecurityConfig (including method security) so
 * unauthenticated requests are rejected and @PreAuthorize is enforced.
 * {@link #auth()} authenticates a request with every permission, {@link #auth(Permission...)} with only the given ones.
 * The JWT filter itself is exercised end to end in the integration tests.
 */
@Import( { SecurityConfig.class, SecurityFilter.class, TokenConfig.class } )
@TestPropertySource( properties = "secretKey=test-secret" )
public abstract class ControllerTestSupport
{
    // required by SecurityFilter
    @MockitoBean
    private UserRepository userRepository;

    protected RequestPostProcessor auth()
    {
        return auth( Permission.values() );
    }

    protected RequestPostProcessor auth( Permission... permissions )
    {
        SimpleGrantedAuthority[] authorities = new SimpleGrantedAuthority[permissions.length];

        for ( int i = 0; i < permissions.length; i++ )
        {
            authorities[i] = new SimpleGrantedAuthority( permissions[i].name() );
        }

        return user( "tester@mail.com" ).authorities( authorities );
    }

    protected RequestPostProcessor superUser()
    {
        return user( "super@mail.com" ).roles( "SUPER_USER" ).authorities(
                java.util.stream.Stream.concat(
                        java.util.stream.Stream.of( new SimpleGrantedAuthority( "ROLE_SUPER_USER" ) ),
                        java.util.Arrays.stream( Permission.values() ).map( p -> new SimpleGrantedAuthority( p.name() ) ) ).toList() );
    }

    /**
     * Authenticates with the same principal type the JWT filter produces ({@link JWTUserData}).
     */
    protected RequestPostProcessor authAs( Long userId, String... authorities )
    {
        JWTUserData principal = JWTUserData.builder().userId( userId ).email( "tester@mail.com" ).build();

        return authentication( new UsernamePasswordAuthenticationToken( principal, null,
                java.util.Arrays.stream( authorities ).map( SimpleGrantedAuthority::new ).toList() ) );
    }
}
