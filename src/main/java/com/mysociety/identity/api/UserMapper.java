package com.mysociety.identity.api;

import com.mysociety.identity.domain.AppUser;
import com.mysociety.identity.domain.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static com.mysociety.identity.api.AuthDtos.RoleResponse;
import static com.mysociety.identity.api.AuthDtos.UserResponse;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "scopes", ignore = true)
    UserResponse toResponse(AppUser user);

    RoleResponse toResponse(Role role);
}
