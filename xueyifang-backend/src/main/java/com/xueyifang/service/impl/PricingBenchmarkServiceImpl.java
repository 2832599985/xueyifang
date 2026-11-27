package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.ServiceTag;
import com.xueyifang.domain.vo.PricingBenchmarkVO;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.ServiceTagMapper;
import com.xueyifang.service.IPricingBenchmarkService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PricingBenchmarkServiceImpl implements IPricingBenchmarkService {

    @Resource
    private ServiceMapper serviceMapper;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private ServiceTagMapper serviceTagMapper;

    @Override
    public PricingBenchmarkVO getPricingBenchmark(Long tagId, Long campusId, Integer tradeType, BigDecimal proposedPrice) {
        PricingBenchmarkVO vo = new PricingBenchmarkVO()
                .setTagId(tagId)
                .setCampusId(campusId)
                .setTradeType(tradeType)
                .setProposedPrice(proposedPrice);

        if (tagId != null) {
            ServiceTag tag = serviceTagMapper.selectById(tagId);
            if (tag != null) {
                vo.setTagName(tag.getTagName());
            }
        }
        if (vo.getTagName() == null) {
            vo.setTagName("全校通用技能服务");
        }

        // 1. 查询符合分类、校区、交易方式的在售/历史技能服务
        LambdaQueryWrapper<Service> serviceQuery = new LambdaQueryWrapper<>();
        serviceQuery.eq(Service::getIsDelete, 0);
        if (tagId != null && tagId > 0) {
            serviceQuery.eq(Service::getTagId, tagId);
        }
        if (campusId != null && campusId > 0) {
            serviceQuery.eq(Service::getCampusId, campusId);
        }
        if (tradeType != null && tradeType > 0) {
            serviceQuery.eq(Service::getTradeType, tradeType);
        }
        List<Service> matchingServices = serviceMapper.selectList(serviceQuery);
        if (matchingServices == null) {
            matchingServices = Collections.emptyList();
        }

        List<BigDecimal> samplePrices = new ArrayList<>();
        int activeListingCount = 0;
        for (Service svc : matchingServices) {
            if (svc.getPrice() != null && svc.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                if (svc.getStatus() != null && svc.getStatus() == 1) {
                    samplePrices.add(svc.getPrice());
                    activeListingCount++;
                }
            }
        }

        // 2. 查询历史已支付/已完结真实成交订单（赋予 2 倍权重，确保真实成交价主导分位数）
        int completedOrderCount = 0;
        Set<Long> serviceIds = matchingServices.stream()
                .map(Service::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (!serviceIds.isEmpty() || (tagId == null || tagId <= 0)) {
            LambdaQueryWrapper<ServiceOrder> orderQuery = new LambdaQueryWrapper<>();
            orderQuery.eq(ServiceOrder::getIsDelete, 0)
                    .eq(ServiceOrder::getPaymentStatus, OrderConstant.PAYMENT_PAID);
            if (!serviceIds.isEmpty()) {
                orderQuery.in(ServiceOrder::getServiceId, serviceIds);
            } else {
                if (campusId != null && campusId > 0) {
                    orderQuery.eq(ServiceOrder::getCampusId, campusId);
                }
                if (tradeType != null && tradeType > 0) {
                    orderQuery.eq(ServiceOrder::getTradeType, tradeType);
                }
            }
            orderQuery.last("LIMIT 300");
            List<ServiceOrder> paidOrders = serviceOrderMapper.selectList(orderQuery);
            if (paidOrders != null) {
                for (ServiceOrder order : paidOrders) {
                    BigDecimal unitPrice = order.getUnitPrice() != null ? order.getUnitPrice() : order.getTotalAmount();
                    if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
                        completedOrderCount++;
                        // 真实成交样本双倍加权
                        samplePrices.add(unitPrice);
                        samplePrices.add(unitPrice);
                    }
                }
            }
        }

        // 3. 若新分类冷启动样本不足 4 个，补充分类基准锚点曲线以提供平滑分位参考
        if (samplePrices.size() < 4) {
            List<BigDecimal> coldStartBaseline = Arrays.asList(
                    new BigDecimal("25.00"),
                    new BigDecimal("35.00"),
                    new BigDecimal("50.00"),
                    new BigDecimal("68.00"),
                    new BigDecimal("95.00"),
                    new BigDecimal("140.00")
            );
            samplePrices.addAll(coldStartBaseline);
        }

        Collections.sort(samplePrices);

        BigDecimal minPrice = samplePrices.get(0).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxPrice = samplePrices.get(samplePrices.size() - 1).setScale(2, RoundingMode.HALF_UP);
        BigDecimal p25 = computePercentile(samplePrices, 0.25);
        BigDecimal p50 = computePercentile(samplePrices, 0.50);
        BigDecimal p75 = computePercentile(samplePrices, 0.75);
        BigDecimal p90 = computePercentile(samplePrices, 0.90);

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal price : samplePrices) {
            sum = sum.add(price);
        }
        BigDecimal avgPrice = sum.divide(BigDecimal.valueOf(samplePrices.size()), 2, RoundingMode.HALF_UP);

        vo.setCompletedOrderSampleCount(completedOrderCount)
                .setActiveListingSampleCount(activeListingCount)
                .setTotalSampleCount(completedOrderCount + activeListingCount)
                .setMinPrice(minPrice)
                .setP25Price(p25)
                .setP50Price(p50)
                .setP75Price(p75)
                .setP90Price(p90)
                .setMaxPrice(maxPrice)
                .setAvgPrice(avgPrice)
                .setRecommendedMinPrice(p25)
                .setRecommendedMaxPrice(p75);

        // 4. 针对创作者输入的拟定价格或买方查看的价格进行分位诊断
        BigDecimal evalPrice = (proposedPrice != null && proposedPrice.compareTo(BigDecimal.ZERO) > 0)
                ? proposedPrice.setScale(2, RoundingMode.HALF_UP)
                : p50;
        vo.setProposedPrice(evalPrice);

        long countBelowOrEqual = samplePrices.stream()
                .filter(p -> p.compareTo(evalPrice) <= 0)
                .count();
        int percentileRank = (int) Math.min(99, Math.max(1, Math.round((countBelowOrEqual * 100.0) / samplePrices.size())));
        vo.setPercentileRank(percentileRank);

        if (evalPrice.compareTo(p25) < 0) {
            vo.setPricePositionCode("BELOW_P25_VALUE")
                    .setPricePositionLabel("高性价比引流区（低于 P25）")
                    .setConversionAdvice("当前定价极具价格竞争力，预计首单转化速度提升约 35%，适合新晋创作者快速积累首批真实好评与履约信用分。");
        } else if (evalPrice.compareTo(p75) <= 0) {
            vo.setPricePositionCode("SWEET_SPOT_P25_P75")
                    .setPricePositionLabel("校内黄金成交区间（P25 ~ P75）")
                    .setConversionAdvice("当前定价精准处于本校区同类技能的主流成交中枢（中位价 ¥" + p50 + "），兼顾出单转化率与劳务时间回报。");
        } else if (evalPrice.compareTo(p90) <= 0) {
            vo.setPricePositionCode("PREMIUM_P75_P90")
                    .setPricePositionLabel("资深创作者品质溢价区（P75 ~ P90）")
                    .setConversionAdvice("当前定价高于校内 75% 同类服务，建议在详情中展示代表作品集、竞赛证书或承诺免费微调次数以支撑溢价。");
        } else {
            vo.setPricePositionCode("ABOVE_P90_OUTLIER")
                    .setPricePositionLabel("高客单定制顶配区（高于 P90）")
                    .setConversionAdvice("当前定价已超过校内 90% 同类成交样本，强烈建议开启「分阶段里程碑托管结算」并缴纳创作者履约保证金以消除买方顾虑。");
        }

        return vo;
    }

    /**
     * 线性插值计算经验分位数
     */
    public static BigDecimal computePercentile(List<BigDecimal> sorted, double percentile) {
        if (sorted == null || sorted.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (sorted.size() == 1) {
            return sorted.get(0).setScale(2, RoundingMode.HALF_UP);
        }
        double pos = percentile * (sorted.size() - 1);
        int lowerIndex = (int) Math.floor(pos);
        int upperIndex = (int) Math.ceil(pos);
        if (lowerIndex == upperIndex) {
            return sorted.get(lowerIndex).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal lowerVal = sorted.get(lowerIndex);
        BigDecimal upperVal = sorted.get(upperIndex);
        BigDecimal weight = BigDecimal.valueOf(pos - lowerIndex);
        return lowerVal.add(upperVal.subtract(lowerVal).multiply(weight)).setScale(2, RoundingMode.HALF_UP);
    }
}
