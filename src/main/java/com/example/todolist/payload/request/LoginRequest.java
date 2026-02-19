package com.example.todolist.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String username;

    private String email;

    @NotBlank
    private String password;
}
