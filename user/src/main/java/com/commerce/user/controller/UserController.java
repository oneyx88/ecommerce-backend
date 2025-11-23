package com.commerce.user.controller;

import com.commerce.user.config.AppConstants;
import com.commerce.user.dto.*;
import com.commerce.user.dto.auth.LoginRequest;
import com.commerce.user.dto.auth.LoginSuccessResponse;
import com.commerce.user.dto.auth.RefreshTokenResponse;
import com.commerce.user.dto.auth.SignupRequest;
import com.commerce.user.dto.user.PagedUserResponse;
import com.commerce.user.dto.user.UserInfoResponse;
import com.commerce.user.dto.user.UserUpdateRequest;
import com.commerce.user.model.AppRole;
import com.commerce.user.service.UserService;
import com.commerce.user.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @author Yixi Wan
 * @date 2025/10/28 21:33
 * @package com.commerce.user.controller
 * <p>
 * Description:
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    @PreAuthorize("permitAll()")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(userService.registerUser(request, AppRole.USER));
    }

    @PostMapping("/signup/sellers")
    @PreAuthorize("permitAll()")
    public ResponseEntity<MessageResponse> registerSeller(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(userService.registerUser(request, AppRole.SELLER));
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserInfoResponse> getUserInfo(@RequestHeader("X-User-Id") String keycloakId) {
        UserInfoResponse userInfo = userService.getUserInfo(keycloakId);
        return ResponseEntity.ok(userInfo);
    }

    // 登录：写 HttpOnly Cookie，返回用户信息 + token 过期时间
    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LoginSuccessResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginSuccessResponse body = authService.loginAndSetCookie(request, response);
        return ResponseEntity.ok(body);
    }

    // 刷新 token：旋转 Cookie，返回新 access/refresh 及过期时间
    @PostMapping("/refresh")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RefreshTokenResponse> refresh(@CookieValue(value = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(RefreshTokenResponse.builder()
                    .message("Missing refresh_token cookie")
                    .accessToken(null)
                    .refreshToken(null)
                    .accessTokenExpiresIn(null)
                    .refreshTokenExpiresIn(null)
                    .build());
        }
        RefreshTokenResponse body = authService.refreshAndRotateCookie(refreshToken, response);
        return ResponseEntity.ok(body);
    }

    // 登出：通知 Keycloak 并清理 Cookie
    @PostMapping("/logout")
    @PreAuthorize("permitAll()")
    public ResponseEntity<MessageResponse> logout(@CookieValue(value = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        authService.logoutAndClearCookie(refreshToken, response);
        return ResponseEntity.ok(MessageResponse.builder().message("Logout successful").build());
    }

    @PutMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<MessageResponse> updateUser(
            @RequestHeader("X-User-Id") String keycloakId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        MessageResponse message = userService.updateUser(keycloakId, request);
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/{keycloakId}")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<MessageResponse> deleteUser(
            @PathVariable String keycloakId
    ) {
        MessageResponse message = userService.deleteUser(keycloakId);
        return ResponseEntity.ok(message);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/sellers")
    public ResponseEntity<?> getAllSellers(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_USERS_BY) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder
    ) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        return ResponseEntity.ok(userService.getAllSellers(pageDetails));
    }





}