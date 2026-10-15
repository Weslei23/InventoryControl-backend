package com.wsdev.simplestock.domain.user.dto.request;

import com.wsdev.simplestock.domain.user.model.enums.Permission;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class UpdatePermissionsRequestDTO
{
    @NotNull( message = "argument 'permissions' must not be null." )
    private Set<Permission> permissions;
}
