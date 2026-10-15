package com.wsdev.simplestock.domain.user.dto.request;

import com.wsdev.simplestock.domain.user.model.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRoleRequestDTO
{
    @NotNull( message = "argument 'role' must not be null." )
    private Role role;
}
