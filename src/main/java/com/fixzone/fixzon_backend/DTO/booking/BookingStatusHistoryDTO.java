package com.fixzone.fixzon_backend.DTO.booking;

import com.fixzone.fixzon_backend.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusHistoryDTO {
    private UUID id;
    private UUID bookingId;
    private BookingStatus status;
    private String statusDisplay;
    private LocalDateTime changedAt;
    private String changedBy;
}
