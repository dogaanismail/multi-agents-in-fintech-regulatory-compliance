package org.banksolution.model.response;

import java.util.List;

public record CurrentUserResponse(
        String username,
        String fullName,
        String email,
        List<String> roles) {
}
