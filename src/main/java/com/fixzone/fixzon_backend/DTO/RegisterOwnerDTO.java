package com.fixzone.fixzon_backend.DTO;

import lombok.Data;

@Data
public class RegisterOwnerDTO {
    @jakarta.validation.constraints.NotBlank(message = "Company name is required")
    @jakarta.validation.constraints.Size(min = 3, message = "Company name must be at least 3 characters")
    private String companyName;

    @jakarta.validation.constraints.NotBlank(message = "Company number is required")
    @jakarta.validation.constraints.Pattern(regexp = "^[0-9+]{10,15}$", message = "Invalid phone format")
    private String companyNumber;

    @jakarta.validation.constraints.NotBlank(message = "Full name is required")
    private String fullName;

    @jakarta.validation.constraints.NotBlank(message = "Email is required")
    @jakarta.validation.constraints.Email(message = "Invalid email format")
    private String email;

    @jakarta.validation.constraints.NotBlank(message = "Password is required")
    @jakarta.validation.constraints.Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
