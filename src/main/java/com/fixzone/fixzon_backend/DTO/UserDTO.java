package com.fixzone.fixzon_backend.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID userId;

    @NotBlank(message = "Full name is required")
    @jakarta.validation.constraints.Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @jakarta.validation.constraints.Pattern(
        regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        message = "Invalid email format (e.g. name@example.com)"
    )
    private String email;

    @jakarta.validation.constraints.Pattern(regexp = "^$|^[0-9+()\\s-]{9,20}$", message = "Phone must be 9-20 digits")
    private String phone;
    private String role;
    private Boolean emailVerified;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String status;
    private String profilePictureUrl;
}
