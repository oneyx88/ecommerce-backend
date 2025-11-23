package com.commerce.user.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yixi Wan
 * @date 2025/11/19
 * @package com.commerce.user.dto
 * <p>
 * Description: 登录成功响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginSuccessResponse {
    private String message;
    private LoginUserResponse user;
    private Integer accessTokenExpiresIn;
    private Integer refreshTokenExpiresIn;
}