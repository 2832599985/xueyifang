package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xueyifang.domain.dto.admin.AdminUserStatusRequest;
import com.xueyifang.domain.vo.admin.AdminStatisticsVO;
import com.xueyifang.domain.vo.admin.AdminTrendVO;
import com.xueyifang.domain.vo.admin.AdminUserVO;
import com.xueyifang.domain.vo.admin.PendingServiceVO;
import com.xueyifang.domain.vo.admin.PendingUserVO;

/**
 * 管理员服务接口
 *
 * @author xueyifang
 */
public interface IAdminService {

    /**
     * 获取待审核用户列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 待审核用户分页列表
     */
    Page<PendingUserVO> getPendingUsers(Integer pageNum, Integer pageSize);

    /**
     * 审核用户权限申请
     *
     * @param userId 用户ID
     * @param approved true=批准, false=拒绝
     * @param reason 拒绝原因（拒绝时建议填写）
     */
    void reviewUserPermission(Long userId, Boolean approved, String reason);

    /**
     * 获取待审核服务列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 待审核服务分页列表
     */
    Page<PendingServiceVO> getPendingServices(Integer pageNum, Integer pageSize);

    /**
     * 审核服务发布申请
     *
     * @param serviceId 服务ID
     * @param approved true=批准, false=拒绝
     * @param reason 拒绝原因（拒绝时建议填写）
     */
    void reviewService(Long serviceId, Boolean approved, String reason);

    /**
     * 获取系统统计数据
     *
     * @return 系统统计数据
     */
    AdminStatisticsVO getSystemStatistics();

    /**
     * 获取近7天趋势数据
     *
     * @return 趋势数据
     */
    AdminTrendVO getTrendStatistics();

    /**
     * 分页查询全量用户列表（支持关键词、账户状态、发布权限筛选）
     */
    Page<AdminUserVO> listUsers(Integer pageNum, Integer pageSize, String keyword, Integer accountStatus, Integer publishPermission);

    /**
     * 更新用户账户状态或发布权限
     */
    void updateUserStatus(AdminUserStatusRequest request);

    /**
     * 重置用户密码为默认密码 password123
     */
    void resetUserPassword(Long userId);

    /**
     * 分页查询全量服务列表（支持按状态、关键词筛选）
     */
    Page<PendingServiceVO> listAllServices(Integer pageNum, Integer pageSize, Integer status, String keyword);

    /**
     * 管理员强制下架违规服务并发送通知
     */
    void forceOfflineService(Long serviceId, String reason);
}
