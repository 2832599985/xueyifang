package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ServiceSlotBatchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<SlotItem> slots;

    @Data
    public static class SlotItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private LocalDateTime slotStart;
        private LocalDateTime slotEnd;
        private Integer maxCapacity;
    }
}
