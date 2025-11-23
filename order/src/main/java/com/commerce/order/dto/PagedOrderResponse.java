package com.commerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Yixi Wan
 * @date 2025/11/20 20:49
 * @package com.commerce.order.dto
 * <p>
 * Description:
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedOrderResponse {

    private List<OrderResponse> orders;
    private Integer pageNumber;
    private Integer pageSize;
    private Integer totalPages;
    private Long totalElements;
    private Boolean isLastPage;
}
