package com.commerce.user.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Yixi Wan
 * @date 2025/11/21 12:34
 * @package com.commerce.user.dto
 * <p>
 * Description:
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedUserResponse {

    private List<UserInfoResponse> users;

    private Integer pageNumber;
    private Integer pageSize;
    private Integer totalPages;
    private Long totalElements;
    private Boolean isLastPage;
}

