package com.commerce.user.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author Yixi Wan
 * @date 2025/11/19 17:27
 * @package com.commerce.user.dto
 * <p>
 * Description:
 */
@Data
public class UserUpdateRequest {

    @NotBlank
    @Size(max = 50)
    private String firstName;

    @NotBlank
    @Size(max = 50)
    private String lastName;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 40)
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,40}$",
            message = "Password must contain uppercase, lowercase, number, and special character"
    )
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}

