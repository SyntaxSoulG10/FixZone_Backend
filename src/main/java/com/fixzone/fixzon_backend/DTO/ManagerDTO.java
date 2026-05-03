package com.fixzone.fixzon_backend.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class ManagerDTO extends UserDTO {
    private String managerCode;
    
    @NotNull(message = "Managed center ID is required")
    private UUID managedCenterId;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordHash; // Keep for creation/updates but exclude from responses
    
    private Boolean sendInvite;
}

