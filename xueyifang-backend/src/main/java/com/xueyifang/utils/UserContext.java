package com.xueyifang.utils;

import com.xueyifang.domain.po.User;

/**
 * 用户上下文工具类
 * 使用 ThreadLocal 存储当前线程的用户信息
 * 
 * 优点：
 * 1. 同一个请求线程中，任何地方都可以获取当前用户
 * 2. 线程隔离，不会相互影响
 * 3. 避免参数传递，代码更简洁
 * 
 * 使用场景：
 * - 在Service层获取当前用户
 * - 在AOP切面获取当前用户
 * - 在工具类中获取当前用户
 * 
 * @author xueyifang
 */
public class UserContext {

    /**
     * ThreadLocal 存储当前用户
     * 每个线程都有独立的副本，互不干扰
     */
    private static final ThreadLocal<User> USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前用户
     * 
     * @param user 用户对象
     */
    public static void setCurrentUser(User user) {
        USER_THREAD_LOCAL.set(user);
    }

    /**
     * 获取当前用户
     * 
     * @return 当前用户，如果未登录返回 null
     */
    public static User getCurrentUser() {
        return USER_THREAD_LOCAL.get();
    }

    /**
     * 获取当前用户ID
     * 
     * @return 用户ID，如果未登录返回 null
     */
    public static Long getCurrentUserId() {
        User user = USER_THREAD_LOCAL.get();
        return user != null ? user.getId() : null;
    }

    /**
     * 获取当前用户学号
     * 
     * @return 学号，如果未登录返回 null
     */
    public static String getCurrentStudentId() {
        User user = USER_THREAD_LOCAL.get();
        return user != null ? user.getStudentId() : null;
    }

    /**
     * 判断当前是否已登录
     * 
     * @return true=已登录，false=未登录
     */
    public static boolean isLogin() {
        return USER_THREAD_LOCAL.get() != null;
    }

    /**
     * 判断当前用户是否为管理员
     * 
     * @return true=管理员，false=非管理员或未登录
     */
    public static boolean isAdmin() {
        User user = USER_THREAD_LOCAL.get();
        return user != null && user.getRole() != null && user.getRole() == 2;
    }

    /**
     * 清除当前用户信息
     * ⚠️ 重要：请求结束后必须调用，防止内存泄漏
     */
    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }
}

