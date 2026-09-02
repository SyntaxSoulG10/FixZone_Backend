package com.fixzone.fixzon_backend.DTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SuperAdminDTO extends UserDTO {
    private String adminCode;
    private String passwordHash;
}
