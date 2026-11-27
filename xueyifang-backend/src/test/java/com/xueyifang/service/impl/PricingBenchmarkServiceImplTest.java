package com.xueyifang.service.impl;

import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.ServiceTag;
import com.xueyifang.domain.vo.PricingBenchmarkVO;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.ServiceTagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PricingBenchmarkServiceImplTest {

    @Mock
    private ServiceMapper serviceMapper;

    @Mock
    private ServiceOrderMapper serviceOrderMapper;

    @Mock
    private ServiceTagMapper serviceTagMapper;

    @InjectMocks
    private PricingBenchmarkServiceImpl pricingBenchmarkService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getPricingBenchmark_computesEmpiricalPercentilesAndDiagnosesPricePosition() {
        ServiceTag tag = new ServiceTag();
        tag.setId(10L);
        tag.setTagName("Python/算法辅导");
        when(serviceTagMapper.selectById(10L)).thenReturn(tag);

        Service s1 = new Service().setId(101L).setTagId(10L).setStatus(1).setPrice(new BigDecimal("40.00"));
        Service s2 = new Service().setId(102L).setTagId(10L).setStatus(1).setPrice(new BigDecimal("60.00"));
        Service s3 = new Service().setId(103L).setTagId(10L).setStatus(1).setPrice(new BigDecimal("80.00"));
        Service s4 = new Service().setId(104L).setTagId(10L).setStatus(1).setPrice(new BigDecimal("120.00"));
        when(serviceMapper.selectList(any())).thenReturn(Arrays.asList(s1, s2, s3, s4));

        ServiceOrder o1 = new ServiceOrder().setId(201L).setServiceId(101L).setPaymentStatus(OrderConstant.PAYMENT_PAID).setUnitPrice(new BigDecimal("50.00"));
        ServiceOrder o2 = new ServiceOrder().setId(202L).setServiceId(102L).setPaymentStatus(OrderConstant.PAYMENT_PAID).setUnitPrice(new BigDecimal("70.00"));
        ServiceOrder o3 = new ServiceOrder().setId(203L).setServiceId(103L).setPaymentStatus(OrderConstant.PAYMENT_PAID).setUnitPrice(new BigDecimal("90.00"));
        when(serviceOrderMapper.selectList(any())).thenReturn(Arrays.asList(o1, o2, o3));

        // 测算处于黄金区间 [P25, P75] 内的 65 元报价
        PricingBenchmarkVO sweetSpot = pricingBenchmarkService.getPricingBenchmark(10L, 1L, 1, new BigDecimal("65.00"));
        assertNotNull(sweetSpot);
        assertEquals("Python/算法辅导", sweetSpot.getTagName());
        assertEquals(3, sweetSpot.getCompletedOrderSampleCount());
        assertEquals(4, sweetSpot.getActiveListingSampleCount());
        assertEquals(7, sweetSpot.getTotalSampleCount());
        assertTrue(sweetSpot.getP25Price().compareTo(sweetSpot.getP50Price()) <= 0);
        assertTrue(sweetSpot.getP50Price().compareTo(sweetSpot.getP75Price()) <= 0);
        assertTrue(sweetSpot.getP75Price().compareTo(sweetSpot.getP90Price()) <= 0);
        assertEquals("SWEET_SPOT_P25_P75", sweetSpot.getPricePositionCode());

        // 测算低于 P25 的高性价比引流价 30 元
        PricingBenchmarkVO valuePos = pricingBenchmarkService.getPricingBenchmark(10L, 1L, 1, new BigDecimal("30.00"));
        assertEquals("BELOW_P25_VALUE", valuePos.getPricePositionCode());

        // 测算高于 P90 的高客单价 200 元
        PricingBenchmarkVO outlierPos = pricingBenchmarkService.getPricingBenchmark(10L, 1L, 1, new BigDecimal("200.00"));
        assertEquals("ABOVE_P90_OUTLIER", outlierPos.getPricePositionCode());
    }
}
