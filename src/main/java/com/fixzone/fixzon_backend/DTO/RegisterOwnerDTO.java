package com.fixzone.fixzon_backend.DTO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterOwnerDTO {
    @NotBlank(message = "Company name is required")
    @Size(min = 3, message = "Company name must be at least 3 characters")
    private String companyName;

    @NotBlank(message = "Company number is required")
    @Pattern(regexp = "^[0-9+]{10,15}$", message = "Invalid phone format")
    private String companyNumber;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
