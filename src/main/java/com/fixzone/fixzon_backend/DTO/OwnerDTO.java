package com.fixzone.fixzon_backend.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OwnerDTO extends UserDTO {
    private String ownerCode;

    @NotBlank(message = "Company name is required")
    private String companyName;

    private String companyEmail;
    private String companyNumber;
    private String passwordHash;
    private String bannerImageUrl;
}
