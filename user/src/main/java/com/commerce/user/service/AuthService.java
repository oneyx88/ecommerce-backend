package com.commerce.user.service;

import com.commerce.user.dto.auth.LoginRequest;
import com.commerce.user.dto.auth.RefreshTokenResponse;
import com.commerce.user.dto.auth.LoginSuccessResponse;
import com.commerce.user.clients.KeycloakTokenResponse;
import com.commerce.user.dto.user.PagedUserResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;

/**
 * @author Yixi Wan
 * @date 2025/11/19
 * @package com.commerce.user.service
 * <p>
 * Description: 认证服务接口
 */
public interface AuthService {
//    LoginSuccessResponse login(LoginRequest request);

    KeycloakTokenResponse loginForToken(LoginRequest request);

    RefreshTokenResponse refresh(String refreshToken);

    void logout(String refreshToken);

    // 下沉 Cookie 设置，以保持 Controller 简洁
    LoginSuccessResponse loginAndSetCookie(LoginRequest request, HttpServletResponse response);

    RefreshTokenResponse refreshAndRotateCookie(String refreshToken, HttpServletResponse response);

    void logoutAndClearCookie(String refreshToken, HttpServletResponse response);

}