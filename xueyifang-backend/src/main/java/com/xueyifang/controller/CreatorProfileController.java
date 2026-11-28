package com.xueyifang.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.ServiceConstant;
import com.xueyifang.domain.po.Campus;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.ServiceReview;
import com.xueyifang.domain.po.ServiceTag;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.CreatorLeaderboardItemVO;
import com.xueyifang.domain.vo.CreatorProfileVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.ICampusService;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.IServiceImageService;
import com.xueyifang.service.IServiceOrderService;
import com.xueyifang.service.IServiceReviewService;
import com.xueyifang.service.IServiceService;
import com.xueyifang.service.IServiceTagService;
import com.xueyifang.service.IUserService;
import com.xueyifang.utils.FileUrlResolver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/creator")
@Api(tags = "创作者公开数字信用档案、作品集与校友技能风云榜接口")
public class CreatorProfileController {

    @Resource
    private IUserService userService;

    @Resource
    private IProfessionalService professionalService;

    @Resource
    private ICampusService campusService;

    @Resource
    private IServiceService serviceService;

    @Resource
    private IServiceImageService serviceImageService;

    @Resource
    private IServiceTagService serviceTagService;

    @Resource
    private IServiceOrderService serviceOrderService;

    @Resource
    private IServiceReviewService serviceReviewService;

    @Resource
    private FileUrlResolver fileUrlResolver;

    @GetMapping("/leaderboard")
    @ApiOperation("查询校友创作者技能风云榜与多维能力等级勋章（支持按校区、分类及四大维度排序）")
    public BaseResponse<List<CreatorLeaderboardItemVO>> getCreatorLeaderboard(
            @RequestParam(value = "campusId", required = false) Long campusId,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "rankBy", defaultValue = "COMPREHENSIVE") String rankBy,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit
    ) {
        int maxLimit = (limit != null && limit > 0) ? Math.min(50, limit) : 20;

        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(User::getIsDelete, 0)
                .eq(User::getAccountStatus, 1)
                .eq(User::getPublishPermission, 1);
        if (campusId != null && campusId > 0) {
            userQuery.eq(User::getCampusId, campusId);
        }
        List<User> creators = userService.list(userQuery);
        if (creators == null || creators.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }

        Map<Long, String> campusNameMap = new HashMap<>();
        if (campusService != null) {
            List<Campus> campuses = campusService.list();
            if (campuses != null) {
                for (Campus c : campuses) {
                    campusNameMap.put(c.getId(), c.getCampusName());
                }
            }
        }

        Map<Long, String> profNameMap = new HashMap<>();
        if (professionalService != null) {
            List<Professional> profs = professionalService.list();
            if (profs != null) {
                for (Professional p : profs) {
                    profNameMap.put(p.getId(), p.getProfessionalName());
                }
            }
        }

        Map<Long, String> tagMap = new HashMap<>();
        if (serviceTagService != null) {
            List<ServiceTag> tags = serviceTagService.list();
            if (tags != null) {
                for (ServiceTag t : tags) {
                    tagMap.put(t.getId(), t.getTagName());
                }
            }
        }

        List<Long> creatorIds = creators.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Service> allOnlineServices = creatorIds.isEmpty() ? Collections.emptyList()
                : serviceService.list(new LambdaQueryWrapper<Service>()
                .select(Service::getId, Service::getSellerId, Service::getTagId, Service::getStatus)
                .in(Service::getSellerId, creatorIds)
                .eq(Service::getIsDelete, 0)
                .eq(Service::getStatus, ServiceConstant.STATUS_ONLINE)
                .notLike(Service::getServiceTitle, "【悬赏定向单】")
                .notLike(Service::getServiceTitle, "【拼班团课】"));
        if (allOnlineServices == null) {
            allOnlineServices = Collections.emptyList();
        }
        Map<Long, List<Service>> servicesBySeller = allOnlineServices.stream()
                .filter(s -> s.getSellerId() != null)
                .collect(Collectors.groupingBy(Service::getSellerId));

        List<ServiceOrder> completedOrders = creatorIds.isEmpty() ? Collections.emptyList()
                : serviceOrderService.list(new LambdaQueryWrapper<ServiceOrder>()
                .select(ServiceOrder::getId, ServiceOrder::getSellerId, ServiceOrder::getTotalAmount)
                .in(ServiceOrder::getSellerId, creatorIds)
                .eq(ServiceOrder::getIsDelete, 0)
                .eq(ServiceOrder::getOrderStatus, OrderConstant.ORDER_COMPLETED));
        if (completedOrders == null) {
            completedOrders = Collections.emptyList();
        }
        Map<Long, List<ServiceOrder>> ordersBySeller = completedOrders.stream()
                .filter(o -> o.getSellerId() != null)
                .collect(Collectors.groupingBy(ServiceOrder::getSellerId));

        List<ServiceReview> allReviews = creatorIds.isEmpty() ? Collections.emptyList()
                : serviceReviewService.list(new LambdaQueryWrapper<ServiceReview>()
                .select(ServiceReview::getId, ServiceReview::getSellerId, ServiceReview::getRating)
                .in(ServiceReview::getSellerId, creatorIds));
        if (allReviews == null) {
            allReviews = Collections.emptyList();
        }
        Map<Long, List<ServiceReview>> reviewsBySeller = allReviews.stream()
                .filter(r -> r.getSellerId() != null)
                .collect(Collectors.groupingBy(ServiceReview::getSellerId));

        List<CreatorLeaderboardItemVO> items = new ArrayList<>();
        for (User u : creators) {
            List<Service> sellerSvcs = servicesBySeller.getOrDefault(u.getId(), Collections.emptyList());
            if (tagId != null && tagId > 0) {
                boolean hasMatchingTag = sellerSvcs.stream().anyMatch(s -> tagId.equals(s.getTagId()));
                if (!hasMatchingTag) {
                    continue;
                }
            }

            List<ServiceOrder> sellerOrders = ordersBySeller.getOrDefault(u.getId(), Collections.emptyList());
            int compCount = sellerOrders.size();
            BigDecimal totalRevenue = BigDecimal.ZERO;
            for (ServiceOrder o : sellerOrders) {
                if (o.getTotalAmount() != null) {
                    totalRevenue = totalRevenue.add(o.getTotalAmount());
                }
            }
            BigDecimal practiceHours = totalRevenue.divide(new BigDecimal("30.00"), 1, RoundingMode.HALF_UP)
                    .add(BigDecimal.valueOf(compCount * 0.5))
                    .setScale(1, RoundingMode.HALF_UP);

            List<ServiceReview> sellerReviews = reviewsBySeller.getOrDefault(u.getId(), Collections.emptyList());
            int revCount = sellerReviews.size();
            double avgRatingVal = sellerReviews.stream()
                    .filter(r -> r.getRating() != null)
                    .mapToInt(ServiceReview::getRating)
                    .average()
                    .orElse(5.0);
            BigDecimal avgRating = BigDecimal.valueOf(avgRatingVal).setScale(2, RoundingMode.HALF_UP);

            int credit = u.getCreditScore() != null ? u.getCreditScore() : 100;
            int eduVer = u.getEduVerified() != null ? u.getEduVerified() : 0;
            BigDecimal deposit = u.getSellerDeposit() != null ? u.getSellerDeposit() : BigDecimal.ZERO;

            // 计算创作者多维综合影响力指数
            double rawInfluence = credit * 0.45
                    + compCount * 12.0
                    + avgRatingVal * 8.0
                    + (eduVer == 1 ? 15.0 : 0.0)
                    + (deposit.compareTo(BigDecimal.ZERO) > 0 ? 12.0 : 0.0)
                    + sellerSvcs.size() * 4.0;
            BigDecimal influenceScore = BigDecimal.valueOf(rawInfluence).setScale(1, RoundingMode.HALF_UP);

            Set<String> topTags = new LinkedHashSet<>();
            for (Service s : sellerSvcs) {
                if (s.getTagId() != null && tagMap.containsKey(s.getTagId())) {
                    topTags.add(tagMap.get(s.getTagId()));
                }
                if (topTags.size() >= 3) {
                    break;
                }
            }

            List<CreatorLeaderboardItemVO.CreatorBadgeVO> badges = buildCreatorBadges(eduVer, deposit, credit, avgRatingVal, revCount, compCount);
            String levelTitle = resolveCreatorLevelTitle(influenceScore.doubleValue(), compCount);

            CreatorLeaderboardItemVO item = new CreatorLeaderboardItemVO()
                    .setUserId(u.getId())
                    .setDisplayName(StringUtils.defaultIfBlank(u.getNickname(), StringUtils.defaultIfBlank(u.getRealName(), "校园创作者#" + u.getId())))
                    .setAvatar(resolvePublicAvatar(u.getAvatar()))
                    .setGrade(u.getGrade())
                    .setProfessionalName(u.getProfessionalId() != null ? profNameMap.getOrDefault(u.getProfessionalId(), "交叉学科创作者") : "校园认证创作者")
                    .setCampusId(u.getCampusId())
                    .setCampusName(u.getCampusId() != null ? campusNameMap.getOrDefault(u.getCampusId(), "主校区") : "主校区")
                    .setBio(StringUtils.defaultIfBlank(u.getBio(), "专注提供高质量校园技能担保交付与学业互助辅导。"))
                    .setEduVerified(eduVer)
                    .setCreditScore(credit)
                    .setCreatorLevelTitle(levelTitle)
                    .setSellerDeposit(deposit.setScale(2, RoundingMode.HALF_UP))
                    .setActiveServiceCount(sellerSvcs.size())
                    .setCompletedOrderCount(compCount)
                    .setTotalPracticeHours(practiceHours)
                    .setAverageRating(avgRating)
                    .setReviewCount(revCount)
                    .setCompositeInfluenceScore(influenceScore)
                    .setTopSkillTags(new ArrayList<>(topTags))
                    .setBadges(badges);

            items.add(item);
        }

        // 按指定维度排序
        Comparator<CreatorLeaderboardItemVO> comparator;
        if ("COMPLETED_ORDERS".equalsIgnoreCase(rankBy)) {
            comparator = Comparator.comparing(CreatorLeaderboardItemVO::getCompletedOrderCount).reversed()
                    .thenComparing(CreatorLeaderboardItemVO::getCompositeInfluenceScore, Comparator.reverseOrder());
        } else if ("CREDIT_SCORE".equalsIgnoreCase(rankBy)) {
            comparator = Comparator.comparing(CreatorLeaderboardItemVO::getCreditScore).reversed()
                    .thenComparing(CreatorLeaderboardItemVO::getCompositeInfluenceScore, Comparator.reverseOrder());
        } else if ("RATING".equalsIgnoreCase(rankBy)) {
            comparator = Comparator.comparing(CreatorLeaderboardItemVO::getAverageRating).reversed()
                    .thenComparing(CreatorLeaderboardItemVO::getReviewCount, Comparator.reverseOrder())
                    .thenComparing(CreatorLeaderboardItemVO::getCompositeInfluenceScore, Comparator.reverseOrder());
        } else {
            comparator = Comparator.comparing(CreatorLeaderboardItemVO::getCompositeInfluenceScore).reversed()
                    .thenComparing(CreatorLeaderboardItemVO::getCompletedOrderCount, Comparator.reverseOrder());
        }

        items.sort(comparator);
        if (items.size() > maxLimit) {
            items = new ArrayList<>(items.subList(0, maxLimit));
        }
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setRank(i + 1);
        }

        return ResultUtils.success(items);
    }

    private List<CreatorLeaderboardItemVO.CreatorBadgeVO> buildCreatorBadges(
            int eduVerified,
            BigDecimal deposit,
            int creditScore,
            double avgRating,
            int reviewCount,
            int completedOrders) {
        List<CreatorLeaderboardItemVO.CreatorBadgeVO> list = new ArrayList<>();
        if (eduVerified == 1) {
            list.add(new CreatorLeaderboardItemVO.CreatorBadgeVO()
                    .setBadgeCode("EDU_SCHOLAR")
                    .setBadgeName("教育邮箱双核认证")
                    .setBadgeTone("emerald")
                    .setBadgeDesc("已完成本校 .edu.cn 教育邮箱与学籍实名双重核验"));
        }
        if (deposit != null && deposit.compareTo(new BigDecimal("50.00")) >= 0) {
            list.add(new CreatorLeaderboardItemVO.CreatorBadgeVO()
                    .setBadgeCode("DEPOSIT_GUARDIAN")
                    .setBadgeName("保证金护航导师")
                    .setBadgeTone("gold")
                    .setBadgeDesc("已缴纳足额创作者履约保证金，支持爽约自动退一赔十"));
        }
        if (creditScore >= 105) {
            list.add(new CreatorLeaderboardItemVO.CreatorBadgeVO()
                    .setBadgeCode("HIGH_CREDIT_MODEL")
                    .setBadgeName("校园信用标杆")
                    .setBadgeTone("blue")
                    .setBadgeDesc("校园履约信用分达 105+，零违规零爽约记录"));
        }
        if (avgRating >= 4.8 && (reviewCount >= 1 || completedOrders >= 1)) {
            list.add(new CreatorLeaderboardItemVO.CreatorBadgeVO()
                    .setBadgeCode("FIVE_STAR_MENTOR")
                    .setBadgeName("五星口碑创作者")
                    .setBadgeTone("purple")
                    .setBadgeDesc("历史真实完单综合评分保持 4.8 星以上"));
        }
        if (completedOrders >= 3) {
            list.add(new CreatorLeaderboardItemVO.CreatorBadgeVO()
                    .setBadgeCode("PRACTICE_PIONEER")
                    .setBadgeName("实战交付先锋")
                    .setBadgeTone("gold")
                    .setBadgeDesc("累计完成多笔校园技能担保交付并签发 SHA-256 存证"));
        }
        return list;
    }

    private String resolveCreatorLevelTitle(double influence, int completedOrders) {
        if (influence >= 160 || completedOrders >= 8) {
            return "Lv.5 殿堂首席导师";
        }
        if (influence >= 130 || completedOrders >= 4) {
            return "Lv.4 资深金牌创作者";
        }
        if (influence >= 110 || completedOrders >= 2) {
            return "Lv.3 校园骨干行家";
        }
        if (influence >= 95 || completedOrders >= 1) {
            return "Lv.2 进阶实干能手";
        }
        return "Lv.1 新锐认证创作者";
    }

    @GetMapping("/{userId}/profile")
    @ApiOperation("查询创作者公开数字信用档案、在线技能与已核验成交评价（严格隐私脱敏）")
    public BaseResponse<CreatorProfileVO> getCreatorProfile(@PathVariable("userId") Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创作者ID不能为空");
        }
        User user = userService.getById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())
                || Integer.valueOf(2).equals(user.getAccountStatus())) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        CreatorProfileVO profile = new CreatorProfileVO();
        profile.setUserId(user.getId());
        profile.setDisplayName(StringUtils.defaultIfBlank(user.getNickname(), user.getRealName()));
        profile.setAvatar(resolvePublicAvatar(user.getAvatar()));
        profile.setGrade(user.getGrade());
        profile.setBio(user.getBio());
        profile.setEduVerified(user.getEduVerified() != null ? user.getEduVerified() : 0);
        int score = user.getCreditScore() != null ? user.getCreditScore() : 100;
        profile.setCreditScore(score);
        profile.setCreditTier(resolveCreditTier(score));
        profile.setPublishPermission(user.getPublishPermission() != null ? user.getPublishPermission() : 0);
        profile.setSellerDeposit(user.getSellerDeposit() != null ? user.getSellerDeposit() : BigDecimal.ZERO);

        if (user.getProfessionalId() != null && professionalService != null) {
            Professional prof = professionalService.getById(user.getProfessionalId());
            if (prof != null) {
                profile.setProfessionalName(prof.getProfessionalName());
            }
        }
        if (user.getCampusId() != null && campusService != null) {
            Campus campus = campusService.getById(user.getCampusId());
            if (campus != null) {
                profile.setCampusName(campus.getCampusName());
            }
        }

        // 统计已完结担保订单数
        long completedOrders = serviceOrderService.count(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getSellerId, userId)
                .eq(ServiceOrder::getIsDelete, 0)
                .eq(ServiceOrder::getOrderStatus, OrderConstant.ORDER_COMPLETED));
        profile.setCompletedOrderCount(completedOrders);

        // 查询在架公开技能（过滤悬赏定向与拼班内部快照单，保持橱窗整洁）
        List<Service> onlineServices = serviceService.list(new LambdaQueryWrapper<Service>()
                .eq(Service::getSellerId, userId)
                .eq(Service::getStatus, ServiceConstant.STATUS_ONLINE)
                .eq(Service::getIsDelete, 0)
                .notLike(Service::getServiceTitle, "【悬赏定向单】")
                .notLike(Service::getServiceTitle, "【拼班团课】")
                .orderByDesc(Service::getCurrentPurchaseCount)
                .orderByDesc(Service::getCreateTime));
        profile.setActiveServiceCount((long) onlineServices.size());

        Map<Long, String> tagMap = new HashMap<>();
        if (serviceTagService != null) {
            for (ServiceTag t : serviceTagService.list()) {
                tagMap.put(t.getId(), t.getTagName());
            }
        }

        List<Long> serviceIds = onlineServices.stream().map(Service::getId).collect(Collectors.toList());
        Map<Long, String> coverMap = (!serviceIds.isEmpty() && serviceImageService != null)
                ? serviceImageService.getCoverImageMap(serviceIds) : Collections.emptyMap();

        List<CreatorProfileVO.CreatorServiceItem> svcItems = new ArrayList<>(onlineServices.size());
        for (Service s : onlineServices) {
            CreatorProfileVO.CreatorServiceItem item = new CreatorProfileVO.CreatorServiceItem();
            item.setId(s.getId());
            item.setServiceTitle(s.getServiceTitle());
            item.setServiceDescription(s.getServiceDescription());
            item.setCoverImage(coverMap.get(s.getId()));
            item.setTagName(s.getTagId() != null ? tagMap.get(s.getTagId()) : null);
            item.setTradeType(s.getTradeType());
            item.setPrice(s.getPrice());
            item.setCurrentPurchaseCount(s.getCurrentPurchaseCount() != null ? s.getCurrentPurchaseCount() : 0);
            item.setAverageRating(s.getAverageRating());
            svcItems.add(item);
        }
        profile.setServices(svcItems);

        // 查询创作者收到的真实订单评价
        List<ServiceReview> reviews = serviceReviewService.list(new LambdaQueryWrapper<ServiceReview>()
                .eq(ServiceReview::getSellerId, userId)
                .orderByDesc(ServiceReview::getCreateTime)
                .last("LIMIT 20"));
        long totalReviewCount = serviceReviewService.count(new LambdaQueryWrapper<ServiceReview>()
                .eq(ServiceReview::getSellerId, userId));
        profile.setReviewCount(totalReviewCount);

        if (!reviews.isEmpty()) {
            Double overallAvg = serviceReviewService.getSellerAverageRating(userId);
            double avg = overallAvg != null ? overallAvg : reviews.stream()
                    .filter(r -> r.getRating() != null)
                    .mapToInt(ServiceReview::getRating)
                    .average()
                    .orElse(0.0);
            profile.setAverageRating(avg > 0 ? BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP) : null);

            Set<Long> buyerIds = reviews.stream()
                    .filter(r -> !Integer.valueOf(1).equals(r.getIsAnonymous()) && r.getBuyerId() != null)
                    .map(ServiceReview::getBuyerId)
                    .collect(Collectors.toSet());
            Map<Long, User> buyerMap = buyerIds.isEmpty() ? Collections.emptyMap()
                    : userService.listByIds(buyerIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

            Set<Long> reviewedSvcIds = reviews.stream().map(ServiceReview::getServiceId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, String> svcTitleMap = reviewedSvcIds.isEmpty() ? Collections.emptyMap()
                    : serviceService.listByIds(reviewedSvcIds).stream()
                    .collect(Collectors.toMap(Service::getId, Service::getServiceTitle, (a, b) -> a));

            List<CreatorProfileVO.CreatorReviewItem> reviewItems = new ArrayList<>(reviews.size());
            for (ServiceReview r : reviews) {
                CreatorProfileVO.CreatorReviewItem ri = new CreatorProfileVO.CreatorReviewItem();
                ri.setId(r.getId());
                ri.setServiceId(r.getServiceId());
                ri.setServiceTitle(svcTitleMap.getOrDefault(r.getServiceId(), "校内担保服务"));
                if (Integer.valueOf(1).equals(r.getIsAnonymous())) {
                    ri.setBuyerDisplayName("匿名同学");
                } else {
                    User b = buyerMap.get(r.getBuyerId());
                    ri.setBuyerDisplayName(b != null ? StringUtils.defaultIfBlank(b.getNickname(), "校内同学") : "校内同学");
                }
                ri.setRating(r.getRating());
                ri.setContent(r.getContent());
                ri.setCreateTime(r.getCreateTime());
                reviewItems.add(ri);
            }
            profile.setRecentReviews(reviewItems);
        } else {
            profile.setRecentReviews(Collections.emptyList());
        }

        return ResultUtils.success(profile);
    }

    private String resolvePublicAvatar(String avatar) {
        if (StringUtils.isBlank(avatar)) {
            return avatar;
        }
        return fileUrlResolver != null ? fileUrlResolver.toPublicUrl(avatar) : avatar;
    }

    private String resolveCreditTier(int score) {
        if (score >= 110) {
            return "极好 · 校园标杆创作者";
        }
        if (score >= 90) {
            return "优秀 · 稳定履约";
        }
        if (score >= 60) {
            return "正常 · 基础担保";
        }
        return "观察期";
    }
}
