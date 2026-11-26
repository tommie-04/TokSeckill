package com.tokenflash.user.auth;

// Loads the user from Redis, refreshes the token lifetime, and clears thread-local state after the request.

import cn.hutool.core.util.StrUtil;
import com.tokenflash.user.dto.UserDTO;
import com.tokenflash.infrastructure.redis.UserSessionStore;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final UserSessionStore sessions;

    public RefreshTokenInterceptor(UserSessionStore sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求头中的token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }
        UserDTO user = sessions.refreshSession(token);
        if (user != null) {
            UserHolder.saveUser(user);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }
}
