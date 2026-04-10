package com.fixzone.fixzon_backend.DTO.customerprofile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCustomerProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    private String name;

    @Pattern(regexp = "^$|^[+0-9\\-\\s]{7,20}$", message = "Phone number format is invalid")
    private String phone;

    @Size(max = 500, message = "Profile image URL must be at most 500 characters")
    private String profileImageUrl;
}
