package com.fixzone.fixzon_backend.DTO;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class ManagerDTO extends UserDTO {
    private String managerCode;
    
    @jakarta.validation.constraints.NotNull(message = "Managed center ID is required")
    private UUID managedCenterId;
    
    private String passwordHash; // Keep for creation/updates if needed
    private Boolean sendInvite;
}
