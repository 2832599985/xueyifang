package com.xueyifang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xueyifang.domain.dto.EduVerifyConfirmRequest;
import com.xueyifang.domain.dto.EduVerifySendRequest;
import com.xueyifang.domain.dto.UserChangePasswordRequest;
import com.xueyifang.domain.dto.UserLoginRequest;
import com.xueyifang.domain.dto.UserRegisterRequest;
import com.xueyifang.domain.dto.UserUpdateRequest;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.LoginUserVO;
import com.xueyifang.domain.vo.UserRegisterVO;

import java.util.Map;

public interface IUserService extends IService<User> {

    UserRegisterVO register(UserRegisterRequest registerRequest);

    LoginUserVO login(UserLoginRequest loginRequest);

    boolean logout(String token);

    User getUserById(Long userId);

    boolean updateUserProfile(Long userId, UserUpdateRequest updateRequest);

    boolean changePassword(Long userId, UserChangePasswordRequest changePasswordRequest);

    Map<String, Object> sendEduVerifyCode(Long userId, EduVerifySendRequest request);

    void confirmEduVerify(Long userId, EduVerifyConfirmRequest request);
}
