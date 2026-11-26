package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.config.FileStorageProperties;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.EduVerifyConfirmRequest;
import com.xueyifang.domain.dto.EduVerifySendRequest;
import com.xueyifang.domain.dto.UserChangePasswordRequest;
import com.xueyifang.domain.dto.UserLoginRequest;
import com.xueyifang.domain.dto.UserRegisterRequest;
import com.xueyifang.domain.dto.UserUpdateRequest;
import com.xueyifang.domain.po.Campus;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.LoginUserVO;
import com.xueyifang.domain.vo.UserRegisterVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.CampusMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.ICampusService;
import com.xueyifang.service.ICreditScoreService;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.IUserService;
import com.xueyifang.utils.FileUrlResolver;
import com.xueyifang.utils.JwtUtils;
import com.xueyifang.utils.PasswordUtils;
import com.xueyifang.utils.RedisUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final String EDU_CODE_KEY_PREFIX = "user:edu:otp:";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${password.salt:xueyifang}")
    private String passwordSalt;

    @Value("${xueyifang.user.nickname-prefix:xueyifang}")
    private String nicknamePrefix;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private IProfessionalService professionalService;

    @Resource
    private FileStorageProperties fileStorageProperties;

    @Resource
    private FileUrlResolver fileUrlResolver;

    @Resource
    private CampusMapper campusMapper;

    @Resource
    private ICampusService campusService;

    @Resource
    private ICreditScoreService creditScoreService;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,16}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._+-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRegisterVO register(UserRegisterRequest registerRequest) {
        validateRegisterRequest(registerRequest);

        String studentId = registerRequest.getStudentId();
        String password = registerRequest.getPassword();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("student_id", studentId);
        if (this.count(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_STUDENT_ID_EXIST);
        }

        User user = new User();
        user.setStudentId(studentId);
        user.setPassword(PasswordUtils.hashPassword(password));
        user.setRealName(registerRequest.getRealName());
        user.setPhone(registerRequest.getPhone());
        user.setProfessionalId(registerRequest.getProfessionalId());
        user.setCampusId(1L);
        user.setEmail(registerRequest.getEmail());
        user.setDormitory(registerRequest.getDormitory());
        user.setNickname(generateUniqueNickname());
        user.setRole(UserConstant.ROLE_STUDENT);
        user.setPublishPermission(UserConstant.NO_PUBLISH_PERMISSION);
        user.setPermissionReviewStatus(null);
        user.setWalletBalance(BigDecimal.ZERO);
        user.setFrozenAmount(BigDecimal.ZERO);
        user.setSellerDeposit(BigDecimal.ZERO);
        user.setEduVerified(0);
        user.setCreditScore(100);
        user.setAccountStatus(UserConstant.ACCOUNT_NORMAL);

        if (!this.save(user)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败");
        }

        UserRegisterVO registerVO = new UserRegisterVO();
        registerVO.setUserId(user.getId());
        registerVO.setStudentId(user.getStudentId());
        registerVO.setRealName(user.getRealName());
        registerVO.setPublishPermission(user.getPublishPermission());
        registerVO.setCreateTime(user.getCreateTime());
        return registerVO;
    }

    @Override
    public Map<String, Object> sendEduVerifyCode(Long userId, EduVerifySendRequest request) {
        if (userId == null || request == null || StringUtils.isBlank(request.getEduEmail())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入校园教育邮箱地址");
        }
        String email = request.getEduEmail().trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        Long campusId = user.getCampusId() != null ? user.getCampusId() : 1L;
        if (campusService != null && !campusService.isAllowedEduEmail(campusId, email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持本校认证后缀校园邮箱（如 .edu.cn）进行学籍实名核验");
        }

        String otp = String.format("%06d", RANDOM.nextInt(1000000));
        if (redisUtils != null) {
            redisUtils.set(EDU_CODE_KEY_PREFIX + userId, email + "|" + otp, 300L);
        }
        log.info("[校园实名邮箱核验] userId={}, eduEmail={}, 验证码已生成（有效期5分钟）", userId, email);

        Map<String, Object> res = new HashMap<>();
        res.put("eduEmail", email);
        res.put("expireSeconds", 300);
        // 非生产环境未配置企业 SMTP 时返回开发联调验证码，生产 profile 自动隐藏并走邮件通道投递
        if (!"prod".equalsIgnoreCase(activeProfile)) {
            res.put("devPreviewCode", otp);
        }
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmEduVerify(Long userId, EduVerifyConfirmRequest request) {
        if (userId == null || request == null
                || StringUtils.isAnyBlank(request.getEduEmail(), request.getVerifyCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入校园邮箱与6位验证码");
        }
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        if (Integer.valueOf(1).equals(user.getEduVerified())) {
            return;
        }
        String key = EDU_CODE_KEY_PREFIX + userId;
        Object raw = redisUtils != null ? redisUtils.get(key) : null;
        if (raw == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期，请重新获取");
        }
        String expected = request.getEduEmail().trim().toLowerCase() + "|" + request.getVerifyCode().trim();
        if (!expected.equals(raw.toString())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "校园邮箱或验证码不匹配");
        }
        if (redisUtils != null) {
            redisUtils.delete(key);
        }

        User update = new User();
        update.setId(userId);
        update.setEduVerified(1);
        update.setVerifiedEduEmail(request.getEduEmail().trim().toLowerCase());
        this.updateById(update);

        if (creditScoreService != null) {
            creditScoreService.adjustCreditScore(userId, 8, "EDU_VERIFY", "完成校园教育邮箱学籍核验奖励", null);
        }
        if (redisUtils != null) {
            redisUtils.deleteUserCache(userId);
        }
    }

    private void validateRegisterRequest(UserRegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (StringUtils.isAnyBlank(
                request.getStudentId(),
                request.getPassword(),
                request.getConfirmPassword(),
                request.getRealName(),
                request.getPhone())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "必填字段不能为空");
        }
        if (request.getProfessionalId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业ID不能为空");
        }
        if (request.getStudentId().length() < 6 || request.getStudentId().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号长度应在6-20位之间");
        }
        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码必须为8-16位，且包含数字和字母");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        if (!PHONE_PATTERN.matcher(request.getPhone()).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }
        if (request.getRealName().length() < 2 || request.getRealName().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "真实姓名长度应在2-20位之间");
        }
    }

    private String generateUniqueNickname() {
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return nicknamePrefix + "_" + uniqueId;
    }

    @Override
    public LoginUserVO login(UserLoginRequest loginRequest) {
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

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("student_id", studentId);
        queryWrapper.eq("is_delete", 0);
        User user = this.getOne(queryWrapper);

        if (user == null || !PasswordUtils.matches(password, user.getPassword(), passwordSalt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号或密码错误");
        }

        if (!PasswordUtils.isBcryptHash(user.getPassword())) {
            String upgradedHash = PasswordUtils.hashPassword(password);
            User upgradeEntity = new User();
            upgradeEntity.setId(user.getId());
            upgradeEntity.setPassword(upgradedHash);
            this.updateById(upgradeEntity);
            user.setPassword(upgradedHash);
        }

        if (UserConstant.ACCOUNT_DISABLED == user.getAccountStatus()) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账户已被禁用，请联系管理员");
        }

        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        if (loginUserVO.getCreditScore() == null) {
            loginUserVO.setCreditScore(100);
        }
        if (loginUserVO.getEduVerified() == null) {
            loginUserVO.setEduVerified(0);
        }
        if (user.getProfessionalId() != null && professionalService != null) {
            Professional professional = professionalService.getById(user.getProfessionalId());
            if (professional != null) {
                loginUserVO.setProfessionalName(professional.getProfessionalName());
            }
        }
        if (user.getCampusId() != null && campusMapper != null) {
            Campus campus = campusMapper.selectById(user.getCampusId());
            if (campus != null) {
                loginUserVO.setCampusName(campus.getCampusName());
            }
        }
        if (StringUtils.isNotBlank(loginUserVO.getAvatar()) && fileUrlResolver != null) {
            loginUserVO.setAvatar(fileUrlResolver.toPublicUrl(loginUserVO.getAvatar()));
        }
        if (redisUtils != null) {
            redisUtils.cacheUser(user.getId(), user, 1800L);
        }
        return loginUserVO;
    }

    @Override
    public boolean logout(String token) {
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Token不能为空");
        }
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Token无效");
        }
        Claims claims = jwtUtils.parseJwtToken(token);
        Date expiration = claims.getExpiration();
        long expireTime = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        if (expireTime > 0) {
            redisUtils.addTokenToBlacklist(token, expireTime);
        }
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
        if (userId == null || updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST, "用户不存在");
        }
        validateUpdateRequest(updateRequest, userId);

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

        boolean professionalChanged = false;
        String professionalRedisKey = null;
        if (updateRequest.getProfessionalId() != null && !updateRequest.getProfessionalId().equals(user.getProfessionalId())) {
            professionalRedisKey = "user:professional:update:" + userId;
            Object lastUpdateTime = redisUtils.get(professionalRedisKey);
            if (lastUpdateTime != null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业修改一个月只能修改一次");
            }
            user.setProfessionalId(updateRequest.getProfessionalId());
            professionalChanged = true;
        }
        if (StringUtils.isNotBlank(updateRequest.getAvatar())) {
            user.setAvatar(updateRequest.getAvatar());
        }
        if (updateRequest.getBio() != null) {
            user.setBio(updateRequest.getBio().trim());
        }

        if (!this.updateById(user)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败");
        }
        if (professionalChanged && professionalRedisKey != null) {
            redisUtils.set(professionalRedisKey, String.valueOf(System.currentTimeMillis()), 30 * 24 * 60 * 60);
        }
        if (redisUtils != null) {
            redisUtils.deleteUserCache(userId);
        }
        return true;
    }

    private void validateUpdateRequest(UserUpdateRequest request, Long currentUserId) {
        if (StringUtils.isNotBlank(request.getNickname())) {
            int len = request.getNickname().trim().length();
            if (len < 2 || len > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称长度应在2-20位之间");
            }
        }
        if (StringUtils.isNotBlank(request.getPhone()) && !PHONE_PATTERN.matcher(request.getPhone().trim()).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }
        if (StringUtils.isNotBlank(request.getEmail())) {
            if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
            }
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("email", request.getEmail());
            queryWrapper.ne("id", currentUserId);
            if (this.count(queryWrapper) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已被使用");
            }
        }
        if (StringUtils.isNotBlank(request.getGrade())) {
            String g = request.getGrade();
            if (!g.equals(UserConstant.GRADE_FRESHMAN)
                    && !g.equals(UserConstant.GRADE_SOPHOMORE)
                    && !g.equals(UserConstant.GRADE_JUNIOR)
                    && !g.equals(UserConstant.GRADE_SENIOR)
                    && !g.equals(UserConstant.GRADE_MASTER_1)
                    && !g.equals(UserConstant.GRADE_MASTER_2)
                    && !g.equals(UserConstant.GRADE_MASTER_3)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "年级取值不合法");
            }
        }
        if (request.getProfessionalId() != null) {
            if (request.getProfessionalId() <= 0 || professionalService.getById(request.getProfessionalId()) == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业不存在或已被删除");
            }
        }
        if (StringUtils.isNotBlank(request.getBio()) && request.getBio().length() > 200) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "个人简介不能超过200字");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(Long userId, UserChangePasswordRequest changePasswordRequest) {
        if (userId == null || changePasswordRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        String oldPassword = changePasswordRequest.getOldPassword();
        String newPassword = changePasswordRequest.getNewPassword();
        String confirmPassword = changePasswordRequest.getConfirmPassword();
        if (StringUtils.isAnyBlank(oldPassword, newPassword, confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST, "用户不存在");
        }
        if (!PasswordUtils.matches(oldPassword, user.getPassword(), passwordSalt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码错误");
        }
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码必须为8-16位，且包含数字和字母");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        }
        if (newPassword.equals(oldPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能与旧密码相同");
        }
        user.setPassword(PasswordUtils.hashPassword(newPassword));
        if (!this.updateById(user)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "密码修改失败");
        }
        if (redisUtils != null) {
            redisUtils.deleteUserCache(userId);
        }
        return true;
    }
}
