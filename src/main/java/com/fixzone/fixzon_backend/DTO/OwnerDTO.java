package com.fixzone.fixzon_backend.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OwnerDTO extends UserDTO {
    private String ownerCode;

    @NotBlank(message = "Company name is required")
    @jakarta.validation.constraints.Size(min = 3, max = 150, message = "Company name must be 3-150 characters")
    private String companyName;

    @jakarta.validation.constraints.Email(message = "Invalid company email format")
    private String companyEmail;

    @jakarta.validation.constraints.Pattern(regexp = "^[0-9+]{10,15}$", message = "Company phone must be 10-15 digits")
    private String companyNumber;
    private String passwordHash;
    private String bannerImageUrl;
}
