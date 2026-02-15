package com.fixzone.fixzon_backend.DTO;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerDTO {
    private Long id;
    private String name;
    private String email;
    private Integer visits;
    private BigDecimal totalSpent;
    private LocalDateTime lastVisit;
    private String status;
    private String avatarUrl;
}
