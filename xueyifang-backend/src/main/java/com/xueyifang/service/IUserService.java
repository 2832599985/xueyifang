package com.xueyifang.service;

import com.xueyifang.domain.dto.UserChangePasswordRequest;
import com.xueyifang.domain.dto.UserLoginRequest;
import com.xueyifang.domain.dto.UserRegisterRequest;
import com.xueyifang.domain.dto.UserUpdateRequest;
import com.xueyifang.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.vo.LoginUserVO;
import com.xueyifang.domain.vo.UserRegisterVO;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author luoyi
 * @since 2025-10-27
 */
public interface IUserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求参数
     * @return 注册成功后的用户信息
     */
    UserRegisterVO register(UserRegisterRequest registerRequest);

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求参数
     * @return 登录成功后的用户信息
     */
    LoginUserVO login(UserLoginRequest loginRequest);

    /**
     * 用户登出
     *
     * @param token 当前用户的Token
     * @return 是否成功
     */
    boolean logout(String token);

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param updateRequest 更新请求参数
     * @return 是否成功
     */
    boolean updateUserProfile(Long userId, UserUpdateRequest updateRequest);

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param changePasswordRequest 修改密码请求参数
     * @return 是否成功
     */
    boolean changePassword(Long userId, UserChangePasswordRequest changePasswordRequest);

}
