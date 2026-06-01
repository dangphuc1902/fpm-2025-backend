package com.fpm2025.user_auth_service.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateFamilyRequest {
    @NotBlank(message = "Family name cannot be blank")
    private String name;
    
    private String description;
}
