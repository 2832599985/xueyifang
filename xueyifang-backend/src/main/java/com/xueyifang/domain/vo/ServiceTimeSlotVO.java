package com.xueyifang.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ServiceTimeSlotVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long serviceId;
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;
    private Integer maxCapacity;
    private Integer bookedCount;
    private Integer remainingCapacity;
    private Integer status;
}
