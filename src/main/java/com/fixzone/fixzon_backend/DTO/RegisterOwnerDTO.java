package com.fixzone.fixzon_backend.DTO;

import lombok.Data;

@Data
public class RegisterOwnerDTO {
    private String companyName;
    private String companyNumber;
    private String fullName;
    private String email;
    private String password;
}
