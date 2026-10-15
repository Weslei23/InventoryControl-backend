package com.wsdev.simplestock.domain.user;

import com.wsdev.simplestock.common.config.TokenConfig;
import com.wsdev.simplestock.common.exceptions.EmailAlreadyInUseException;
import com.wsdev.simplestock.domain.user.dto.request.LoginRequestDTO;
import com.wsdev.simplestock.domain.user.dto.request.RegisterUserRequestDTO;
import com.wsdev.simplestock.domain.user.dto.response.LoginResponseDTO;
import com.wsdev.simplestock.domain.user.dto.response.RegisterUserResponseDTO;
import com.wsdev.simplestock.domain.user.mapper.RegisterMapper;
import com.wsdev.simplestock.domain.user.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenConfig tokenConfig;

    /**
     * Login
     * @param loginRequestDTO
     * @return
     */
    public LoginResponseDTO login( LoginRequestDTO loginRequestDTO )
    {
        UsernamePasswordAuthenticationToken userAndPass = new UsernamePasswordAuthenticationToken( loginRequestDTO.getEmail(), loginRequestDTO.getPassword() );
        Authentication authentication = authenticationManager.authenticate( userAndPass );

        User user = (User) authentication.getPrincipal();

        String token = tokenConfig.getSecretKey( user );

        return new LoginResponseDTO( token );
    }

    /**
     * Register
     * @param registerUserRequestDTO
     * @return
     */
    public RegisterUserResponseDTO register( RegisterUserRequestDTO registerUserRequestDTO )
    {
        if ( userRepository.existsByEmail( registerUserRequestDTO.getEmail() ) )
        {
            throw new EmailAlreadyInUseException( registerUserRequestDTO.getEmail() );
        }

        User newUser = RegisterMapper.dtoToEntity( registerUserRequestDTO );

        newUser.setPassword( passwordEncoder.encode( registerUserRequestDTO.getPassword() ) );

        userRepository.save( newUser );

        return RegisterMapper.entityToDto( newUser );
    }
}