package com.fixzone.fixzon_backend.DTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerDTO extends UserDTO {
    private String customerCode;
    private String preferredContactMethod;
}
