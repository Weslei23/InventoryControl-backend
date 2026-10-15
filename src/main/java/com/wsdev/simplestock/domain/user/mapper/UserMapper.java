package com.wsdev.simplestock.domain.user.mapper;

import com.wsdev.simplestock.domain.user.dto.response.UserResponseDTO;
import com.wsdev.simplestock.domain.user.model.User;

import java.util.TreeSet;

public class UserMapper
{
    /**
     * Entity to dto
     *
     * @param user
     * @return
     */
    public static UserResponseDTO entityToDto( User user )
    {
        UserResponseDTO dto = new UserResponseDTO();

        dto.setId( user.getId() );
        dto.setUsername( user.getUsername() );
        dto.setEmail( user.getEmail() );
        dto.setRole( user.getRole() );
        dto.setPermissions( new TreeSet<>( user.getPermissions() ) );
        dto.setEffectivePermissions( new TreeSet<>( user.getEffectivePermissions() ) );

        return dto;
    }
}
