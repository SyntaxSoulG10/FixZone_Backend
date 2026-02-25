package com.fixzone.fixzon_backend.DTO;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class ManagerDTO extends UserDTO {
    private String managerCode;
    private UUID managedCenterId;
    private String passwordHash; // Keep for creation/updates if needed
}
