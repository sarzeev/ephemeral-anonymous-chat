package com.example.chatbackend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SessionBootstrapRequest(
        @NotBlank
        @Size(min = 3, max = 8)
        @Pattern(regexp = "^[A-Za-z0-9]{3,8}$")
        String tempUserId,

        @NotBlank
        @Pattern(regexp = "^\\d{4,8}$")
        String joinToken
) {
}

