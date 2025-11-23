package com.commerce.user.service;

import com.commerce.user.dto.*;
import com.commerce.user.dto.auth.SignupRequest;
import com.commerce.user.dto.user.PagedUserResponse;
import com.commerce.user.dto.user.UserInfoResponse;
import com.commerce.user.dto.user.UserUpdateRequest;
import com.commerce.user.exceptions.ApiException;
import com.commerce.user.exceptions.ResourceNotFoundException;
import com.commerce.user.model.AppRole;
import com.commerce.user.model.Role;
import com.commerce.user.model.User;
import com.commerce.user.repository.RoleRepository;
import com.commerce.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Yixi Wan
 * @date 2025/10/28 22:12
 * @package com.commerce.user.service
 * <p>
 * Description:
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private KeycloakUserService keycloakUserService;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    @Transactional
    public MessageResponse registerUser(SignupRequest request, AppRole role) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new ApiException(
                    "Username " + request.getUsername() + " is already taken!",
                    HttpStatus.BAD_REQUEST
            );
        }


        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new ApiException(
                    "Email " + request.getEmail() + " is already taken!",
                    HttpStatus.BAD_REQUEST
            );
        }


        // 创建keycloak user
        String keycloakUserId = keycloakUserService.createUser(request); // 回调keycloak id
        keycloakUserService.assignRealmRole(keycloakUserId, String.valueOf(role));

        // 查找数据库role
        Role dbRole = roleRepository.findByRoleName(role)
                .orElseThrow(() -> new ApiException("Role " + role + " not found in database", HttpStatus.INTERNAL_SERVER_ERROR));

        // 构建user实体
        User user = modelMapper.map(request, User.class);
        user.setKeycloakId(keycloakUserId);
        user.setRoles(Set.of(dbRole));
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        return new MessageResponse("User registered successfully!");
    }

    @Override
    public UserInfoResponse getUserInfo(String keycloakId) {
        // 当数据库中不存在该 Keycloak 用户时，直接抛出异常
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "keycloakId", keycloakId));
        UserInfoResponse userInfo = modelMapper.map(user, UserInfoResponse.class);

        userInfo.setRoles(
                user.getRoles().stream()
                        .map(Role::getRoleName)   // Role → AppRole
                        .collect(Collectors.toSet())
        );
        log.info(user.getRoles().toString());
        return userInfo;

    }

    @Override
    @Transactional
    public MessageResponse updateUser(String keycloakId, UserUpdateRequest request) {

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "keycloakId", keycloakId));

        // 检查 email 是否被别人占用
        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email " + request.getEmail() + " is already taken!", HttpStatus.BAD_REQUEST);
        }

        // 更新 Keycloak 信息
        keycloakUserService.updateUser(
                keycloakId,
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword()
        );

        // 更新数据库
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        return new MessageResponse("User updated successfully!");
    }

    @Override
    @Transactional
    public MessageResponse deleteUser(String keycloakId) {

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "keycloakId", keycloakId));

        // Keycloak 侧删除用户
        keycloakUserService.deleteUser(keycloakId);

        // 数据库软删除
        user.setDeleted(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new MessageResponse("User deleted successfully!");
    }

    @Override
    public PagedUserResponse getAllSellers(Pageable pageable) {

        Page<User> userPage = userRepository.findAllByRole(AppRole.SELLER, pageable);
        List<User> users = userPage.getContent();

        if (users.isEmpty()) {
            throw new ResourceNotFoundException("No sellers found");
        }

        List<UserInfoResponse> responses = users.stream()
                .map(user -> {
                    UserInfoResponse dto = modelMapper.map(user, UserInfoResponse.class);
                    dto.setRoles(
                            user.getRoles().stream()
                                    .map(Role::getRoleName)
                                    .collect(Collectors.toSet())
                    );
                    return dto;
                })
                .collect(Collectors.toList());

        return PagedUserResponse.builder()
                .users(responses)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .isLastPage(userPage.isLast())
                .build();
    }



}
