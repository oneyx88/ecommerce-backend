package com.commerce.payment.service;

import com.commerce.payment.dto.StripePaymentRequest;
import com.commerce.payment.exceptions.ResourceNotFoundException;
import com.commerce.payment.model.Payment;
import com.commerce.payment.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @author Yixi Wan
 * @date 2025/11/19 22:36
 * @package com.commerce.payment.service
 * <p>
 * Description:
 */

// todo 向stripe添加更多信息email，user
@Service
@Transactional
public class StripeServiceImpl implements StripeService {

    @Value("${stripe.secret-key}")
    private String stripeApiKey;

    @PostConstruct
    public void init(){
        Stripe.apiKey = stripeApiKey;
    }

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public String createStripePaymentIntent(Long orderId) throws StripeException {

        // 1️⃣ 查 Payment（不更新 DB）
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        // 2️⃣ 创建 PaymentIntent
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(payment.getAmount().longValue()*100)
                .setCurrency(payment.getCurrency())
                .putMetadata("paymentId", payment.getPaymentId().toString())
                .putMetadata("orderId", payment.getOrderId().toString())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        return intent.getClientSecret();
    }


}
