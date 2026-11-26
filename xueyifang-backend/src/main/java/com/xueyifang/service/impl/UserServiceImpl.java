package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.UserChangePasswordRequest;
import com.xueyifang.domain.dto.UserLoginRequest;
import com.xueyifang.domain.dto.UserRegisterRequest;
import com.xueyifang.domain.dto.UserUpdateRequest;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.LoginUserVO;
import com.xueyifang.domain.vo.UserRegisterVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.utils.JwtUtils;
import com.xueyifang.utils.RedisUtils;
import com.xueyifang.config.FileStorageProperties;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author luoyi
 * @since 2025-10-27
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 密码加密盐值，从配置文件读取
     */
    @Value("${password.salt:xueyifang}")
    private String passwordSalt;

    /**
     * 用户昵称前缀，从配置文件读取
     */
    @Value("${xueyifang.user.nickname-prefix:xueyifang}")
    private String nicknamePrefix;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private IProfessionalService professionalService;

    @Resource
    private FileStorageProperties fileStorageProperties;

    @Resource
    private com.xueyifang.utils.FileUrlResolver fileUrlResolver;

    /**
     * 密码正则：8-16位，需包含数字和字母
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,16}$");

    /**
     * 手机号正则
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 邮箱正则
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRegisterVO register(UserRegisterRequest registerRequest) {
        // 1. 参数校验
        validateRegisterRequest(registerRequest);

        String studentId = registerRequest.getStudentId();
        String password = registerRequest.getPassword();
        String confirmPassword = registerRequest.getConfirmPassword();
        String realName = registerRequest.getRealName();
        String phone = registerRequest.getPhone();
        Long professionalId = registerRequest.getProfessionalId();

        // 2. 检查学号是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("student_id", studentId);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_STUDENT_ID_EXIST);
        }

        // 3. 密码加密（MD5 + 盐值）
        String encryptedPassword = encryptPassword(password);

        // 4. 创建用户对象
        User user = new User();
        user.setStudentId(studentId);
        user.setPassword(encryptedPassword);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setProfessionalId(professionalId);
        user.setEmail(registerRequest.getEmail());
        user.setDormitory(registerRequest.getDormitory());

        // 设置初始昵称
        user.setNickname(generateUniqueNickname());

        //todo 设置默认的头像url

        // 设置默认值（新注册学生默认无发布权限且处于未申请状态，需主动提交申请才进入待审核队列）
        user.setRole(UserConstant.ROLE_STUDENT);
        user.setPublishPermission(UserConstant.NO_PUBLISH_PERMISSION);
        user.setPermissionReviewStatus(null);
        user.setWalletBalance(BigDecimal.ZERO);
        user.setFrozenAmount(BigDecimal.ZERO);
        user.setAccountStatus(UserConstant.ACCOUNT_NORMAL);


        // 5. 保存用户
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败");
        }

        // 6. 构建返回对象
        UserRegisterVO registerVO = new UserRegisterVO();
        registerVO.setUserId(user.getId());
        registerVO.setStudentId(user.getStudentId());
        registerVO.setRealName(user.getRealName());
        registerVO.setPublishPermission(user.getPublishPermission());
        registerVO.setCreateTime(user.getCreateTime());

        log.info("用户注册成功，学号：{}", studentId);
        return registerVO;
    }

    /**
     * 校验注册请求参数
     */
    private void validateRegisterRequest(UserRegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        String studentId = request.getStudentId();
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();
        String realName = request.getRealName();
        String phone = request.getPhone();
        Long professionalId = request.getProfessionalId();

        // 必填字段校验
        if (StringUtils.isAnyBlank(studentId, password, confirmPassword, realName, phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "必填字段不能为空");
        }

        if (professionalId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业ID不能为空");
        }

        // 学号长度校验
        if (studentId.length() < 6 || studentId.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号长度应在6-20位之间");
        }

        // 密码格式校验
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码必须为8-16位，且包含数字和字母");
        }

        // 确认密码校验
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 手机号格式校验
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }

        // 真实姓名长度校验
        if (realName.length() < 2 || realName.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "真实姓名长度应在2-20位之间");
        }
    }

    /**
     * 密码加密（生产级 BCrypt 随机盐哈希）
     *
     * @param password 原始密码
     * @return 加密后的密码
     */
    private String encryptPassword(String password) {
        return com.xueyifang.utils.PasswordUtils.hashPassword(password);
    }

    /**
     * 生成唯一昵称
     * 格式：配置的前缀 + UUID的前8位
     *
     * @return 唯一昵称
     */
    private String generateUniqueNickname() {
        // 使用UUID的前8位确保唯一性
        String uniqueId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return nicknamePrefix + "_" + uniqueId;
    }

    @Override
    public LoginUserVO login(UserLoginRequest loginRequest) {
        // 1. 参数校验
        if (loginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        
        String studentId = loginRequest.getStudentId();
        String password = loginRequest.getPassword();
        
        if (StringUtils.isAnyBlank(studentId, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号或密码不能为空");
        }
        
        if (studentId.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号格式错误");
        }
        
        if (password.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式错误");
        }
        
        // 2. 按学号查询未删除用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("student_id", studentId);
        queryWrapper.eq("is_delete", 0);
        User user = this.getOne(queryWrapper);
        
        // 3. 校验密码（兼容 BCrypt 随机盐哈希与历史固定盐 MD5）
        if (user == null || !com.xueyifang.utils.PasswordUtils.matches(password, user.getPassword(), passwordSalt)) {
            log.info("用户登录失败，学号与密码不匹配：{}", studentId);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号或密码错误");
        }

        // 4. 若数据库中仍是历史 MD5 哈希，登录验证通过后自动无感升级为 BCrypt 随机盐哈希
        if (!com.xueyifang.utils.PasswordUtils.isBcryptHash(user.getPassword())) {
            String upgradedHash = com.xueyifang.utils.PasswordUtils.hashPassword(password);
            User upgradeEntity = new User();
            upgradeEntity.setId(user.getId());
            upgradeEntity.setPassword(upgradedHash);
            this.updateById(upgradeEntity);
            user.setPassword(upgradedHash);
            log.info("用户密码哈希已无感升级为 BCrypt，userId={}", user.getId());
        }
        
        // 5. 检查账户状态
        if (UserConstant.ACCOUNT_DISABLED == user.getAccountStatus()) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账户已被禁用，请联系管理员");
        }
        
        // 6. 构建返回对象（脱敏）
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);

        // 填充专业名称
        if (user.getProfessionalId() != null) {
            Professional professional = professionalService.getById(user.getProfessionalId());
            if (professional != null) {
                loginUserVO.setProfessionalName(professional.getProfessionalName());
            }
        }
        if (StringUtils.isNotBlank(loginUserVO.getAvatar()) && fileUrlResolver != null) {
            loginUserVO.setAvatar(fileUrlResolver.toPublicUrl(loginUserVO.getAvatar()));
        }
        if (redisUtils != null) {
            redisUtils.cacheUser(user.getId(), user, 1800L);
        }
        
        log.info("用户登录成功，学号：{}，用户ID：{}", studentId, user.getId());
        return loginUserVO;
    }

    @Override
    public boolean logout(String token) {
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Token不能为空");
        }
        
        // 验证Token
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Token无效");
        }
        
        // 计算Token的剩余有效期（用于设置黑名单过期时间）
        Claims claims = jwtUtils.parseJwtToken(token);
        Date expiration = claims.getExpiration();
        long expireTime = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        
        if (expireTime > 0) {
            // 将Token加入黑名单，使Token立即失效
            redisUtils.addTokenToBlacklist(token, expireTime);
            log.info("Token已加入黑名单，剩余有效期：{}秒", expireTime);
        }
        
        log.info("用户登出成功，Token已失效");
        return true;
    }

    @Override
    public User getUserById(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST, "用户不存在");
        }

        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserProfile(Long userId, UserUpdateRequest updateRequest) {
        // 1. 参数校验
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        if (updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 2. 查询用户是否存在
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST, "用户不存在");
        }

        // 3. 校验更新字段
        validateUpdateRequest(updateRequest, userId);

        // 4. 更新用户信息（只更新非空字段，允许清空寝室号与个人简介）
        if (StringUtils.isNotBlank(updateRequest.getNickname())) {
            user.setNickname(updateRequest.getNickname().trim());
        }

        if (StringUtils.isNotBlank(updateRequest.getPhone())) {
            user.setPhone(updateRequest.getPhone().trim());
        }

        if (StringUtils.isNotBlank(updateRequest.getEmail())) {
            user.setEmail(updateRequest.getEmail().trim());
        }

        if (updateRequest.getDormitory() != null) {
            user.setDormitory(updateRequest.getDormitory().trim());
        }

        if (StringUtils.isNotBlank(updateRequest.getGrade())) {
            user.setGrade(updateRequest.getGrade().trim());
        }

        // 专业修改标记，用于更新成功后设置 Redis 限制
        boolean professionalChanged = false;
        String professionalRedisKey = null;

        if (updateRequest.getProfessionalId() != null) {
            // 只有当专业ID真正改变时才进行限制检查和更新
            if (!updateRequest.getProfessionalId().equals(user.getProfessionalId())) {
                // 检查专业修改限制（30天只能改一次）
                professionalRedisKey = "user:professional:update:" + userId;
                Object lastUpdateTime = redisUtils.get(professionalRedisKey);
                if (lastUpdateTime != null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业修改一个月只能修改一次");
                }
                user.setProfessionalId(updateRequest.getProfessionalId());
                professionalChanged = true;
            }
        }

        if (StringUtils.isNotBlank(updateRequest.getAvatar())) {
            user.setAvatar(updateRequest.getAvatar());
        }

        if (updateRequest.getBio() != null) {
            user.setBio(updateRequest.getBio().trim());
        }

        // 5. 保存更新
        boolean updateResult = this.updateById(user);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败");
        }

        // 6. 更新成功后设置专业修改限制（避免更新失败时产生虚假限制）并清理用户缓存
        if (professionalChanged && professionalRedisKey != null) {
            redisUtils.set(professionalRedisKey, String.valueOf(System.currentTimeMillis()), 30 * 24 * 60 * 60);
        }
        if (redisUtils != null) {
            redisUtils.deleteUserCache(userId);
        }

        log.info("用户信息更新成功，userId：{}", userId);
        return true;
    }

    /**
     * 校验更新请求参数
     */
    private void validateUpdateRequest(UserUpdateRequest request, Long currentUserId) {
        // 昵称校验
        if (StringUtils.isNotBlank(request.getNickname())) {
            if (request.getNickname().trim().length() < 2 || request.getNickname().trim().length() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称长度应在2-20位之间");
            }
        }

        // 手机号校验
        if (StringUtils.isNotBlank(request.getPhone())) {
            if (!PHONE_PATTERN.matcher(request.getPhone().trim()).matches()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
            }
        }

        // 邮箱校验
        if (StringUtils.isNotBlank(request.getEmail())) {
            // 格式校验
            if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
            }

            // 检查邮箱是否已被其他用户使用
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("email", request.getEmail());
            queryWrapper.ne("id", currentUserId);
            long count = this.count(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已被使用");
            }
        }

        // 年级校验
        if (StringUtils.isNotBlank(request.getGrade())) {
            String grade = request.getGrade();
            if (!grade.equals(UserConstant.GRADE_FRESHMAN) &&
                !grade.equals(UserConstant.GRADE_SOPHOMORE) &&
                !grade.equals(UserConstant.GRADE_JUNIOR) &&
                !grade.equals(UserConstant.GRADE_SENIOR) &&
                !grade.equals(UserConstant.GRADE_MASTER_1) &&
                !grade.equals(UserConstant.GRADE_MASTER_2) &&
                !grade.equals(UserConstant.GRADE_MASTER_3)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    String.format("年级只能是：%s、%s、%s、%s、%s、%s、%s",
                        UserConstant.GRADE_FRESHMAN,
                        UserConstant.GRADE_SOPHOMORE,
                        UserConstant.GRADE_JUNIOR,
                        UserConstant.GRADE_SENIOR,
                        UserConstant.GRADE_MASTER_1,
                        UserConstant.GRADE_MASTER_2,
                        UserConstant.GRADE_MASTER_3));
            }
        }

        // 专业ID校验
        if (request.getProfessionalId() != null) {
            // 基本参数校验
            if (request.getProfessionalId() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业ID必须大于0");
            }

            // 专业ID存在性校验（MyBatis-Plus 会自动过滤已删除的数据）
            Professional professional = professionalService.getById(request.getProfessionalId());
            if (professional == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业不存在或已被删除");
            }

            log.info("专业ID校验通过，professionalId：{}，professionalName：{}",
                request.getProfessionalId(), professional.getProfessionalName());
        }

        // 个人简介长度校验
        if (StringUtils.isNotBlank(request.getBio())) {
            if (request.getBio().length() > 200) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "个人简介不能超过200字");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(Long userId, UserChangePasswordRequest changePasswordRequest) {
        log.info("用户修改密码请求，userId：{}", userId);

        // 1. 参数校验
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        if (changePasswordRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        String oldPassword = changePasswordRequest.getOldPassword();
        String newPassword = changePasswordRequest.getNewPassword();
        String confirmPassword = changePasswordRequest.getConfirmPassword();

        // 必填字段校验
        if (StringUtils.isAnyBlank(oldPassword, newPassword, confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }

        // 2. 查询用户是否存在
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST, "用户不存在");
        }

        // 3. 验证旧密码是否正确（兼容 BCrypt 与历史固定盐 MD5）
        if (!com.xueyifang.utils.PasswordUtils.matches(oldPassword, user.getPassword(), passwordSalt)) {
            log.warn("用户修改密码失败，旧密码错误，userId：{}", userId);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码错误");
        }

        // 4. 校验新密码格式
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码必须为8-16位，且包含数字和字母");
        }

        // 5. 校验新密码与确认密码是否一致
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        }

        // 6. 校验新密码不能与旧密码相同
        if (newPassword.equals(oldPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能与旧密码相同");
        }

        // 7. 加密新密码并更新
        String encryptedNewPassword = encryptPassword(newPassword);
        user.setPassword(encryptedNewPassword);

        boolean updateResult = this.updateById(user);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "密码修改失败");
        }
        if (redisUtils != null) {
            redisUtils.deleteUserCache(userId);
        }

        log.info("用户密码修改成功，userId：{}", userId);
        return true;
    }

}
