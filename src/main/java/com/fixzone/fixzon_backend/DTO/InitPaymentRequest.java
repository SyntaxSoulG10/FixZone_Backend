package com.fixzone.fixzon_backend.DTO;

import lombok.Data;
import java.util.UUID;

@Data
public class InitPaymentRequest {
    private UUID servicePackageId;
    private UUID vehicleId;
    private String date;
    private String timeSlot;
}
