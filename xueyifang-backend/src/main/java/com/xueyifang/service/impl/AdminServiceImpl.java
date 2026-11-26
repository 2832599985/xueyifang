package com.xueyifang.service.impl;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.*;
import com.xueyifang.domain.dto.admin.AdminUserStatusRequest;
import com.xueyifang.domain.po.*;
import com.xueyifang.domain.vo.admin.AdminStatisticsVO;
import com.xueyifang.domain.vo.admin.AdminTrendVO;
import com.xueyifang.domain.vo.admin.AdminUserVO;
import com.xueyifang.domain.vo.admin.PendingServiceVO;
import com.xueyifang.domain.vo.admin.PendingUserVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.*;
import com.xueyifang.service.IAdminService;
import com.xueyifang.service.INotificationService;
import com.xueyifang.service.IServiceImageService;
import com.xueyifang.utils.RedisUtils;
import com.xueyifang.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员服务实现类
 *
 * @author xueyifang
 */
@Service
@Slf4j
public class AdminServiceImpl implements IAdminService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private ServiceMapper serviceMapper;

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Resource
    private ServiceDisputeMapper serviceDisputeMapper;

    @Resource
    private ProfessionalMapper professionalMapper;

    @Resource
    private ServiceTagMapper serviceTagMapper;

    @Resource
    private IServiceImageService serviceImageService;

    @Resource
    private INotificationService notificationService;

    @Resource
    private RedisUtils redisUtils;

    @Value("${password.salt:xueyifang}")
    private String passwordSalt;

    /**
     * 趋势统计的天数
     */
    private static final int TREND_DAYS = 7;

    @Override
    public Page<PendingUserVO> getPendingUsers(Integer pageNum, Integer pageSize) {
        // 查询待审核用户
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPermissionReviewStatus, UserConstant.PERMISSION_PENDING)
                .eq(User::getIsDelete, 0)
                .orderByAsc(User::getCreateTime);

        Page<User> userPage = userMapper.selectPage(page, wrapper);

        // 获取所有专业ID并批量查询
        Set<Long> professionalIds = userPage.getRecords().stream()
                .map(User::getProfessionalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> professionalNameMap = new HashMap<>();
        if (!professionalIds.isEmpty()) {
            LambdaQueryWrapper<Professional> profWrapper = new LambdaQueryWrapper<>();
            profWrapper.in(Professional::getId, professionalIds);
            for (Professional p : professionalMapper.selectList(profWrapper)) {
                professionalNameMap.put(p.getId(), p.getProfessionalName());
            }
        }

        // 转换为VO
        Map<Long, String> finalProfessionalNameMap = professionalNameMap;
        List<PendingUserVO> voList = userPage.getRecords().stream().map(user -> {
            PendingUserVO vo = new PendingUserVO();
            vo.setUserId(user.getId());
            vo.setStudentId(user.getStudentId());
            vo.setRealName(user.getRealName());
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
            vo.setGrade(user.getGrade());
            vo.setProfessionalName(finalProfessionalNameMap.get(user.getProfessionalId()));
            vo.setPermissionReviewStatus(user.getPermissionReviewStatus());
            vo.setApplyReason(user.getPermissionApplyReason());
            vo.setCreateTime(user.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        Page<PendingUserVO> voPage = new Page<>(pageNum, pageSize, userPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewUserPermission(Long userId, Boolean approved, String reason) {
        if (userId == null || approved == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        // 检查是否已审核
        if (!Integer.valueOf(UserConstant.PERMISSION_PENDING).equals(user.getPermissionReviewStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该用户权限已审核，无需重复操作");
        }

        // 更新用户权限状态
        User updateUser = new User();
        updateUser.setId(userId);
        if (approved) {
            updateUser.setPublishPermission(UserConstant.HAS_PUBLISH_PERMISSION);
            updateUser.setPermissionReviewStatus(UserConstant.PERMISSION_APPROVED);
        } else {
            updateUser.setPublishPermission(UserConstant.NO_PUBLISH_PERMISSION);
            updateUser.setPermissionReviewStatus(UserConstant.PERMISSION_REJECTED);
        }

        int updated = userMapper.updateById(updateUser);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新用户权限失败");
        }
        if (redisUtils != null) {
            redisUtils.deleteUserCache(userId);
        }

        // 记录管理员操作日志
        Long adminId = UserContext.getCurrentUserId();
        log.info("[管理员操作] 权限审核 - adminId={}, userId={}, userName={}, approved={}, reason={}",
                adminId, userId, user.getRealName(), approved, reason);

        // 发送通知
        notificationService.sendPermissionNotification(userId, approved, reason);
    }

    @Override
    public Page<PendingServiceVO> getPendingServices(Integer pageNum, Integer pageSize) {
        // 查询待审核服务（仅处于待审核状态的服务）
        Page<com.xueyifang.domain.po.Service> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<com.xueyifang.domain.po.Service> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.xueyifang.domain.po.Service::getReviewStatus, ServiceConstant.REVIEW_PENDING)
                .eq(com.xueyifang.domain.po.Service::getStatus, ServiceConstant.STATUS_PENDING)
                .eq(com.xueyifang.domain.po.Service::getIsDelete, 0)
                .orderByAsc(com.xueyifang.domain.po.Service::getCreateTime);

        Page<com.xueyifang.domain.po.Service> servicePage = serviceMapper.selectPage(page, wrapper);

        // 获取卖家ID和标签ID并批量查询
        Set<Long> sellerIds = servicePage.getRecords().stream()
                .map(com.xueyifang.domain.po.Service::getSellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> tagIds = servicePage.getRecords().stream()
                .map(com.xueyifang.domain.po.Service::getTagId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 注意：Collectors.toMap 遇到 null value 会抛 NPE，姓名/名称字段可能为空，因此用循环构建
        Map<Long, String> sellerNameMap = new HashMap<>();
        if (!sellerIds.isEmpty()) {
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.in(User::getId, sellerIds);
            for (User u : userMapper.selectList(userWrapper)) {
                sellerNameMap.put(u.getId(), u.getRealName());
            }
        }

        Map<Long, String> tagNameMap = new HashMap<>();
        if (!tagIds.isEmpty()) {
            LambdaQueryWrapper<ServiceTag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.in(ServiceTag::getId, tagIds);
            for (ServiceTag tag : serviceTagMapper.selectList(tagWrapper)) {
                tagNameMap.put(tag.getId(), tag.getTagName());
            }
        }

        // 获取封面图（每个服务取sortOrder最小的图片）
        Set<Long> serviceIds = servicePage.getRecords().stream()
                .map(com.xueyifang.domain.po.Service::getId)
                .collect(Collectors.toSet());

        Map<Long, String> coverImageMap = serviceImageService.getCoverImageMap(serviceIds);

        // 转换为VO
        Map<Long, String> finalSellerNameMap = sellerNameMap;
        Map<Long, String> finalTagNameMap = tagNameMap;
        Map<Long, String> finalCoverImageMap = coverImageMap;
        List<PendingServiceVO> voList = servicePage.getRecords().stream().map(service -> {
            PendingServiceVO vo = new PendingServiceVO();
            vo.setServiceId(service.getId());
            vo.setServiceTitle(service.getServiceTitle());
            vo.setCoverImage(finalCoverImageMap.get(service.getId()));
            vo.setDescription(service.getServiceDescription());
            vo.setSellerName(finalSellerNameMap.get(service.getSellerId()));
            vo.setTagName(finalTagNameMap.get(service.getTagId()));
            vo.setPrice(service.getPrice());
            vo.setStatus(service.getStatus());
            vo.setReviewStatus(service.getReviewStatus());
            vo.setReviewReason(service.getReviewReason());
            vo.setCreateTime(service.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        Page<PendingServiceVO> voPage = new Page<>(pageNum, pageSize, servicePage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewService(Long serviceId, Boolean approved, String reason) {
        if (serviceId == null || approved == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 查询服务
        com.xueyifang.domain.po.Service service = serviceMapper.selectById(serviceId);
        if (service == null || service.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }

        // 检查是否已审核（若已是通过且上架状态，管理员再次批准视为幂等成功）
        if (Boolean.TRUE.equals(approved)
                && Integer.valueOf(ServiceConstant.REVIEW_APPROVED).equals(service.getReviewStatus())
                && Integer.valueOf(ServiceConstant.STATUS_ONLINE).equals(service.getStatus())) {
            return;
        }
        if (!Integer.valueOf(ServiceConstant.REVIEW_PENDING).equals(service.getReviewStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该服务已审核，无需重复操作");
        }

        // 获取当前管理员ID
        Long adminId = UserContext.getCurrentUserId();

        // 更新服务状态
        com.xueyifang.domain.po.Service updateService = new com.xueyifang.domain.po.Service();
        updateService.setId(serviceId);
        if (approved) {
            // 批准：自动上架
            updateService.setStatus(ServiceConstant.STATUS_ONLINE);
            updateService.setReviewStatus(ServiceConstant.REVIEW_APPROVED);
        } else {
            // 拒绝：设置为驳回状态
            updateService.setStatus(ServiceConstant.STATUS_REJECTED);
            updateService.setReviewStatus(ServiceConstant.REVIEW_REJECTED);
            updateService.setReviewReason(reason);
        }
        updateService.setReviewedBy(adminId);
        updateService.setReviewTime(LocalDateTime.now());

        int updated = serviceMapper.updateById(updateService);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新服务状态失败");
        }

        // 清除服务详情缓存
        redisUtils.delete(CacheConstant.SERVICE_DETAIL_PREFIX + serviceId);

        // 记录管理员操作日志
        log.info("[管理员操作] 服务审核 - adminId={}, serviceId={}, serviceTitle={}, sellerId={}, approved={}, reason={}",
                adminId, serviceId, service.getServiceTitle(), service.getSellerId(), approved, reason);

        // 发送通知
        notificationService.sendServiceReviewNotification(service.getSellerId(), serviceId, service.getServiceTitle(), approved, reason);
    }

    @Override
    public AdminStatisticsVO getSystemStatistics() {
        // 1. 尝试从缓存获取
        AdminStatisticsVO cachedVO = redisUtils.getJson(CacheConstant.STATISTICS_ADMIN_SYSTEM, AdminStatisticsVO.class);
        if (cachedVO != null) {
            log.debug("系统统计缓存命中");
            return cachedVO;
        }

        // 2. 缓存未命中，查询计算
        log.debug("系统统计缓存未命中");
        AdminStatisticsVO vo = new AdminStatisticsVO();

        // 1. 总用户数（未删除）
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getIsDelete, 0);
        vo.setTotalUsers(userMapper.selectCount(userWrapper).intValue());

        // 2. 活跃用户数（近30天有订单的买家或卖家，SQL 去重统计）
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        vo.setActiveUsers(Convert.toInt(serviceOrderMapper.countActiveUsersSince(thirtyDaysAgo), 0));

        // 3. 总服务数（未删除）
        LambdaQueryWrapper<com.xueyifang.domain.po.Service> serviceWrapper = new LambdaQueryWrapper<>();
        serviceWrapper.eq(com.xueyifang.domain.po.Service::getIsDelete, 0);
        vo.setTotalServices(serviceMapper.selectCount(serviceWrapper).intValue());

        // 4. 总订单数（未删除）
        LambdaQueryWrapper<ServiceOrder> allOrderWrapper = new LambdaQueryWrapper<>();
        allOrderWrapper.eq(ServiceOrder::getIsDelete, 0);
        vo.setTotalOrders(serviceOrderMapper.selectCount(allOrderWrapper).intValue());

        // 5. 已完成订单数 + 7. 总交易金额（一条 SQL 聚合，不再把订单全部拉回内存累加）
        Map<String, Object> completedStats = serviceOrderMapper.countAndSumByStatus(OrderConstant.ORDER_COMPLETED, null);
        vo.setCompletedOrders(Convert.toInt(completedStats.get("order_count"), 0));
        vo.setTotalTransactionAmount(Convert.toBigDecimal(completedStats.get("total_amount"), BigDecimal.ZERO));

        // 6. 待处理纠纷数
        LambdaQueryWrapper<ServiceDispute> disputeWrapper = new LambdaQueryWrapper<>();
        disputeWrapper.eq(ServiceDispute::getDisputeStatus, DisputeConstant.STATUS_PENDING)
                .eq(ServiceDispute::getIsDelete, 0);
        vo.setPendingDisputes(serviceDisputeMapper.selectCount(disputeWrapper).intValue());

        // 8. 今日新增用户
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        LambdaQueryWrapper<User> todayUserWrapper = new LambdaQueryWrapper<>();
        todayUserWrapper.ge(User::getCreateTime, startOfDay).lt(User::getCreateTime, endOfDay)
                .eq(User::getIsDelete, 0);
        vo.setTodayNewUsers(userMapper.selectCount(todayUserWrapper).intValue());

        // 9. 今日新增服务
        LambdaQueryWrapper<com.xueyifang.domain.po.Service> todayServiceWrapper = new LambdaQueryWrapper<>();
        todayServiceWrapper.ge(com.xueyifang.domain.po.Service::getCreateTime, startOfDay)
                .lt(com.xueyifang.domain.po.Service::getCreateTime, endOfDay)
                .eq(com.xueyifang.domain.po.Service::getIsDelete, 0);
        vo.setTodayNewServices(serviceMapper.selectCount(todayServiceWrapper).intValue());

        // 10. 今日新增订单
        LambdaQueryWrapper<ServiceOrder> todayOrderWrapper = new LambdaQueryWrapper<>();
        todayOrderWrapper.ge(ServiceOrder::getCreateTime, startOfDay).lt(ServiceOrder::getCreateTime, endOfDay)
                .eq(ServiceOrder::getIsDelete, 0);
        vo.setTodayNewOrders(serviceOrderMapper.selectCount(todayOrderWrapper).intValue());

        // 写入缓存
        redisUtils.setJson(CacheConstant.STATISTICS_ADMIN_SYSTEM, vo, CacheConstant.STATISTICS_ADMIN_SYSTEM_TTL);
        log.debug("系统统计写入缓存");

        return vo;
    }

    @Override
    public AdminTrendVO getTrendStatistics() {
        AdminTrendVO cachedVO = redisUtils.getJson(CacheConstant.STATISTICS_ADMIN_TREND, AdminTrendVO.class);
        if (cachedVO != null) {
            log.debug("趋势统计缓存命中");
            return cachedVO;
        }

        log.debug("趋势统计缓存未命中");
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(TREND_DAYS - 1).atStartOfDay();

        // 两条 GROUP BY 查询替代原来的 7 天 × 3 条 = 21 次查询
        Map<String, Map<String, Object>> orderStats = indexByDate(
                serviceOrderMapper.dailyOrderStats(start, OrderConstant.ORDER_COMPLETED));
        Map<String, Map<String, Object>> serviceStats = indexByDate(serviceMapper.countDailyCreated(start));

        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MM-dd");
        List<String> dates = new ArrayList<>();
        List<Integer> orderCounts = new ArrayList<>();
        List<Integer> serviceCounts = new ArrayList<>();
        List<BigDecimal> transactionAmounts = new ArrayList<>();

        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String key = date.toString(); // yyyy-MM-dd，与 SQL DATE_FORMAT 一致
            Map<String, Object> orderRow = orderStats.getOrDefault(key, Collections.emptyMap());
            Map<String, Object> serviceRow = serviceStats.getOrDefault(key, Collections.emptyMap());

            dates.add(date.format(labelFormatter));
            orderCounts.add(Convert.toInt(orderRow.get("order_count"), 0));
            serviceCounts.add(Convert.toInt(serviceRow.get("service_count"), 0));
            transactionAmounts.add(Convert.toBigDecimal(orderRow.get("completed_amount"), BigDecimal.ZERO));
        }

        AdminTrendVO vo = new AdminTrendVO();
        vo.setDates(dates);
        vo.setOrderCounts(orderCounts);
        vo.setServiceCounts(serviceCounts);
        vo.setTransactionAmounts(transactionAmounts);

        redisUtils.setJson(CacheConstant.STATISTICS_ADMIN_TREND, vo, CacheConstant.STATISTICS_ADMIN_TREND_TTL);
        log.debug("趋势统计写入缓存");

        return vo;
    }

        private static Map<String, Map<String, Object>> indexByDate(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object date = row.get("stat_date");
            if (date != null) {
                result.put(date.toString(), row);
            }
        }
        return result;
    }

    @Override
    public Page<AdminUserVO> listUsers(Integer pageNum, Integer pageSize, String keyword, Integer accountStatus, Integer publishPermission) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getIsDelete, 0);
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getStudentId, kw)
                    .or().like(User::getRealName, kw)
                    .or().like(User::getNickname, kw)
                    .or().like(User::getPhone, kw));
        }
        if (accountStatus != null) {
            wrapper.eq(User::getAccountStatus, accountStatus);
        }
        if (publishPermission != null) {
            wrapper.eq(User::getPublishPermission, publishPermission);
        }
        wrapper.orderByAsc(User::getId);

        Page<User> userPage = userMapper.selectPage(page, wrapper);
        Set<Long> professionalIds = userPage.getRecords().stream()
                .map(User::getProfessionalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> professionalNameMap = new HashMap<>();
        if (!professionalIds.isEmpty()) {
            LambdaQueryWrapper<Professional> profWrapper = new LambdaQueryWrapper<>();
            profWrapper.in(Professional::getId, professionalIds);
            for (Professional p : professionalMapper.selectList(profWrapper)) {
                professionalNameMap.put(p.getId(), p.getProfessionalName());
            }
        }

        List<AdminUserVO> voList = userPage.getRecords().stream().map(user -> {
            AdminUserVO vo = new AdminUserVO();
            vo.setUserId(user.getId());
            vo.setStudentId(user.getStudentId());
            vo.setRealName(user.getRealName());
            vo.setNickname(user.getNickname());
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
            vo.setDormitory(user.getDormitory());
            vo.setGrade(user.getGrade());
            vo.setProfessionalId(user.getProfessionalId());
            vo.setProfessionalName(professionalNameMap.get(user.getProfessionalId()));
            vo.setRole(user.getRole());
            vo.setPublishPermission(user.getPublishPermission());
            vo.setPermissionReviewStatus(user.getPermissionReviewStatus());
            vo.setApplyReason(user.getPermissionApplyReason());
            vo.setWalletBalance(user.getWalletBalance());
            vo.setFrozenAmount(user.getFrozenAmount());
            vo.setAccountStatus(user.getAccountStatus());
            vo.setCreateTime(user.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        Page<AdminUserVO> voPage = new Page<>(pageNum, pageSize, userPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(AdminUserStatusRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        User target = userMapper.selectById(request.getUserId());
        if (target == null || (target.getIsDelete() != null && target.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        if (Integer.valueOf(UserConstant.ROLE_ADMIN).equals(target.getRole())
                && Integer.valueOf(UserConstant.ACCOUNT_DISABLED).equals(request.getAccountStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能禁用管理员账号");
        }

        User update = new User();
        update.setId(target.getId());
        boolean changed = false;

        if (request.getAccountStatus() != null) {
            if (request.getAccountStatus() != UserConstant.ACCOUNT_NORMAL
                    && request.getAccountStatus() != UserConstant.ACCOUNT_DISABLED) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户状态值不合法");
            }
            update.setAccountStatus(request.getAccountStatus());
            changed = true;
        }
        if (request.getPublishPermission() != null) {
            if (request.getPublishPermission() != UserConstant.NO_PUBLISH_PERMISSION
                    && request.getPublishPermission() != UserConstant.HAS_PUBLISH_PERMISSION) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "发布权限值不合法");
            }
            update.setPublishPermission(request.getPublishPermission());
            update.setPermissionReviewStatus(request.getPublishPermission() == UserConstant.HAS_PUBLISH_PERMISSION
                    ? UserConstant.PERMISSION_APPROVED
                    : UserConstant.PERMISSION_REJECTED);
            changed = true;
        }
        if (!changed) {
            return;
        }
        userMapper.updateById(update);
        if (redisUtils != null) {
            redisUtils.deleteUserCache(target.getId());
        }
        if (request.getPublishPermission() != null
                && !request.getPublishPermission().equals(target.getPublishPermission())) {
            boolean granted = request.getPublishPermission() == UserConstant.HAS_PUBLISH_PERMISSION;
            notificationService.sendPermissionNotification(target.getId(), granted,
                    granted ? null : "管理员调整/回收了您的服务发布权限");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        User target = userMapper.selectById(userId);
        if (target == null || (target.getIsDelete() != null && target.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        String defaultEncrypted = com.xueyifang.utils.PasswordUtils.hashPassword("password123");
        User update = new User();
        update.setId(userId);
        update.setPassword(defaultEncrypted);
        userMapper.updateById(update);
        if (redisUtils != null) {
            redisUtils.deleteUserCache(userId);
        }
    }

    @Override
    public Page<PendingServiceVO> listAllServices(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        Page<com.xueyifang.domain.po.Service> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<com.xueyifang.domain.po.Service> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.xueyifang.domain.po.Service::getIsDelete, 0);
        if (status != null) {
            wrapper.eq(com.xueyifang.domain.po.Service::getStatus, status);
        }
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(com.xueyifang.domain.po.Service::getServiceTitle, kw)
                    .or().like(com.xueyifang.domain.po.Service::getServiceDescription, kw));
        }
        wrapper.orderByDesc(com.xueyifang.domain.po.Service::getCreateTime);

        Page<com.xueyifang.domain.po.Service> servicePage = serviceMapper.selectPage(page, wrapper);
        Set<Long> sellerIds = servicePage.getRecords().stream()
                .map(com.xueyifang.domain.po.Service::getSellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> tagIds = servicePage.getRecords().stream()
                .map(com.xueyifang.domain.po.Service::getTagId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> sellerNameMap = new HashMap<>();
        if (!sellerIds.isEmpty()) {
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.in(User::getId, sellerIds);
            for (User u : userMapper.selectList(userWrapper)) {
                sellerNameMap.put(u.getId(), u.getRealName());
            }
        }
        Map<Long, String> tagNameMap = new HashMap<>();
        if (!tagIds.isEmpty()) {
            LambdaQueryWrapper<ServiceTag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.in(ServiceTag::getId, tagIds);
            for (ServiceTag tag : serviceTagMapper.selectList(tagWrapper)) {
                tagNameMap.put(tag.getId(), tag.getTagName());
            }
        }
        Set<Long> serviceIds = servicePage.getRecords().stream()
                .map(com.xueyifang.domain.po.Service::getId)
                .collect(Collectors.toSet());
        Map<Long, String> coverImageMap = serviceImageService.getCoverImageMap(serviceIds);

        List<PendingServiceVO> voList = servicePage.getRecords().stream().map(service -> {
            PendingServiceVO vo = new PendingServiceVO();
            vo.setServiceId(service.getId());
            vo.setServiceTitle(service.getServiceTitle());
            vo.setCoverImage(coverImageMap.get(service.getId()));
            vo.setDescription(service.getServiceDescription());
            vo.setSellerName(sellerNameMap.get(service.getSellerId()));
            vo.setTagName(tagNameMap.get(service.getTagId()));
            vo.setPrice(service.getPrice());
            vo.setStatus(service.getStatus());
            vo.setReviewStatus(service.getReviewStatus());
            vo.setReviewReason(service.getReviewReason());
            vo.setCreateTime(service.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        Page<PendingServiceVO> voPage = new Page<>(pageNum, pageSize, servicePage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceOfflineService(Long serviceId, String reason) {
        if (serviceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "服务ID不能为空");
        }
        com.xueyifang.domain.po.Service service = serviceMapper.selectById(serviceId);
        if (service == null || (service.getIsDelete() != null && service.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_EXIST);
        }
        String cleanReason = StringUtils.isNotBlank(reason) ? reason.trim() : "管理员巡检违规强制下架";
        com.xueyifang.domain.po.Service update = new com.xueyifang.domain.po.Service();
        update.setId(serviceId);
        update.setStatus(ServiceConstant.STATUS_OFFLINE);
        update.setReviewStatus(ServiceConstant.REVIEW_PENDING);
        update.setReviewReason(cleanReason);
        update.setReviewedBy(UserContext.getCurrentUserId());
        update.setReviewTime(LocalDateTime.now());
        serviceMapper.updateById(update);

        if (redisUtils != null) {
            redisUtils.delete(CacheConstant.SERVICE_DETAIL_PREFIX + serviceId);
        }
        notificationService.sendServiceRemovalNotification(service.getSellerId(), serviceId, service.getServiceTitle(), cleanReason);
    }
}
