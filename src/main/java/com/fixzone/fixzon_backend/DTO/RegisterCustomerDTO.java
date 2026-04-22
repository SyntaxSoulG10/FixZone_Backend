package com.fixzone.fixzon_backend.DTO;

import lombok.Data;

@Data
public class RegisterCustomerDTO {
    private String fullName;
    private String email;
    private String password;
}
