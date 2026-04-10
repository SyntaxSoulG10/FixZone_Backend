package com.fixzone.fixzon_backend.DTO.customerprofile;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomerSettingsResponse {
    private String language;
    private boolean notificationsEnabled;
}
