package com.wsdev.simplestock.domain.user;

import com.wsdev.simplestock.common.exceptions.ForbiddenOperationException;
import com.wsdev.simplestock.common.exceptions.RecordNotFoundException;
import com.wsdev.simplestock.domain.user.dto.response.UserResponseDTO;
import com.wsdev.simplestock.domain.user.mapper.UserMapper;
import com.wsdev.simplestock.domain.user.model.User;
import com.wsdev.simplestock.domain.user.model.enums.Permission;
import com.wsdev.simplestock.domain.user.model.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService
{
    @Autowired
    private UserRepository userRepository;

    /**
     * Get all users
     *
     * @param pageable
     * @return
     */
    public Page<UserResponseDTO> getUsers( Pageable pageable )
    {
        return userRepository.findAll( pageable ).map( UserMapper::entityToDto );
    }

    /**
     * Get user by id
     *
     * @param id
     * @return
     */
    public UserResponseDTO getUserById( Long id )
    {
        return UserMapper.entityToDto( findUser( id ) );
    }

    /**
     * Every permission that can be granted
     *
     * @return
     */
    public List<Permission> getAvailablePermissions()
    {
        return List.of( Permission.values() );
    }

    /**
     * Replace the permissions granted individually to a user.
     *
     * @param actor       who is making the change
     * @param targetId    user to be changed
     * @param permissions the new set of individual permissions
     * @return
     */
    @Transactional
    public UserResponseDTO updatePermissions( Authentication actor, Long targetId, Set<Permission> permissions )
    {
        User target = findUser( targetId );

        boolean actorIsSuperUser = hasAuthority( actor, Role.SUPER_USER.authority() );

        if ( target.getRole() == Role.SUPER_USER )
        {
            throw new ForbiddenOperationException( "The super user permissions cannot be changed." );
        }

        if ( isSameUser( actor, target ) )
        {
            throw new ForbiddenOperationException( "You cannot change your own permissions." );
        }

        if ( target.getRole() != Role.USER && !actorIsSuperUser )
        {
            throw new ForbiddenOperationException( "Only the super user can change the permissions of an admin." );
        }

        Set<Permission> requested = permissions == null ? new HashSet<>() : new HashSet<>( permissions );

        Set<Permission> changed = new HashSet<>( requested );
        changed.addAll( target.getPermissions() );
        Set<Permission> unchanged = new HashSet<>( requested );
        unchanged.retainAll( target.getPermissions() );
        changed.removeAll( unchanged );

        if ( !actorIsSuperUser )
        {
            for ( Permission permission : changed )
            {
                if ( permission.isSuperUserOnly() )
                {
                    throw new ForbiddenOperationException( "Only the super user can grant or revoke " + permission + "." );
                }

                if ( !hasAuthority( actor, permission.name() ) )
                {
                    throw new ForbiddenOperationException( "You cannot grant or revoke a permission you do not have: " + permission + "." );
                }
            }
        }

        target.setPermissions( requested );

        return UserMapper.entityToDto( userRepository.save( target ) );
    }

    /**
     * Change a user role between ADMIN and USER. Only reachable by the super user.
     *
     * @param actor    who is making the change
     * @param targetId user to be changed
     * @param role     new role
     * @return
     */
    @Transactional
    public UserResponseDTO updateRole( Authentication actor, Long targetId, Role role )
    {
        User target = findUser( targetId );

        if ( role == Role.SUPER_USER )
        {
            throw new ForbiddenOperationException( "The super user role cannot be assigned." );
        }

        if ( target.getRole() == Role.SUPER_USER )
        {
            throw new ForbiddenOperationException( "The super user role cannot be changed." );
        }

        if ( isSameUser( actor, target ) )
        {
            throw new ForbiddenOperationException( "You cannot change your own role." );
        }

        target.setRole( role );

        return UserMapper.entityToDto( userRepository.save( target ) );
    }

    private User findUser( Long id )
    {
        return userRepository.findById( id ).orElseThrow( () -> new RecordNotFoundException( id ) );
    }

    private boolean isSameUser( Authentication actor, User target )
    {
        return actor.getPrincipal() instanceof com.wsdev.simplestock.common.config.JWTUserData data
                && data.getUserId() != null
                && data.getUserId().equals( target.getId() );
    }

    private boolean hasAuthority( Authentication actor, String authority )
    {
        for ( GrantedAuthority granted : actor.getAuthorities() )
        {
            if ( authority.equals( granted.getAuthority() ) )
            {
                return true;
            }
        }

        return false;
    }
}
