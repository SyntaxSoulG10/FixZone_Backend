package com.fixzone.fixzon_backend.DTO.customerprofile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCustomerSettingsRequest {

    @NotBlank(message = "Language is required")
    @Size(max = 20, message = "Language must be at most 20 characters")
    private String language;

    @NotNull(message = "notificationsEnabled is required")
    private Boolean notificationsEnabled;
}
