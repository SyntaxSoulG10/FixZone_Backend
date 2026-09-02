package com.fixzone.fixzon_backend.DTO;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class NotificationDTO {
    private UUID id;
    private String title;
    private String message;
    private String type;
    @JsonProperty("isRead")
    private boolean isRead;
    @JsonProperty("isArchived")
    private boolean isArchived;
    private LocalDateTime createdAt;
    private UUID recipientId;
    private String targetUrl;
}
