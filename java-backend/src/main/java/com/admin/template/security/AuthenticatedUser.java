package com.admin.template.security;

import java.util.List;

public record AuthenticatedUser(long id, String username, List<String> roles) {
    public AuthenticatedUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
