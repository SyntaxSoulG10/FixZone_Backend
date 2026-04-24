package com.fixzone.fixzon_backend.DTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OwnerDTO extends UserDTO {
    private String ownerCode;
    private String companyName;
    private String companyEmail;
    private String companyNumber;
    private String passwordHash;
    private String bannerImageUrl;
}
