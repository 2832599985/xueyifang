package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.config.FileStorageProperties;
import com.xueyifang.constant.CacheConstant;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.ServiceConstant;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.ServicePublishRequest;
import com.xueyifang.domain.dto.ServiceUpdateRequest;
import com.xueyifang.domain.po.Campus;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.Service;
import com.xueyifang.domain.po.ServiceImage;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.ServiceReview;
import com.xueyifang.domain.po.ServiceTag;
import com.xueyifang.domain.po.TradeLocation;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.ServiceDetailVO;
import com.xueyifang.domain.vo.ServiceListItemVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.CampusMapper;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.mapper.ServiceOrderMapper;
import com.xueyifang.mapper.ServiceReviewMapper;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.IServiceFavoriteService;
import com.xueyifang.service.IServiceImageService;
import com.xueyifang.service.IServiceReviewService;
import com.xueyifang.service.IServiceService;
import com.xueyifang.service.IServiceTagService;
import com.xueyifang.service.IServiceTimeSlotService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.ITradeLocationService;
import com.xueyifang.service.IUserService;
import com.xueyifang.utils.FileUrlResolver;
import com.xueyifang.utils.RedisUtils;
import com.xueyifang.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@Slf4j
public class ServiceServiceImpl extends ServiceImpl<ServiceMapper, Service> implements IServiceService {

    @Resource
    private IServiceImageService serviceImageService;

    @Resource
    private IUserService userService;

    @Resource
    private IProfessionalService professionalService;

    @Resource
    private IServiceTagService serviceTagService;

    @Resource
    private FileStorageProperties properties;

    @Resource
    private IServiceFavoriteService serviceFavoriteService;

    @Resource
    private ISysConfigService sysConfigService;

    @Resource
    private ITradeLocationService tradeLocationService;

    @Resource
    private IServiceReviewService serviceReviewService;

    @Resource
    private ServiceReviewMapper serviceReviewMapper;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private FileUrlResolver fileUrlResolver;

    @Resource
    private CampusMapper campusMapper;

    @Resource
    private IServiceTimeSlotService serviceTimeSlotService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishService(Long userId, ServicePublishRequest request) {
        if (userId == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        Integer publishPermission = user.getPublishPermission();
        if (publishPermission == null || publishPermission != UserConstant.HAS_PUBLISH_PERMISSION) {
            throw new BusinessException(ErrorCode.USER_NO_PUBLISH_PERMISSION);
        }

        // 校验卖家校园履约信用分门槛
        if (user.getCreditScore() != null && sysConfigService != null) {
            int minPublishScore = sysConfigService.getIntValue(SysConfigConstant.MIN_CREDIT_SCORE_FOR_PUBLISH, 70);
            if (user.getCreditScore() < minPublishScore) {
                throw new BusinessException(ErrorCode.USER_CREDIT_TOO_LOW,
                        "当前履约信用分（" + user.getCreditScore() + "）低于发布门槛（" + minPublishScore + "），暂不可上架新服务");
            }
        }

        if (StringUtils.isBlank(request.getServiceTitle()) || StringUtils.isBlank(request.getServiceDescription())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题或描述不能为空");
        }
        if (request.getTradeType() == null
                || (request.getTradeType() != ServiceConstant.TRADE_TYPE_OFFLINE
                    && request.getTradeType() != ServiceConstant.TRADE_TYPE_ONLINE)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "交易类型不合法");
        }
        if (request.getPrice() == null || request.getPrice().signum() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "价格必须大于0");
        }

        Service entity = new Service();
        entity.setServiceTitle(request.getServiceTitle());
        entity.setServiceDescription(request.getServiceDescription());
        entity.setSellerId(userId);
        entity.setCampusId(request.getCampusId() != null ? request.getCampusId() : (user.getCampusId() != null ? user.getCampusId() : 1L));
        entity.setTagId(request.getTagId());
        entity.setTradeType(request.getTradeType());
        entity.setTradeLocationId(request.getTradeLocationId());
        entity.setPrice(request.getPrice());
        entity.setMaxPurchases(request.getMaxPurchases() == null ? -1 : request.getMaxPurchases());
        entity.setCurrentPurchaseCount(0);
        entity.setIsPhysical(request.getIsPhysical() == null ? ServiceConstant.NOT_PHYSICAL : request.getIsPhysical());
        entity.setRequireSlot(request.getRequireSlot() == null ? 0 : request.getRequireSlot());
        entity.setCancelFreeHours(request.getCancelFreeHours() == null ? 6 : request.getCancelFreeHours());
        entity.setRequireBleVerify(request.getRequireBleVerify() == null ? 0 : request.getRequireBleVerify());

        int reviewMode = sysConfigService.getIntValue(SysConfigConstant.REVIEW_MODE, 1);
        if (reviewMode == 2) {
            entity.setStatus(ServiceConstant.STATUS_ONLINE);
            entity.setReviewStatus(ServiceConstant.REVIEW_APPROVED);
        } else {
            entity.setStatus(ServiceConstant.STATUS_PENDING);
            entity.setReviewStatus(ServiceConstant.REVIEW_PENDING);
        }

        if (!this.save(entity)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发布失败");
        }

        Long serviceId = entity.getId();
        List<String> imageUrls = request.getImages();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<ServiceImage> images = new ArrayList<>();
            for (int i = 0; i < imageUrls.size(); i++) {
                String url = toRelativePath(imageUrls.get(i));
                if (StringUtils.isBlank(url)) {
                    continue;
                }
                ServiceImage img = new ServiceImage();
                img.setServiceId(serviceId);
                img.setImageUrl(url);
                img.setSortOrder(i);
                images.add(img);
            }
            if (!images.isEmpty()) {
                serviceImageService.saveBatch(images);
            }
        }
        return serviceId;
    }

    @Override
    public IPage<ServiceListItemVO> searchServices(String keyword, Long tagId, Integer tradeType, Long sellerId, String sortBy, long pageNo, long pageSize) {
        LambdaQueryWrapper<Service> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Service::getIsDelete, 0)
                .eq(Service::getStatus, ServiceConstant.STATUS_ONLINE)
                .eq(Service::getReviewStatus, ServiceConstant.REVIEW_APPROVED)
                .notLikeRight(Service::getServiceTitle, "【悬赏定向单】")
                .notLikeRight(Service::getServiceTitle, "【拼班团课】");

        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Service::getServiceTitle, keyword)
                    .or().like(Service::getServiceDescription, keyword));
        }
        if (tagId != null) {
            wrapper.eq(Service::getTagId, tagId);
        }
        if (tradeType != null) {
            wrapper.eq(Service::getTradeType, tradeType);
        }
        if (sellerId != null) {
            wrapper.eq(Service::getSellerId, sellerId);
        }

        if (StringUtils.isBlank(sortBy) || ServiceConstant.SORT_CREATE_DESC.equalsIgnoreCase(sortBy) || ServiceConstant.SORT_LATEST.equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(Service::getCreateTime);
        } else if (ServiceConstant.SORT_PRICE_ASC.equalsIgnoreCase(sortBy)) {
            wrapper.orderByAsc(Service::getPrice);
        } else if (ServiceConstant.SORT_PRICE_DESC.equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(Service::getPrice);
        } else if (ServiceConstant.SORT_VIEW_DESC.equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(Service::getViewCount);
        } else {
            wrapper.orderByDesc(Service::getCreateTime);
        }

        Page<Service> page = new Page<>(pageNo, pageSize);
        IPage<Service> servicePage = this.page(page, wrapper);
        return toServiceListItemVOPage(servicePage);
    }

    @Override
    public IPage<ServiceListItemVO> listMyServices(Long userId, Integer status, long pageNo, long pageSize) {
        LambdaQueryWrapper<Service> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Service::getIsDelete, 0).eq(Service::getSellerId, userId);
        if (status != null) {
            wrapper.eq(Service::getStatus, status);
        }
        wrapper.orderByDesc(Service::getCreateTime);
        Page<Service> page = new Page<>(pageNo, pageSize);
        return toServiceListItemVOPage(this.page(page, wrapper));
    }

    private IPage<ServiceListItemVO> toServiceListItemVOPage(IPage<Service> servicePage) {
        List<Service> records = servicePage.getRecords();
        if (records.isEmpty()) {
            Page<ServiceListItemVO> voPage = new Page<>(servicePage.getCurrent(), servicePage.getSize(), servicePage.getTotal());
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        List<Long> serviceIds = records.stream().map(Service::getId).collect(Collectors.toList());
        Map<Long, String> coverMap = serviceImageService.getCoverImageMap(serviceIds);

        Set<Long> tagIds = records.stream().map(Service::getTagId).filter(id -> id != null).collect(Collectors.toSet());
        Map<Long, ServiceTag> tagMap = tagIds.isEmpty()
                ? new HashMap<Long, ServiceTag>()
                : serviceTagService.listByIds(tagIds).stream().collect(Collectors.toMap(ServiceTag::getId, t -> t, (a, b) -> a));

        Set<Long> sellerIds = records.stream().map(Service::getSellerId).filter(id -> id != null).collect(Collectors.toSet());
        Map<Long, User> sellerMap = sellerIds.isEmpty()
                ? new HashMap<Long, User>()
                : userService.listByIds(sellerIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Map<Long, Integer> salesCountMap = batchQuerySalesCount(serviceIds);

        List<ServiceListItemVO> voList = records.stream().map(s -> {
            ServiceListItemVO vo = new ServiceListItemVO();
            vo.setId(s.getId());
            vo.setServiceTitle(s.getServiceTitle());
            vo.setPrice(s.getPrice());
            vo.setTagId(s.getTagId());
            vo.setTradeType(s.getTradeType());
            vo.setCoverImage(coverMap.get(s.getId()));
            vo.setViewCount(s.getViewCount());
            vo.setCollectionCount(s.getCollectionCount());
            vo.setCreateTime(s.getCreateTime());
            if (s.getTagId() != null && tagMap.get(s.getTagId()) != null) {
                vo.setTagName(tagMap.get(s.getTagId()).getTagName());
            }
            if (s.getSellerId() != null && sellerMap.get(s.getSellerId()) != null) {
                User seller = sellerMap.get(s.getSellerId());
                vo.setSellerId(s.getSellerId());
                vo.setSellerName(seller.getNickname());
                vo.setSellerAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(seller.getAvatar()) : seller.getAvatar());
            }
            vo.setSalesCount(salesCountMap.getOrDefault(s.getId(), 0));
            if (s.getAverageRating() != null) {
                vo.setAverageRating(s.getAverageRating().doubleValue());
            }
            vo.setStatus(s.getStatus());
            vo.setReviewStatus(s.getReviewStatus());
            vo.setReviewReason(s.getReviewReason());
            vo.setMaxPurchases(s.getMaxPurchases());
            vo.setCurrentPurchaseCount(s.getCurrentPurchaseCount());
            vo.setUpdateTime(s.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());

        Page<ServiceListItemVO> voPage = new Page<>(servicePage.getCurrent(), servicePage.getSize(), servicePage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private Map<Long, Integer> batchQuerySalesCount(List<Long> serviceIds) {
        Map<Long, Integer> salesCountMap = new HashMap<>();
        if (serviceIds.isEmpty()) {
            return salesCountMap;
        }
        List<Map<String, Object>> results = serviceOrderMapper.batchCountSalesByServiceIds(
                serviceIds, OrderConstant.ORDER_COMPLETED);
        for (Map<String, Object> row : results) {
            Long serviceId = ((Number) row.get("service_id")).longValue();
            Integer count = ((Number) row.get("sales_count")).intValue();
            salesCountMap.put(serviceId, count);
        }
        return salesCountMap;
    }

    @Override
    public ServiceDetailVO getServiceDetail(Long serviceId) {
        if (serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "服务ID不能为空");
        }
        Service s = this.getById(serviceId);
        if (s == null || (s.getIsDelete() != null && s.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }

        if (!((s.getStatus() != null && s.getStatus() == ServiceConstant.STATUS_ONLINE)
                && (s.getReviewStatus() != null && s.getReviewStatus() == ServiceConstant.REVIEW_APPROVED))) {
            Long currentUserId = UserContext.getCurrentUserId();
            boolean isOwner = currentUserId != null && currentUserId.equals(s.getSellerId());
            if (!isOwner && !UserContext.isAdmin()) {
                if (s.getStatus() != null && s.getStatus() == ServiceConstant.STATUS_OFFLINE) {
                    throw new BusinessException(ErrorCode.SERVICE_OFFLINE);
                }
                if (s.getReviewStatus() != null && s.getReviewStatus() == ServiceConstant.REVIEW_PENDING) {
                    throw new BusinessException(ErrorCode.SERVICE_REVIEWING);
                }
                if (s.getReviewStatus() != null && s.getReviewStatus() == ServiceConstant.REVIEW_REJECTED) {
                    throw new BusinessException(ErrorCode.SERVICE_REJECTED, s.getReviewReason());
                }
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
            }
        }

        String cacheKey = CacheConstant.SERVICE_DETAIL_PREFIX + serviceId;
        ServiceDetailVO vo = redisUtils != null ? redisUtils.getJson(cacheKey, ServiceDetailVO.class) : null;
        if (vo == null) {
            vo = buildServiceDetailVO(s, serviceId);
            if (redisUtils != null) {
                redisUtils.setJson(cacheKey, vo, CacheConstant.SERVICE_DETAIL_TTL);
            }
        }

        // 动态加载实时可预约档期与用户收藏态
        if (serviceTimeSlotService != null) {
            vo.setAvailableSlots(serviceTimeSlotService.listAvailableSlots(serviceId));
        }
        Long currentUserId = UserContext.getCurrentUserId();
        vo.setIsCollected(currentUserId != null && serviceFavoriteService.isCollected(currentUserId, serviceId));

        this.update(new UpdateWrapper<Service>().eq("id", serviceId).setSql("view_count = view_count + 1"));
        return vo;
    }

    private ServiceDetailVO buildServiceDetailVO(Service s, Long serviceId) {
        List<ServiceImage> imgs = serviceImageService.list(new LambdaQueryWrapper<ServiceImage>()
                .eq(ServiceImage::getServiceId, serviceId)
                .eq(ServiceImage::getIsDelete, 0)
                .orderByAsc(ServiceImage::getSortOrder)
                .orderByAsc(ServiceImage::getId));

        ServiceDetailVO vo = new ServiceDetailVO();
        vo.setId(s.getId());
        vo.setServiceTitle(s.getServiceTitle());
        vo.setServiceDescription(s.getServiceDescription());
        vo.setSellerId(s.getSellerId());
        vo.setCampusId(s.getCampusId() != null ? s.getCampusId() : 1L);
        vo.setTagId(s.getTagId());
        vo.setTradeType(s.getTradeType());
        vo.setTradeLocationId(s.getTradeLocationId());
        vo.setPrice(s.getPrice());
        vo.setMaxPurchases(s.getMaxPurchases());
        vo.setCurrentPurchaseCount(s.getCurrentPurchaseCount());
        vo.setIsPhysical(s.getIsPhysical());
        vo.setRequireSlot(s.getRequireSlot() != null ? s.getRequireSlot() : 0);
        vo.setCancelFreeHours(s.getCancelFreeHours() != null ? s.getCancelFreeHours() : 6);
        vo.setRequireBleVerify(s.getRequireBleVerify() != null ? s.getRequireBleVerify() : 0);
        vo.setStatus(s.getStatus());
        vo.setReviewStatus(s.getReviewStatus());
        vo.setReviewReason(s.getReviewReason());
        vo.setViewCount(s.getViewCount());
        vo.setCollectionCount(s.getCollectionCount());
        vo.setAverageRating(s.getAverageRating());
        vo.setCreateTime(s.getCreateTime());
        vo.setUpdateTime(s.getUpdateTime());
        vo.setImages(imgs.stream()
                .map(img -> fileUrlResolver != null ? fileUrlResolver.toPublicUrl(img.getImageUrl()) : img.getImageUrl())
                .collect(Collectors.toList()));

        if (vo.getCampusId() != null && campusMapper != null) {
            Campus campus = campusMapper.selectById(vo.getCampusId());
            if (campus != null) {
                vo.setCampusName(campus.getCampusName());
            }
        }

        if (s.getSellerId() != null) {
            User seller = userService.getById(s.getSellerId());
            if (seller != null) {
                vo.setSellerName(seller.getNickname());
                vo.setSellerAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(seller.getAvatar()) : seller.getAvatar());
                vo.setSellerRating(serviceReviewService.getSellerAverageRating(s.getSellerId()));
                vo.setSellerGrade(seller.getGrade());
                vo.setSellerBio(seller.getBio());
                vo.setSellerEduVerified(seller.getEduVerified() != null ? seller.getEduVerified() : 0);
                vo.setSellerCreditScore(seller.getCreditScore() != null ? seller.getCreditScore() : 100);

                Map<String, Object> sellerCompletedStats = serviceOrderMapper.countAndSumByStatus(
                        OrderConstant.ORDER_COMPLETED, s.getSellerId());
                if (sellerCompletedStats != null && sellerCompletedStats.get("order_count") instanceof Number) {
                    vo.setSellerCompletedOrders(((Number) sellerCompletedStats.get("order_count")).intValue());
                } else {
                    vo.setSellerCompletedOrders(0);
                }

                if (seller.getProfessionalId() != null) {
                    Professional professional = professionalService.getById(seller.getProfessionalId());
                    if (professional != null) {
                        vo.setSellerProfessional(professional.getProfessionalName());
                    }
                }
            }
        }

        if (s.getTagId() != null) {
            ServiceTag tag = serviceTagService.getById(s.getTagId());
            if (tag != null) {
                vo.setTagName(tag.getTagName());
            }
        }

        if (s.getTradeLocationId() != null) {
            TradeLocation location = tradeLocationService.getById(s.getTradeLocationId());
            if (location != null) {
                vo.setTradeLocationName(location.getLocationName());
                vo.setTradeLocationLat(location.getLatitude());
                vo.setTradeLocationLng(location.getLongitude());
            }
        }

        Long reviewCount = serviceReviewMapper.selectCount(new LambdaQueryWrapper<ServiceReview>()
                .eq(ServiceReview::getServiceId, serviceId)
                .eq(ServiceReview::getIsDelete, 0));
        vo.setReviewCount(reviewCount.intValue());

        Long salesCount = serviceOrderMapper.selectCount(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getServiceId, serviceId)
                .eq(ServiceOrder::getOrderStatus, OrderConstant.ORDER_COMPLETED)
                .eq(ServiceOrder::getIsDelete, 0));
        vo.setSalesCount(salesCount.intValue());

        return vo;
    }

    private void deleteServiceDetailCache(Long serviceId) {
        if (redisUtils != null) {
            redisUtils.delete(CacheConstant.SERVICE_DETAIL_PREFIX + serviceId);
        }
    }

    private String toRelativePath(String fileUrl) {
        return fileUrlResolver != null ? fileUrlResolver.toRelativeKey(fileUrl) : fileUrl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateService(Long userId, Long serviceId, ServiceUpdateRequest request) {
        if (userId == null || serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        Service service = this.getById(serviceId);
        if (service == null || (service.getIsDelete() != null && service.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }
        if (!userId.equals(service.getSellerId()) && !UserContext.isAdmin()) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_OWNER);
        }
        Integer status = service.getStatus();
        if (status != null && status == ServiceConstant.STATUS_ONLINE) {
            throw new BusinessException(ErrorCode.SERVICE_CANNOT_EDIT, "上架中的服务请先下架再编辑");
        }

        Service updateEntity = new Service();
        updateEntity.setId(serviceId);
        boolean hasUpdateFields = false;

        if (request.getServiceTitle() != null) {
            if (StringUtils.isBlank(request.getServiceTitle())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题不能为空");
            }
            updateEntity.setServiceTitle(request.getServiceTitle());
            hasUpdateFields = true;
        }
        if (request.getServiceDescription() != null) {
            if (StringUtils.isBlank(request.getServiceDescription())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "描述不能为空");
            }
            updateEntity.setServiceDescription(request.getServiceDescription());
            hasUpdateFields = true;
        }
        if (request.getCampusId() != null) {
            updateEntity.setCampusId(request.getCampusId());
            hasUpdateFields = true;
        }
        if (request.getTagId() != null) {
            updateEntity.setTagId(request.getTagId());
            hasUpdateFields = true;
        }
        if (request.getTradeType() != null) {
            if (request.getTradeType() != ServiceConstant.TRADE_TYPE_OFFLINE
                    && request.getTradeType() != ServiceConstant.TRADE_TYPE_ONLINE) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "交易类型不合法");
            }
            updateEntity.setTradeType(request.getTradeType());
            hasUpdateFields = true;
        }
        if (request.getTradeLocationId() != null) {
            updateEntity.setTradeLocationId(request.getTradeLocationId());
            hasUpdateFields = true;
        }
        if (request.getPrice() != null) {
            if (request.getPrice().signum() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "价格必须大于0");
            }
            updateEntity.setPrice(request.getPrice());
            hasUpdateFields = true;
        }
        if (request.getMaxPurchases() != null) {
            updateEntity.setMaxPurchases(request.getMaxPurchases());
            hasUpdateFields = true;
        }
        if (request.getIsPhysical() != null) {
            updateEntity.setIsPhysical(request.getIsPhysical());
            hasUpdateFields = true;
        }
        if (request.getRequireSlot() != null) {
            updateEntity.setRequireSlot(request.getRequireSlot());
            hasUpdateFields = true;
        }
        if (request.getCancelFreeHours() != null) {
            updateEntity.setCancelFreeHours(request.getCancelFreeHours());
            hasUpdateFields = true;
        }
        if (request.getRequireBleVerify() != null) {
            updateEntity.setRequireBleVerify(request.getRequireBleVerify());
            hasUpdateFields = true;
        }

        boolean imagesUpdated = request.getImages() != null;
        if (hasUpdateFields || imagesUpdated) {
            int reviewMode = sysConfigService.getIntValue(SysConfigConstant.REVIEW_MODE, 1);
            if (status != null && status == ServiceConstant.STATUS_REJECTED) {
                if (reviewMode == 2) {
                    updateEntity.setStatus(ServiceConstant.STATUS_ONLINE);
                    updateEntity.setReviewStatus(ServiceConstant.REVIEW_APPROVED);
                } else {
                    updateEntity.setStatus(ServiceConstant.STATUS_PENDING);
                    updateEntity.setReviewStatus(ServiceConstant.REVIEW_PENDING);
                }
                updateEntity.setReviewReason(null);
            } else if (status != null && status == ServiceConstant.STATUS_OFFLINE) {
                updateEntity.setReviewStatus(ServiceConstant.REVIEW_PENDING);
            }
            this.updateById(updateEntity);
        }

        if (request.getImages() != null) {
            serviceImageService.update(new UpdateWrapper<ServiceImage>()
                    .eq("service_id", serviceId)
                    .eq("is_delete", 0)
                    .set("is_delete", 1));
            if (!request.getImages().isEmpty()) {
                List<ServiceImage> newImages = new ArrayList<>();
                for (int i = 0; i < request.getImages().size(); i++) {
                    String url = toRelativePath(request.getImages().get(i));
                    if (StringUtils.isBlank(url)) {
                        continue;
                    }
                    ServiceImage img = new ServiceImage();
                    img.setServiceId(serviceId);
                    img.setImageUrl(url);
                    img.setSortOrder(i);
                    newImages.add(img);
                }
                if (!newImages.isEmpty()) {
                    serviceImageService.saveBatch(newImages);
                }
            }
        }
        deleteServiceDetailCache(serviceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineService(Long userId, Long serviceId) {
        if (userId == null || serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        Service service = this.getById(serviceId);
        if (service == null || (service.getIsDelete() != null && service.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }
        if (!userId.equals(service.getSellerId()) && !UserContext.isAdmin()) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_OWNER);
        }
        if (service.getStatus() == null
                || (service.getStatus() != ServiceConstant.STATUS_ONLINE
                    && service.getStatus() != ServiceConstant.STATUS_PENDING)) {
            throw new BusinessException(ErrorCode.SERVICE_CANNOT_OFFLINE, "只有上架中或待审核的服务可以下架/撤回");
        }
        Service updateEntity = new Service();
        updateEntity.setId(serviceId);
        updateEntity.setStatus(ServiceConstant.STATUS_OFFLINE);
        if (!this.updateById(updateEntity)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "下架失败");
        }
        deleteServiceDetailCache(serviceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onlineService(Long userId, Long serviceId) {
        if (userId == null || serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        Service service = this.getById(serviceId);
        if (service == null || (service.getIsDelete() != null && service.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }
        if (!userId.equals(service.getSellerId()) && !UserContext.isAdmin()) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_OWNER);
        }
        // 非管理员重新上架前复核创作者发布权限与履约信用分门槛，防止权限被撤销或低信用账号绕过审核重新上架
        if (!UserContext.isAdmin()) {
            User seller = userService.getById(service.getSellerId());
            if (seller == null || (seller.getIsDelete() != null && seller.getIsDelete() == 1)) {
                throw new BusinessException(ErrorCode.USER_NOT_EXIST);
            }
            if (!Integer.valueOf(UserConstant.HAS_PUBLISH_PERMISSION).equals(seller.getPublishPermission())) {
                throw new BusinessException(ErrorCode.USER_NO_PUBLISH_PERMISSION, "您的服务发布权限未开通或已被撤销，无法重新上架");
            }
            if (seller.getCreditScore() != null && sysConfigService != null) {
                int minPublishCredit = sysConfigService.getIntValue(SysConfigConstant.MIN_CREDIT_SCORE_FOR_PUBLISH, 70);
                if (seller.getCreditScore() < minPublishCredit) {
                    throw new BusinessException(ErrorCode.USER_CREDIT_TOO_LOW,
                            "您的履约信用分为 " + seller.getCreditScore() + "，低于服务上架门槛 " + minPublishCredit);
                }
            }
        }
        Integer status = service.getStatus();
        if (status == null || (status != ServiceConstant.STATUS_OFFLINE && status != ServiceConstant.STATUS_REJECTED)) {
            throw new BusinessException(ErrorCode.SERVICE_CANNOT_ONLINE, "只有下架或驳回状态的服务可以提交上架");
        }
        int reviewMode = sysConfigService.getIntValue(SysConfigConstant.REVIEW_MODE, 1);
        Service updateEntity = new Service();
        updateEntity.setId(serviceId);
        boolean alreadyApprovedUnmodified = status == ServiceConstant.STATUS_OFFLINE
                && Integer.valueOf(ServiceConstant.REVIEW_APPROVED).equals(service.getReviewStatus());
        if (reviewMode == 2 || alreadyApprovedUnmodified) {
            updateEntity.setStatus(ServiceConstant.STATUS_ONLINE);
            updateEntity.setReviewStatus(ServiceConstant.REVIEW_APPROVED);
        } else {
            updateEntity.setStatus(ServiceConstant.STATUS_PENDING);
            updateEntity.setReviewStatus(ServiceConstant.REVIEW_PENDING);
            updateEntity.setReviewReason(null);
            updateEntity.setReviewedBy(null);
            updateEntity.setReviewTime(null);
        }
        if (!this.updateById(updateEntity)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交审核失败");
        }
        deleteServiceDetailCache(serviceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteService(Long userId, Long serviceId) {
        if (userId == null || serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        Service service = this.getById(serviceId);
        if (service == null || service.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }
        if (!service.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_OWNER);
        }
        Integer status = service.getStatus();
        if (status == null || (status != ServiceConstant.STATUS_OFFLINE && status != ServiceConstant.STATUS_REJECTED)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有下架或已驳回的服务可以删除，请先下架服务");
        }
        Long activeOrderCount = serviceOrderMapper.selectCount(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getServiceId, serviceId)
                .eq(ServiceOrder::getIsDelete, 0)
                .in(ServiceOrder::getOrderStatus,
                        OrderConstant.ORDER_UNPAID,
                        OrderConstant.ORDER_PENDING_SHIP,
                        OrderConstant.ORDER_PENDING_RECEIPT));
        if (activeOrderCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该服务有进行中的订单，无法删除");
        }
        Service updateEntity = new Service();
        updateEntity.setId(serviceId);
        updateEntity.setIsDelete(1);
        if (!this.updateById(updateEntity)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败");
        }
        deleteServiceDetailCache(serviceId);
    }
}
