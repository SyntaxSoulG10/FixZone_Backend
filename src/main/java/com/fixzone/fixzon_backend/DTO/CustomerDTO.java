package com.fixzone.fixzon_backend.DTO;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerDTO extends UserDTO {
    private String customerCode;
    private String preferredContactMethod;
    private Integer visits;
    private BigDecimal totalSpent;
}

