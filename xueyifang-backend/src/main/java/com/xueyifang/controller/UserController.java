package com.xueyifang.controller;


import com.xueyifang.annotation.RateLimit;
import com.xueyifang.common.BaseResponse;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.common.ResultUtils;
import com.xueyifang.constant.SysConfigConstant;
import com.xueyifang.domain.dto.UserChangePasswordRequest;
import com.xueyifang.domain.dto.UserLoginRequest;
import com.xueyifang.domain.dto.UserRegisterRequest;
import com.xueyifang.domain.dto.UserUpdateRequest;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.LoginUserVO;
import com.xueyifang.domain.vo.UserRegisterVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.ISysConfigService;
import com.xueyifang.service.IUserService;
import com.xueyifang.utils.JwtUtils;
import com.xueyifang.utils.UserContext;
import com.xueyifang.config.FileStorageProperties;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * ## 🔐 用户认证模块
 *
 * ### 1. 用户注册
 * **请求方式**：`POST /auth/register`
 *
 * **请求体**：
 * ```json
 * {
 *   "studentId": "20240001",
 *   "password": "password123",
 *   "confirmPassword": "password123",
 *   "realName": "张三",
 *   "phone": "13800138000",
 *   "professionalId": 1,
 *   "email": "zhangsan@example.com",
 *   "dormitory": "宿舍1-101"
 * }
 * ```
 *
 * **响应**：
 * ```json
 * {
 *   "code": 0,
 *   "message": "注册成功",
 *   "data": {
 *     "userId": 1,
 *     "studentId": "20240001",
 *     "realName": "张三",
 *     "publishPermission": 0,
 *     "createTime": "2025-10-27 16:00:00"
 *   }
 * }
 * ```
 *
 * **说明**：
 * - `studentId`：学号，必填，全局唯一
 * - `password`：密码，8-16位，需包含数字和字母
 * - `confirmPassword`：确认密码，需与password相同
 * - 注册成功后默认 `publishPermission=0`（无发布权限）
 * - 返回200且code=0表示注册成功
 *
 * ---
 *
 * ### 2. 用户登录
 * **请求方式**：`POST /auth/login`
 *
 * **请求体**：
 * ```json
 * {
 *   "studentId": "20240001",
 *   "password": "password123"
 * }
 * ```
 *
 * **响应**：
 * ```json
 * {
 *   "code": 0,
 *   "message": "登录成功",
 *   "data": {
 *     "userId": 1,
 *     "studentId": "20240001",
 *     "realName": "张三",
 *     "nickname": "小张",
 *     "avatar": "https://example.com/avatar.jpg",
 *     "role": 1,
 *     "publishPermission": 1,
 *     "permissionReviewStatus": 1,
 *     "walletBalance": 100.00,
 *     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *     "tokenExpireTime": 1698340800
 *   }
 * }
 * ```
 *
 * **说明**：
 * - 登录成功返回JWT Token
 * - Token需放在后续请求的Authorization header中
 * - `role`：1=学生，2=管理员
 * - `publishPermission`：0=无，1=有
 * - `permissionReviewStatus`：0=未审核，1=通过，2=拒绝
 *
 * ---
 *
 * ### 3. 获取当前用户信息
 * **请求方式**：`GET /auth/currentUser`
 *
 * **请求头**：
 * ```
 * Authorization: Bearer {token}
 * ```
 *
 * **响应**：
 * ```json
 * {
 *   "code": 0,
 *   "message": "success",
 *   "data": {
 *     "userId": 1,
 *     "studentId": "20240001",
 *     "realName": "张三",
 *     "nickname": "小张",
 *     "phone": "13800138000",
 *     "email": "zhangsan@example.com",
 *     "dormitory": "宿舍1-101",
 *     "grade": "大三",
 *     "professionalId": 1,
 *     "professionalName": "计算机科学与技术",
 *     "avatar": "https://example.com/avatar.jpg",
 *     "bio": "热爱编程",
 *     "role": 1,
 *     "publishPermission": 1,
 *     "permissionReviewStatus": 1,
 *     "walletBalance": 100.00,
 *     "frozenAmount": 50.00,
 *     "accountStatus": 1
 *   }
 * }
 * ```
 *
 * ---
 *
 * ### 4. 修改个人信息
 * **请求方式**：`PUT /auth/updateProfile`
 *
 * **请求头**：
 * ```
 * Authorization: Bearer {token}
 * ```
 *
 * **请求体**：
 * ```json
 * {
 *   "nickname": "小张",
 *   "email": "new@example.com",
 *   "dormitory": "宿舍2-202",
 *   "grade": "大三",
 *   "professionalId": 2,
 *   "avatar": "https://example.com/new_avatar.jpg",
 *   "bio": "热爱编程和设计"
 * }
 * ```
 *
 * **响应**：
 * ```json
 * {
 *   "code": 0,
 *   "message": "个人信息更新成功"
 * }
 * ```
 *
 * ---
 *
 * ### 5. 修改密码
 * **请求方式**：`PUT /auth/changePassword`
 *
 * **请求头**：
 * ```
 * Authorization: Bearer {token}
 * ```
 *
 * **请求体**：
 * ```json
 * {
 *   "oldPassword": "password123",
 *   "newPassword": "newpassword456",
 *   "confirmPassword": "newpassword456"
 * }
 * ```
 *
 * **响应**：
 * ```json
 * {
 *   "code": 0,
 *   "message": "密码修改成功"
 * }
 * ```
 */

/**
 * 用户接口
 *
 * @author luoyi
 */
@RestController
@RequestMapping("/auth")
@Slf4j
@Api(tags = "用户认证模块")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IProfessionalService professionalService;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private FileStorageProperties fileStorageProperties;

    @Resource
    private ISysConfigService sysConfigService;

    @Resource
    private com.xueyifang.utils.FileUrlResolver fileUrlResolver;

    @Resource
    private com.xueyifang.utils.RedisUtils redisUtils;

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求参数
     * @return 用户信息
     */
    @PostMapping("/register")
    @ApiOperation(value = "用户注册", notes = "新用户注册接口")
    @RateLimit(key = "register", maxRequests = 5, windowSeconds = 60)
    public BaseResponse<UserRegisterVO> register(@RequestBody UserRegisterRequest registerRequest) {
        log.info("用户注册请求，学号：{}", registerRequest.getStudentId());

        // 检查注册开关（兼容 "1" 与 "true"）
        String registerEnabled = sysConfigService.getConfigValue(
                SysConfigConstant.REGISTER_ENABLED,
                SysConfigConstant.REGISTER_ENABLED_DEFAULT
        );
        if (!"1".equals(registerEnabled) && !"true".equalsIgnoreCase(registerEnabled)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "注册功能已关闭，请联系管理员");
        }

        // 调用服务层进行注册
        UserRegisterVO registerVO = userService.register(registerRequest);

        return ResultUtils.success(registerVO);

    }

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求参数
     * @return 用户信息和Token
     */
    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "用户登录接口，返回用户信息和Token")
    @RateLimit(key = "login", maxRequests = 10, windowSeconds = 60)
    public BaseResponse<Map<String, Object>> login(@RequestBody UserLoginRequest loginRequest) {
        log.info("用户登录请求，学号：{}", loginRequest.getStudentId());

        // 调用服务层进行登录（包含密码验证）
        LoginUserVO loginUserVO = userService.login(loginRequest);

        // 生成Token（使用用户ID生成）
        String token = jwtUtils.generateToken(loginUserVO.getId());

        // 构建返回结果（用户信息 + Token）
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("userInfo", loginUserVO);
        resultMap.put("token", token);

        log.info("用户登录成功，学号：{}，用户ID：{}", loginRequest.getStudentId(), loginUserVO.getId());
        return ResultUtils.success(resultMap);
    }

    /**
     * 用户登出
     * 
     * 将Token加入Redis黑名单，使其立即失效
     *
     * @param request HTTP请求对象
     * @return 是否成功
     */
    @PostMapping("/logout")
    @ApiOperation(value = "用户登出", notes = "将Token加入黑名单，使其立即失效")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        // 从拦截器设置的Request属性中获取Token
        String token = (String) request.getAttribute("currentToken");
        
        log.info("用户登出请求，userId：{}", UserContext.getCurrentUserId());
        boolean result = userService.logout(token);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户信息
     *
     * 注意：此接口需要登录后访问，拦截器会自动将用户信息存入 UserContext
     *
     * @return 当前用户信息
     */
    @GetMapping("/currentUser")
    @ApiOperation(value = "获取当前用户", notes = "获取当前登录用户信息（从ThreadLocal获取）")
    public BaseResponse<LoginUserVO> getCurrentUser() {
        // 从 UserContext 获取当前用户（拦截器已设置）
        User currentUser = UserContext.getCurrentUser();

        // 防御性编程：检查用户是否为空（理论上拦截器已经保证不为空）
        if (currentUser == null) {
            log.error("UserContext中没有用户信息，可能是拦截器未生效");
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN, "请先登录");
        }

        // 获取最新用户信息并刷新缓存
        User latestUser = userService.getById(currentUser.getId());
        if (latestUser != null) {
            currentUser = latestUser;
            if (redisUtils != null) {
                redisUtils.cacheUser(currentUser.getId(), currentUser, 1800L);
            }
        }

        // 转换为VO（脱敏）
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(currentUser, loginUserVO);

        // 填充专业名称
        if (currentUser.getProfessionalId() != null) {
            Professional professional = professionalService.getById(currentUser.getProfessionalId());
            if (professional != null) {
                loginUserVO.setProfessionalName(professional.getProfessionalName());
            }
        }
        if (StringUtils.isNotBlank(loginUserVO.getAvatar()) && fileUrlResolver != null) {
            loginUserVO.setAvatar(fileUrlResolver.toPublicUrl(loginUserVO.getAvatar()));
        }

        log.debug("获取当前用户信息，userId：{}", currentUser.getId());
        return ResultUtils.success(loginUserVO);
    }


    /**
     * 修改用户信息
     *
     * @param updateRequest 修改请求参数
     * @return 是否成功
     */
    @PutMapping("/updateProfile")
    @ApiOperation(value = "修改个人信息", notes = "修改当前登录用户的个人信息")
    public BaseResponse<Void> updateProfile(@RequestBody UserUpdateRequest updateRequest) {
        // 从 UserContext 获取当前用户ID（拦截器已设置）
        Long currentUserId = UserContext.getCurrentUserId();

        if (currentUserId == null) {
            log.error("UserContext中没有用户ID，可能是拦截器未生效");
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN, "请先登录");
        }

        log.info("用户更新个人信息请求，userId：{}", currentUserId);

        // 调用服务层更新用户信息
        userService.updateUserProfile(currentUserId, updateRequest);

        return ResultUtils.success("个人信息更新成功");
    }


    /**
     * 修改密码
     *
     * @param changePasswordRequest 修改密码请求参数
     * @return 是否成功
     */
    @PutMapping("/changePassword")
    @ApiOperation(value = "修改密码", notes = "修改当前登录用户的密码")
    public BaseResponse<Void> changePassword(@RequestBody UserChangePasswordRequest changePasswordRequest) {
        // 从 UserContext 获取当前用户ID（拦截器已设置）
        Long currentUserId = UserContext.getCurrentUserId();

        if (currentUserId == null) {
            log.error("UserContext中没有用户ID，可能是拦截器未生效");
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN, "请先登录");
        }

        log.info("用户修改密码请求，userId：{}", currentUserId);

        // 调用服务层修改密码
        userService.changePassword(currentUserId, changePasswordRequest);

        return ResultUtils.success("密码修改成功");
    }

}
