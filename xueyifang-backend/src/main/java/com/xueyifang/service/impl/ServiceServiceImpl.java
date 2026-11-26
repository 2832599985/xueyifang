package com.xueyifang.service.impl;

import com.xueyifang.domain.po.Service;
import com.xueyifang.mapper.ServiceMapper;
import com.xueyifang.service.IServiceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.CacheConstant;
import com.xueyifang.domain.dto.ServicePublishRequest;
import com.xueyifang.domain.dto.ServiceUpdateRequest;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.ServiceImage;
import com.xueyifang.domain.po.ServiceOrder;
import com.xueyifang.domain.po.ServiceReview;
import com.xueyifang.domain.po.ServiceTag;
import com.xueyifang.domain.po.TradeLocation;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.ServiceDetailVO;
import com.xueyifang.domain.vo.ServiceListItemVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.ServiceReviewMapper;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.IServiceFavoriteService;
import com.xueyifang.service.IServiceImageService;
import com.xueyifang.service.IServiceReviewService;
import com.xueyifang.service.IServiceTagService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.ITradeLocationService;
import com.xueyifang.service.IUserService;
import com.xueyifang.utils.RedisUtils;
import com.xueyifang.utils.UserContext;
import com.xueyifang.config.FileStorageProperties;
import com.xueyifang.constant.OrderConstant;
import com.xueyifang.constant.ServiceConstant;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.mapper.ServiceOrderMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务表 服务实现类
 * </p>
 *
 * @author luoyi
 * @since 2025-11-17
 */

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

    /**
     * ServiceReviewServiceImpl 已改为只依赖 Mapper，不再反向依赖本类，因此无需 @Lazy
     */
    @Resource
    private IServiceReviewService serviceReviewService;

    @Resource
    private ServiceReviewMapper serviceReviewMapper;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private com.xueyifang.utils.FileUrlResolver fileUrlResolver;

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

        if (StringUtils.isBlank(request.getServiceTitle()) || StringUtils.isBlank(request.getServiceDescription())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题或描述不能为空");
        }
        if (request.getTradeType() == null || (request.getTradeType() != ServiceConstant.TRADE_TYPE_OFFLINE && request.getTradeType() != ServiceConstant.TRADE_TYPE_ONLINE)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "交易类型不合法");
        }
        if (request.getPrice() == null || request.getPrice().signum() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "价格必须大于0");
        }

        Service entity = new Service();
        entity.setServiceTitle(request.getServiceTitle());
        entity.setServiceDescription(request.getServiceDescription());
        entity.setSellerId(userId);
        entity.setTagId(request.getTagId());
        entity.setTradeType(request.getTradeType());
        entity.setTradeLocationId(request.getTradeLocationId());
        entity.setPrice(request.getPrice());
        entity.setMaxPurchases(request.getMaxPurchases() == null ? -1 : request.getMaxPurchases());
        entity.setCurrentPurchaseCount(0);
        entity.setIsPhysical(request.getIsPhysical() == null ? ServiceConstant.NOT_PHYSICAL : request.getIsPhysical());

        // 根据REVIEW_MODE配置设置初始状态
        int reviewMode = sysConfigService.getIntValue(SysConfigConstant.REVIEW_MODE, 1);
        if (reviewMode == 2) {
            // 免审核模式：直接上架
            entity.setStatus(ServiceConstant.STATUS_ONLINE);
            entity.setReviewStatus(ServiceConstant.REVIEW_APPROVED);
        } else {
            // 需审核模式：待审核
            entity.setStatus(ServiceConstant.STATUS_PENDING);
            entity.setReviewStatus(ServiceConstant.REVIEW_PENDING);
        }

        boolean saved = this.save(entity);
        if (!saved) {
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
                .eq(Service::getReviewStatus, ServiceConstant.REVIEW_APPROVED);

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
        wrapper.eq(Service::getIsDelete, 0)
                .eq(Service::getSellerId, userId);

        if (status != null) {
            wrapper.eq(Service::getStatus, status);
        }

        wrapper.orderByDesc(Service::getCreateTime);

        Page<Service> page = new Page<>(pageNo, pageSize);
        IPage<Service> servicePage = this.page(page, wrapper);

        return toServiceListItemVOPage(servicePage);
    }

    /**
     * 转换为服务列表VO（已优化N+1查询）
     * 优化前：1 + 3N 次数据库查询（N为记录数）
     * 优化后：固定 5 次数据库查询（批量查询标签、卖家、封面图、销量）
     */
    private IPage<ServiceListItemVO> toServiceListItemVOPage(IPage<Service> servicePage) {
        List<Service> records = servicePage.getRecords();
        if (records.isEmpty()) {
            Page<ServiceListItemVO> voPage = new Page<>(servicePage.getCurrent(), servicePage.getSize(), servicePage.getTotal());
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        List<Long> serviceIds = records.stream().map(Service::getId).collect(Collectors.toList());

        // ========== 批量查询1：封面图片 ==========
        Map<Long, String> coverMap = batchQueryCoverImages(serviceIds);

        // ========== 批量查询2：服务标签 ==========
        Set<Long> tagIds = records.stream()
                .map(Service::getTagId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, ServiceTag> tagMap = new HashMap<>();
        if (!tagIds.isEmpty()) {
            List<ServiceTag> tags = serviceTagService.listByIds(tagIds);
            tagMap = tags.stream().collect(Collectors.toMap(ServiceTag::getId, t -> t, (a, b) -> a));
        }

        // ========== 批量查询3：卖家信息 ==========
        Set<Long> sellerIds = records.stream()
                .map(Service::getSellerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, User> sellerMap = new HashMap<>();
        if (!sellerIds.isEmpty()) {
            List<User> sellers = userService.listByIds(sellerIds);
            sellerMap = sellers.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        }

        // ========== 批量查询4：销量统计（GROUP BY） ==========
        Map<Long, Integer> salesCountMap = batchQuerySalesCount(serviceIds);

        // ========== 组装VO（无数据库查询） ==========
        final Map<Long, ServiceTag> finalTagMap = tagMap;
        final Map<Long, User> finalSellerMap = sellerMap;

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

            // 从预加载的Map获取标签名称（无DB查询）
            if (s.getTagId() != null) {
                ServiceTag tag = finalTagMap.get(s.getTagId());
                if (tag != null) {
                    vo.setTagName(tag.getTagName());
                }
            }

            // 从预加载的Map获取卖家信息（无DB查询）
            if (s.getSellerId() != null) {
                User seller = finalSellerMap.get(s.getSellerId());
                if (seller != null) {
                    vo.setSellerId(s.getSellerId());
                    vo.setSellerName(seller.getNickname());
                    vo.setSellerAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(seller.getAvatar()) : seller.getAvatar());
                }
            }

            // 从预加载的Map获取销量（无DB查询）
            vo.setSalesCount(salesCountMap.getOrDefault(s.getId(), 0));

            // 评分功能：从服务表直接获取（BigDecimal 转 Double）
            if (s.getAverageRating() != null) {
                vo.setAverageRating(s.getAverageRating().doubleValue());
            }

            // 填充"我的服务"列表专用字段
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

    /**
     * 批量查询封面图片
     */
    private Map<Long, String> batchQueryCoverImages(List<Long> serviceIds) {
        return serviceImageService.getCoverImageMap(serviceIds);
    }

    /**
     * 批量统计销量（使用GROUP BY，避免N+1查询）
     */
    private Map<Long, Integer> batchQuerySalesCount(List<Long> serviceIds) {
        Map<Long, Integer> salesCountMap = new HashMap<>();
        if (serviceIds.isEmpty()) {
            return salesCountMap;
        }
        // 使用自定义 SQL 一次性批量统计所有服务的销量
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

        boolean canView;
        if ((s.getStatus() != null && s.getStatus() == ServiceConstant.STATUS_ONLINE) && (s.getReviewStatus() != null && s.getReviewStatus() == ServiceConstant.REVIEW_APPROVED)) {
            canView = true;
        } else {
            Long currentUserId = UserContext.getCurrentUserId();
            boolean isOwner = currentUserId != null && currentUserId.equals(s.getSellerId());
            boolean isAdmin = UserContext.isAdmin();
            canView = isOwner || isAdmin;
            if (!canView) {
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

        // 尝试从缓存获取服务详情
        String cacheKey = CacheConstant.SERVICE_DETAIL_PREFIX + serviceId;
        ServiceDetailVO vo = redisUtils.getJson(cacheKey, ServiceDetailVO.class);

        if (vo == null) {
            // 缓存未命中，构建完整VO
            log.debug("服务详情缓存未命中: serviceId={}", serviceId);
            vo = buildServiceDetailVO(s, serviceId);

            // 写入缓存（不含isCollected，因为它是用户相关的）
            redisUtils.setJson(cacheKey, vo, CacheConstant.SERVICE_DETAIL_TTL);
            log.debug("服务详情写入缓存: serviceId={}", serviceId);
        } else {
            log.debug("服务详情缓存命中: serviceId={}", serviceId);
        }

        // 动态数据：实时查询isCollected
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId != null) {
            boolean isCollected = serviceFavoriteService.isCollected(currentUserId, serviceId);
            vo.setIsCollected(isCollected);
        } else {
            vo.setIsCollected(false);
        }

        // 更新浏览量（不走缓存）
        this.update(new UpdateWrapper<Service>().eq("id", serviceId).setSql("view_count = view_count + 1"));

        return vo;
    }

    /**
     * 构建服务详情VO
     */
    private ServiceDetailVO buildServiceDetailVO(Service s, Long serviceId) {
        LambdaQueryWrapper<ServiceImage> imgWrapper = new LambdaQueryWrapper<>();
        imgWrapper.eq(ServiceImage::getServiceId, serviceId)
                .eq(ServiceImage::getIsDelete, 0)
                .orderByAsc(ServiceImage::getSortOrder)
                .orderByAsc(ServiceImage::getId);
        List<ServiceImage> imgs = serviceImageService.list(imgWrapper);

        ServiceDetailVO vo = new ServiceDetailVO();
        vo.setId(s.getId());
        vo.setServiceTitle(s.getServiceTitle());
        vo.setServiceDescription(s.getServiceDescription());
        vo.setSellerId(s.getSellerId());
        vo.setTagId(s.getTagId());
        vo.setTradeType(s.getTradeType());
        vo.setTradeLocationId(s.getTradeLocationId());
        vo.setPrice(s.getPrice());
        vo.setMaxPurchases(s.getMaxPurchases());
        vo.setCurrentPurchaseCount(s.getCurrentPurchaseCount());
        vo.setIsPhysical(s.getIsPhysical());
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

        // 填充卖家信息与校友信用档案
        if (s.getSellerId() != null) {
            User seller = userService.getById(s.getSellerId());
            if (seller != null) {
                vo.setSellerName(seller.getNickname());
                vo.setSellerAvatar(fileUrlResolver != null ? fileUrlResolver.toPublicUrl(seller.getAvatar()) : seller.getAvatar());
                vo.setSellerRating(serviceReviewService.getSellerAverageRating(s.getSellerId()));
                vo.setSellerGrade(seller.getGrade());
                vo.setSellerBio(seller.getBio());

                Map<String, Object> sellerCompletedStats = serviceOrderMapper.countAndSumByStatus(
                        OrderConstant.ORDER_COMPLETED, s.getSellerId());
                if (sellerCompletedStats != null && sellerCompletedStats.get("order_count") instanceof Number) {
                    vo.setSellerCompletedOrders(((Number) sellerCompletedStats.get("order_count")).intValue());
                } else {
                    vo.setSellerCompletedOrders(0);
                }

                // 填充卖家专业
                if (seller.getProfessionalId() != null) {
                    Professional professional = professionalService.getById(seller.getProfessionalId());
                    if (professional != null) {
                        vo.setSellerProfessional(professional.getProfessionalName());
                    }
                }
            }
        }

        // 填充标签名称
        if (s.getTagId() != null) {
            ServiceTag tag = serviceTagService.getById(s.getTagId());
            if (tag != null) {
                vo.setTagName(tag.getTagName());
            }
        }

        // 填充交易地点名称
        if (s.getTradeLocationId() != null) {
            TradeLocation location = tradeLocationService.getById(s.getTradeLocationId());
            if (location != null) {
                vo.setTradeLocationName(location.getLocationName());
            }
        }

        // 统计评价数量
        LambdaQueryWrapper<ServiceReview> reviewCountWrapper = new LambdaQueryWrapper<>();
        reviewCountWrapper.eq(ServiceReview::getServiceId, serviceId)
                .eq(ServiceReview::getIsDelete, 0);
        Long reviewCount = serviceReviewMapper.selectCount(reviewCountWrapper);
        vo.setReviewCount(reviewCount.intValue());

        // 统计销量
        LambdaQueryWrapper<ServiceOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(ServiceOrder::getServiceId, serviceId)
                .eq(ServiceOrder::getOrderStatus, OrderConstant.ORDER_COMPLETED)
                .eq(ServiceOrder::getIsDelete, 0);
        Long salesCount = serviceOrderMapper.selectCount(orderWrapper);
        vo.setSalesCount(salesCount.intValue());

        return vo;
    }

    /**
     * 删除服务详情缓存
     */
    private void deleteServiceDetailCache(Long serviceId) {
        String cacheKey = CacheConstant.SERVICE_DETAIL_PREFIX + serviceId;
        redisUtils.delete(cacheKey);
        log.info("服务详情缓存已删除: serviceId={}", serviceId);
    }

    private String toRelativePath(String fileUrl) {
        if (fileUrlResolver != null) {
            return fileUrlResolver.toRelativeKey(fileUrl);
        }
        return fileUrl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateService(Long userId, Long serviceId, ServiceUpdateRequest request) {
        // 1. 参数校验
        if (userId == null || serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 查询服务
        Service service = this.getById(serviceId);
        if (service == null || (service.getIsDelete() != null && service.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }

        // 3. 权限校验：只有发布者或管理员可编辑
        boolean isOwner = userId.equals(service.getSellerId());
        boolean isAdmin = UserContext.isAdmin();
        if (!isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_OWNER);
        }

        // 4. 状态校验：上架中的服务不能编辑（需先下架）
        Integer status = service.getStatus();
        if (status != null && status == ServiceConstant.STATUS_ONLINE) {
            throw new BusinessException(ErrorCode.SERVICE_CANNOT_EDIT, "上架中的服务请先下架再编辑");
        }

        // 5. 构建更新对象（仅更新非null字段）
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

        // 6. 更新服务基本信息与状态流转
        boolean imagesUpdated = request.getImages() != null;
        if (hasUpdateFields || imagesUpdated) {
            int reviewMode = sysConfigService.getIntValue(SysConfigConstant.REVIEW_MODE, 1);
            if (status != null && status == ServiceConstant.STATUS_REJECTED) {
                // 已驳回的服务修改后自动重新提交审核（或免审直接上架）
                if (reviewMode == 2) {
                    updateEntity.setStatus(ServiceConstant.STATUS_ONLINE);
                    updateEntity.setReviewStatus(ServiceConstant.REVIEW_APPROVED);
                } else {
                    updateEntity.setStatus(ServiceConstant.STATUS_PENDING);
                    updateEntity.setReviewStatus(ServiceConstant.REVIEW_PENDING);
                }
                updateEntity.setReviewReason(null);
            } else if (status != null && status == ServiceConstant.STATUS_OFFLINE) {
                // 已下架服务若修改了内容，下次重新上架需重新审核
                updateEntity.setReviewStatus(ServiceConstant.REVIEW_PENDING);
            }
            this.updateById(updateEntity);
        }

        // 7. 处理图片更新（如果有传图片列表）
        if (request.getImages() != null) {
            // 7.1 逻辑删除旧图片
            UpdateWrapper<ServiceImage> deleteWrapper = new UpdateWrapper<>();
            deleteWrapper.eq("service_id", serviceId)
                    .eq("is_delete", 0)
                    .set("is_delete", 1);
            serviceImageService.update(deleteWrapper);

            // 7.2 插入新图片
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

        // 8. 删除服务详情缓存
        deleteServiceDetailCache(serviceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineService(Long userId, Long serviceId) {
        // 1. 参数校验
        if (userId == null || serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 查询服务
        Service service = this.getById(serviceId);
        if (service == null || (service.getIsDelete() != null && service.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }

        // 3. 权限校验：只有发布者或管理员可操作
        boolean isOwner = userId.equals(service.getSellerId());
        boolean isAdmin = UserContext.isAdmin();
        if (!isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_OWNER);
        }

        // 4. 状态校验：上架中或待审核状态均可下架/撤回审核
        if (service.getStatus() == null
                || (service.getStatus() != ServiceConstant.STATUS_ONLINE
                    && service.getStatus() != ServiceConstant.STATUS_PENDING)) {
            throw new BusinessException(ErrorCode.SERVICE_CANNOT_OFFLINE, "只有上架中或待审核的服务可以下架/撤回");
        }

        // 5. 更新状态为下架
        Service updateEntity = new Service();
        updateEntity.setId(serviceId);
        updateEntity.setStatus(ServiceConstant.STATUS_OFFLINE);

        boolean updated = this.updateById(updateEntity);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "下架失败");
        }

        // 6. 删除服务详情缓存
        deleteServiceDetailCache(serviceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onlineService(Long userId, Long serviceId) {
        // 1. 参数校验
        if (userId == null || serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 查询服务
        Service service = this.getById(serviceId);
        if (service == null || (service.getIsDelete() != null && service.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }

        // 3. 权限校验：只有发布者或管理员可操作
        boolean isOwner = userId.equals(service.getSellerId());
        boolean isAdmin = UserContext.isAdmin();
        if (!isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_OWNER);
        }

        // 4. 状态校验：只有下架或驳回状态可上架/提交审核
        Integer status = service.getStatus();
        if (status == null || (status != ServiceConstant.STATUS_OFFLINE && status != ServiceConstant.STATUS_REJECTED)) {
            throw new BusinessException(ErrorCode.SERVICE_CANNOT_ONLINE, "只有下架或驳回状态的服务可以提交上架");
        }

        // 5. 读取REVIEW_MODE配置
        int reviewMode = sysConfigService.getIntValue(SysConfigConstant.REVIEW_MODE, 1);

        // 6. 根据配置及历史审核状态设置状态（已通过审核且下架期间未修改内容的服务可直接恢复上架）
        Service updateEntity = new Service();
        updateEntity.setId(serviceId);

        boolean alreadyApprovedUnmodified = status == ServiceConstant.STATUS_OFFLINE
                && Integer.valueOf(ServiceConstant.REVIEW_APPROVED).equals(service.getReviewStatus());

        if (reviewMode == 2 || alreadyApprovedUnmodified) {
            // 免审核模式或未修改的已过审服务：直接恢复上架
            updateEntity.setStatus(ServiceConstant.STATUS_ONLINE);
            updateEntity.setReviewStatus(ServiceConstant.REVIEW_APPROVED);
        } else {
            // 需审核模式：进入待审核
            updateEntity.setStatus(ServiceConstant.STATUS_PENDING);
            updateEntity.setReviewStatus(ServiceConstant.REVIEW_PENDING);
            updateEntity.setReviewReason(null);
            updateEntity.setReviewedBy(null);
            updateEntity.setReviewTime(null);
        }

        boolean updated = this.updateById(updateEntity);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交审核失败");
        }

        // 8. 删除服务详情缓存
        deleteServiceDetailCache(serviceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteService(Long userId, Long serviceId) {
        // 1. 参数校验
        if (userId == null || serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 查询服务
        Service service = this.getById(serviceId);
        if (service == null || service.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }

        // 3. 权限检查：只有服务所有者可以删除
        if (!service.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_OWNER);
        }

        // 4. 状态检查：只有下架或已驳回的服务可以删除
        Integer status = service.getStatus();
        if (status == null || (status != ServiceConstant.STATUS_OFFLINE && status != ServiceConstant.STATUS_REJECTED)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有下架或已驳回的服务可以删除，请先下架服务");
        }

        // 5. 检查是否有进行中的订单（待支付、待发货、待收货）
        LambdaQueryWrapper<ServiceOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(ServiceOrder::getServiceId, serviceId)
                .eq(ServiceOrder::getIsDelete, 0)
                .in(ServiceOrder::getOrderStatus,
                        OrderConstant.ORDER_UNPAID,
                        OrderConstant.ORDER_PENDING_SHIP,
                        OrderConstant.ORDER_PENDING_RECEIPT);
        Long activeOrderCount = serviceOrderMapper.selectCount(orderWrapper);
        if (activeOrderCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该服务有进行中的订单，无法删除");
        }

        // 6. 逻辑删除服务
        Service updateEntity = new Service();
        updateEntity.setId(serviceId);
        updateEntity.setIsDelete(1);

        boolean updated = this.updateById(updateEntity);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败");
        }

        // 7. 删除服务详情缓存
        deleteServiceDetailCache(serviceId);

        log.info("服务已删除: serviceId={}, userId={}", serviceId, userId);
    }
}
