package com.commerce.user.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author Yixi Wan
 * @date 2025/11/19
 * @package com.commerce.user.dto
 * <p>
 * Description: 登录成功返回的用户信息简版
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserResponse {
    private String id;        // keycloak user id (sub)
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
}