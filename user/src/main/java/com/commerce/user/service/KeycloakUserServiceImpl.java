package com.commerce.user.service;

import com.commerce.user.dto.auth.SignupRequest;
import com.commerce.user.exceptions.ApiException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Yixi Wan
 * @date 2025/10/29 10:39
 * @package com.commerce.user.service
 * <p>
 * Description:
 */
@Service
public class KeycloakUserServiceImpl implements KeycloakUserService {
    @Autowired
    private Keycloak keycloak;

    @Value( "${keycloak.realm}")
    private String realm;

    @Override
    public String createUser(SignupRequest signupRequest) {
        // 构建用户信息
        UserRepresentation user = new UserRepresentation();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setEnabled(true);

        // 构建密码凭证
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setTemporary(false);
        cred.setValue(signupRequest.getPassword());
        user.setCredentials(List.of(cred));

        // 调用 Keycloak API 创建用户
        UsersResource users = keycloak.realm(realm).users();
        Response response = users.create(user);

        if (response.getStatus() == 201) {
            String location = response.getLocation().toString();
            return location.substring(location.lastIndexOf('/') + 1);
        } else if (response.getStatus() == 409) {
            throw new ApiException("Keycloak Error: User already exists: " + signupRequest.getUsername(), HttpStatus.BAD_REQUEST);
        } else {
            throw new ApiException("Keycloak Error: Failed to create user: " + response.getStatusInfo(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public void assignRealmRole(String userId, String roleName) {
        RealmResource realmResource = keycloak.realm(realm);

        RoleRepresentation role;
        try {
            role = realmResource.roles().get(roleName).toRepresentation();
        } catch (NotFoundException e) {
            throw new ApiException("Role '" + roleName + "' does not exist in realm '" + realm + "'", HttpStatus.NOT_FOUND);
        }

        try {
            realmResource.users().get(userId).roles().realmLevel().add(List.of(role));
        } catch (NotFoundException e) {
            throw new ApiException("User ID '" + userId + "' not found in realm '" + realm + "'", HttpStatus.NOT_FOUND);
        } catch (ForbiddenException e) {
            throw new ApiException("Insufficient permissions to assign role '" + roleName + "'", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            throw new ApiException("Unexpected error assigning role '" + roleName + "': " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateUser(
            String keycloakId,
            String firstName,
            String lastName,
            String email,
            String password
    ) {
        UsersResource usersResource = keycloak.realm(realm).users();
        UserResource userResource;

        try {
            userResource = usersResource.get(keycloakId);
        } catch (jakarta.ws.rs.NotFoundException e) {
            throw new ApiException("User ID '" + keycloakId + "' not found", HttpStatus.NOT_FOUND);
        }

        UserRepresentation representation;

        try {
            representation = userResource.toRepresentation();
        } catch (Exception e) {
            throw new ApiException("Failed to fetch user representation", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 修改基本信息
        representation.setFirstName(firstName);
        representation.setLastName(lastName);
        representation.setEmail(email);

        try {
            userResource.update(representation);
        } catch (jakarta.ws.rs.ForbiddenException e) {
            throw new ApiException("No permission to update user", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            throw new ApiException("Failed to update user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 修改密码（最新文档仍使用 resetPassword）
        if (password != null && !password.isBlank()) {

            CredentialRepresentation pwdCred = new CredentialRepresentation();
            pwdCred.setTemporary(false);
            pwdCred.setType(CredentialRepresentation.PASSWORD);
            pwdCred.setValue(password);

            try {
                userResource.resetPassword(pwdCred);
            } catch (jakarta.ws.rs.ForbiddenException e) {
                throw new ApiException("No permission to reset password", HttpStatus.FORBIDDEN);
            } catch (Exception e) {
                throw new ApiException("Failed to reset password: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    // DELETE USER
    @Override
    public void deleteUser(String keycloakId) {
        UsersResource usersResource = keycloak.realm(realm).users();

        try {
            usersResource.get(keycloakId).remove();
        } catch (jakarta.ws.rs.NotFoundException e) {
            throw new ApiException("User ID '" + keycloakId + "' not found", HttpStatus.NOT_FOUND);
        } catch (jakarta.ws.rs.ForbiddenException e) {
            throw new ApiException("No permission to delete user", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            throw new ApiException("Failed to delete user: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
