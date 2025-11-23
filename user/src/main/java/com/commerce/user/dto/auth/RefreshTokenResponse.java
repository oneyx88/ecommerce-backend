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
 * Description: 刷新 token 响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {
    private String message;
    private String accessToken;
    private String refreshToken;
    private Integer accessTokenExpiresIn;
    private Integer refreshTokenExpiresIn;
}