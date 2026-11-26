import request from '@/api/request'
import type {
    LoginRequest,
    RegisterRequest,
    UpdateProfileRequest,
    ChangePasswordRequest,
    UserInfo,
    LoginResponse
} from '@/types/auth'

export const authApi = {
    /**
     * 用户登录
     */
    login(data: LoginRequest) {
        return request<LoginResponse>({
            url: '/auth/login',
            method: 'post',
            data
        })
    },

    /**
     * 用户注册
     */
    register(data: RegisterRequest) {
        return request<UserInfo>({
            url: '/auth/register',
            method: 'post',
            data
        })
    },

    /**
     * 获取当前用户信息
     */
    getCurrentUser() {
        return request<UserInfo>({
            url: '/auth/currentUser',
            method: 'get'
        })
    },

    /**
     * 修改个人信息
     */
    updateProfile(data: UpdateProfileRequest) {
        return request<void>({
            url: '/auth/updateProfile',
            method: 'put',
            data
        })
    },

    /**
     * 修改密码
     */
    changePassword(data: ChangePasswordRequest) {
        return request<void>({
            url: '/auth/changePassword',
            method: 'put',
            data
        })
    },

    /**
     * 退出登录
     */
    logout() {
        return request<boolean>({
            url: '/auth/logout',
            method: 'post'
        })
    }
}
