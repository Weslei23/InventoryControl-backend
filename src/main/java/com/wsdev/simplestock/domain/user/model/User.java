package com.wsdev.simplestock.domain.user.model;

import com.wsdev.simplestock.domain.movement.model.Movement;
import com.wsdev.simplestock.domain.user.model.enums.Permission;
import com.wsdev.simplestock.domain.user.model.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@Table( name = "tb_users" )
public class User implements UserDetails
{
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    private Long id;
    private String email;
    private String username;
    private String password;

    @Enumerated( EnumType.STRING )
    private Role role = Role.USER;

    @ElementCollection( fetch = FetchType.EAGER )
    @CollectionTable( name = "tb_user_permissions", joinColumns = @JoinColumn( name = "user_id" ) )
    @Column( name = "permission" )
    @Enumerated( EnumType.STRING )
    private Set<Permission> permissions = new HashSet<>();

    @OneToMany( mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true )
    private List<Movement> movements = new ArrayList<>();

    public Role getRole()
    {
        return role == null ? Role.USER : role;
    }

    public Set<Permission> getEffectivePermissions()
    {
        Set<Permission> effective = EnumSet.noneOf( Permission.class );

        effective.addAll( getRole().defaultPermissions() );

        if ( permissions != null )
        {
            effective.addAll( permissions );
        }

        return effective;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        List<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add( new SimpleGrantedAuthority( getRole().authority() ) );
        getEffectivePermissions().forEach( permission -> authorities.add( new SimpleGrantedAuthority( permission.name() ) ) );

        return authorities;
    }

    @Override
    public boolean isAccountNonExpired()
    {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled()
    {
        return UserDetails.super.isEnabled();
    }
}