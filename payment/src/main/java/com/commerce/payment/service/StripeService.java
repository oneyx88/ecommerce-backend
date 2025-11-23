package com.commerce.payment.service;

import com.commerce.payment.dto.StripePaymentRequest;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

/**
 * @author Yixi Wan
 * @date 2025/11/19 22:33
 * @package com.commerce.payment.service
 * <p>
 * Description:
 */
public interface StripeService {
    String createStripePaymentIntent(Long orderId) throws StripeException;
}
