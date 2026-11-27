package com.xueyifang.controller;

import com.xueyifang.common.BaseResponse;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.domain.po.Campus;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.ServiceReview;
import com.xueyifang.domain.po.ServiceTag;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.CreatorLeaderboardItemVO;
import com.xueyifang.service.ICampusService;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.service.IServiceReviewService;
import com.xueyifang.service.IServiceService;
import com.xueyifang.service.IServiceTagService;
import com.xueyifang.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CreatorLeaderboardTest {

    @Mock
    private IUserService userService;

    @Mock
    private IProfessionalService professionalService;

    @Mock
    private ICampusService campusService;

    @Mock
    private IServiceService serviceService;

    @Mock
    private IServiceTagService serviceTagService;

    @Mock
    private IServiceOrderService serviceOrderService;

    @Mock
    private IServiceReviewService serviceReviewService;

    @InjectMocks
    private CreatorProfileController creatorProfileController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getCreatorLeaderboard_computesCompositeInfluenceScoreAwardsBadgesAndRanksCorrectly() {
        User u1 = new User()
                .setId(101L)
                .setNickname("陈学姐")
                .setCampusId(1L)
                .setProfessionalId(10L)
                .setPublishPermission(1)
                .setAccountStatus(1)
                .setEduVerified(1)
                .setCreditScore(115)
                .setSellerDeposit(new BigDecimal("100.00"));

        User u2 = new User()
                .setId(102L)
                .setNickname("刘同学")
                .setCampusId(1L)
                .setProfessionalId(10L)
                .setPublishPermission(1)
                .setAccountStatus(1)
                .setEduVerified(0)
                .setCreditScore(98)
                .setSellerDeposit(BigDecimal.ZERO);

        when(userService.list(any())).thenReturn(Arrays.asList(u2, u1));
        when(campusService.list()).thenReturn(Collections.singletonList(new Campus().setId(1L).setCampusName("中心校区")));
        when(professionalService.list()).thenReturn(Collections.singletonList(new Professional().setId(10L).setProfessionalName("计算机科学与技术")));
        when(serviceTagService.list()).thenReturn(Collections.singletonList(new ServiceTag().setId(5L).setTagName("人工智能与算法")));

        Service s1 = new Service().setId(501L).setSellerId(101L).setTagId(5L).setStatus(1);
        when(serviceService.list(any())).thenReturn(Collections.singletonList(s1));

        ServiceOrder o1 = new ServiceOrder().setId(801L).setSellerId(101L).setOrderStatus(OrderConstant.ORDER_COMPLETED).setTotalAmount(new BigDecimal("90.00"));
        ServiceOrder o2 = new ServiceOrder().setId(802L).setSellerId(101L).setOrderStatus(OrderConstant.ORDER_COMPLETED).setTotalAmount(new BigDecimal("60.00"));
        ServiceOrder o3 = new ServiceOrder().setId(803L).setSellerId(101L).setOrderStatus(OrderConstant.ORDER_COMPLETED).setTotalAmount(new BigDecimal("120.00"));
        when(serviceOrderService.list(any())).thenReturn(Arrays.asList(o1, o2, o3));

        ServiceReview r1 = new ServiceReview().setId(901L).setSellerId(101L).setRating(5);
        when(serviceReviewService.list(any())).thenReturn(Collections.singletonList(r1));

        BaseResponse<List<CreatorLeaderboardItemVO>> res = creatorProfileController.getCreatorLeaderboard(null, null, "COMPREHENSIVE", 10);

        assertNotNull(res);
        List<CreatorLeaderboardItemVO> list = res.getData();
        assertEquals(2, list.size());

        // 陈学姐综合影响力更高，排第 1 名
        CreatorLeaderboardItemVO top1 = list.get(0);
        assertEquals(1, top1.getRank());
        assertEquals(101L, top1.getUserId());
        assertEquals(3, top1.getCompletedOrderCount());
        assertTrue(top1.getCompositeInfluenceScore().compareTo(list.get(1).getCompositeInfluenceScore()) > 0);
        // 应点亮 5 枚荣誉勋章 (EDU_SCHOLAR, DEPOSIT_GUARDIAN, HIGH_CREDIT_MODEL, FIVE_STAR_MENTOR, PRACTICE_PIONEER)
        assertEquals(5, top1.getBadges().size());
    }
}
