package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderMilestonePlanRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<StageItem> stages;

    @Data
    public static class StageItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String title;
        private BigDecimal amount;
    }
}
