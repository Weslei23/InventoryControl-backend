package com.wsdev.simplestock.domain.user.dto.response;

import com.wsdev.simplestock.domain.user.model.enums.Permission;
import com.wsdev.simplestock.domain.user.model.enums.Role;
import lombok.Data;

import java.util.Set;
import java.util.TreeSet;

@Data
public class UserResponseDTO
{
    private Long id;
    private String username;
    private String email;
    private Role role;
    private Set<Permission> permissions = new TreeSet<>();
    private Set<Permission> effectivePermissions = new TreeSet<>();
}
