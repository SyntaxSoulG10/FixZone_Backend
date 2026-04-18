package com.fixzone.fixzon_backend.DTO;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AnalyticsDTO {
    private UUID id;
    private String metrics;
    private LocalDate date;
    private UUID serviceCenterId;
}
