package com.commerce.payment.controller;

import com.commerce.payment.model.Payment;
import com.commerce.payment.repository.PaymentRepository;
import com.commerce.payment.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * @author Yixi Wan
 * @date 2025/11/20 12:10
 * @package com.commerce.payment.controller
 * <p>
 * Description:
 */
@RestController
@RequestMapping("/api/v1/payments/stripe/webhook")
public class StripeWebhookController {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // 1️⃣ 支付成功
        if ("payment_intent.succeeded".equals(event.getType())) {

            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            Long paymentId = Long.valueOf(intent.getMetadata().get("paymentId"));

            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));

            // 更新本地支付信息
            payment.setPgPaymentId(intent.getId());
            payment.setPgStatus("SUCCESS");
            payment.setPgResponseMessage("Stripe Payment Succeeded");
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // 调用你的业务逻辑 → Kafka 更新订单
            paymentService.updatePaymentStatus(paymentId, "SUCCESS", "Stripe Success");
        }

        // 2️⃣ 支付失败
        if ("payment_intent.payment_failed".equals(event.getType())) {

            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            Long paymentId = Long.valueOf(intent.getMetadata().get("paymentId"));

            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));

            payment.setPgPaymentId(intent.getId());
            payment.setPgStatus("FAILED");
            payment.setPgResponseMessage("Stripe Payment Failed");
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            paymentService.updatePaymentStatus(paymentId, "FAILED", "Stripe Failed");
        }

        return ResponseEntity.ok("success");
    }
}

