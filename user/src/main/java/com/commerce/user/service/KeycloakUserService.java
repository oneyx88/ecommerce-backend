package com.commerce.user.service;

import com.commerce.user.dto.auth.SignupRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @author Yixi Wan
 * @date 2025/10/29 10:38
 * @package com.commerce.user.service
 * <p>
 * Description:
 */
public interface KeycloakUserService {
    /**
     * 在 Keycloak 中创建用户
     * @param signupRequest 注册请求DTO
     * @return 新建用户的 Keycloak ID
     */
    String createUser(SignupRequest signupRequest);

    /**
     * 为用户分配角色（可选功能）
     * @param userId Keycloak 用户ID
     * @param roleName 角色名称，如 "ROLE_USER"
     */
    void assignRealmRole(String userId, String roleName);

    void deleteUser(String keycloakId);

    void updateUser(String keycloakId, @NotBlank @Size(max = 50) String firstName, @NotBlank @Size(max = 50) String lastName, @NotBlank @Size(max = 50) @Email String email, @NotBlank @Size(min = 8, max = 40) @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,40}$",
            message = "Password must contain uppercase, lowercase, number, and special character"
    ) String password);
}
