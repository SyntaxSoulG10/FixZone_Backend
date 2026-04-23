package com.fixzone.fixzon_backend.DTO;

import lombok.Data;

@Data
public class AuthRequestDTO {
    private String email;
    private String password;
}
