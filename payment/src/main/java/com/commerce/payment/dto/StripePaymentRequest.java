package com.commerce.payment.dto;

import lombok.Data;

import java.util.Map;

/**
 * @author Yixi Wan
 * @date 2025/11/19 22:35
 * @package com.commerce.payment.dto
 * <p>
 * Description:
 */
@Data
public class StripePaymentRequest {
    private Long amount;
    private String currency;
}
