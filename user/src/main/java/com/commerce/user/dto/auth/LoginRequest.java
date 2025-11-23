package com.commerce.user.dto.auth;

import lombok.Data;

/**
 * @author Yixi Wan
 * @date 2025/11/19
 * @package com.commerce.user.dto
 * <p>
 * Description: 登录请求 DTO
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}