package com.wsdev.simplestock.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.wsdev.simplestock.domain.user.UserRepository;
import com.wsdev.simplestock.domain.user.model.User;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class SecurityFilter extends OncePerRequestFilter
{
    @Autowired
    private TokenConfig tokenConfig;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain ) throws ServletException, IOException
    {
        String authorizedHeader = request.getHeader( "Authorization" );

        if ( Strings.isNotEmpty( authorizedHeader ) && authorizedHeader.startsWith( "Bearer " ) )
        {
            String token = authorizedHeader.substring( "Bearer ".length() );

            Optional<JWTUserData> optUser = tokenConfig.validateToken( token );

            if ( optUser.isPresent() )
            {
                JWTUserData user = optUser.get();

                Optional<User> dbUser = userRepository.findById( user.getUserId() );

                if ( dbUser.isPresent() )
                {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken( user, null, dbUser.get().getAuthorities() );

                    SecurityContextHolder.getContext().setAuthentication( authenticationToken );
                }
            }

            filterChain.doFilter( request, response );
        }
        else
        {
            filterChain.doFilter( request, response );
        }
    }
}