package com.commerce.user.service;

import com.commerce.user.dto.MessageResponse;
import com.commerce.user.dto.auth.SignupRequest;
import com.commerce.user.dto.user.PagedUserResponse;
import com.commerce.user.dto.user.UserInfoResponse;
import com.commerce.user.dto.user.UserUpdateRequest;
import com.commerce.user.model.AppRole;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;

/**
 * @author Yixi Wan
 * @date 2025/10/28 22:11
 * @package com.commerce.user.service
 * <p>
 * Description:
 */
public interface UserService {
    MessageResponse registerUser(SignupRequest request, AppRole role);

    UserInfoResponse getUserInfo(String keycloakId);

    MessageResponse updateUser(String keycloakId, @Valid UserUpdateRequest request);

    MessageResponse deleteUser(String keycloakId);

    PagedUserResponse getAllSellers(Pageable pageDetails);
}
