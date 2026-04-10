package com.fixzone.fixzon_backend.DTO.customerprofile;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomerProfileResponse {
    private String name;
    private String email;
    private String phone;
    private String profileImageUrl;
}
