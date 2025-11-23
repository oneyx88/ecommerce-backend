package com.commerce.payment.service;

import com.commerce.payment.dto.PaymentRequest;
import com.commerce.payment.dto.PaymentResponse;
import com.commerce.payment.exceptions.ResourceNotFoundException;
import com.commerce.payment.kafka.event.PaymentCreatedEvent;
import com.commerce.payment.kafka.event.PaymentFailedEvent;
import com.commerce.payment.kafka.event.PaymentSucceededEvent;
import com.commerce.payment.kafka.config.TopicConstants;
import com.commerce.payment.model.Payment;
import com.commerce.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * @author Yixi Wan
 * @date 2025/11/3 17:14
 * @package com.commerce.payment.service
 * <p>
 * Description:
 */
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ModelMapper modelMapper;

    // 泛型 KafkaTemplate（可以发送任意类型事件）
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;


    // --------------------------------------------------------------------
    // 🧾 支付单创建
    // --------------------------------------------------------------------
    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        // 1️⃣ 构建 Payment 实体
        Payment payment = modelMapper.map(request, Payment.class);
        payment.setPaymentStatus("INITIATED");
        payment.setAmount(request.getTotalAmount());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        // 2️⃣ 保存数据库
        Payment saved = paymentRepository.save(payment);

        // 3️⃣ 注册事务提交后发送 PaymentCreatedEvent
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                saved.getOrderId(),
                saved.getPaymentId(),
                saved.getAmount(),
                saved.getCreatedAt()
        );
        registerPaymentEventAfterCommit(event, TopicConstants.TOPIC_PAYMENT_CREATED);

        log.info("✅ Payment created: paymentId={}, orderId={}", saved.getPaymentId(), saved.getOrderId());

        // 4️⃣ 返回响应
        return modelMapper.map(saved, PaymentResponse.class);
    }

    // --------------------------------------------------------------------
    // 💳 支付状态更新（成功 / 失败）
    // --------------------------------------------------------------------
    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(Long paymentId, String status, String message) {
        // 1️⃣ 查找支付记录
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));

        // 2️⃣ 更新状态
        payment.setPaymentStatus(status);
        payment.setPgResponseMessage(message);
        payment.setUpdatedAt(LocalDateTime.now());
        Payment updated = paymentRepository.save(payment);

        // 3️⃣ 根据状态发送对应事件
        switch (status.toUpperCase()) {
            case "SUCCESS":
                PaymentSucceededEvent successEvent = new PaymentSucceededEvent(
                        updated.getOrderId(),
                        updated.getPaymentId(),
                        updated.getAmount(),
                        updated.getPaymentMethod(),
                        LocalDateTime.now()
                );
                registerPaymentEventAfterCommit(successEvent, TopicConstants.TOPIC_PAYMENT_SUCCEEDED);
                break;

            case "FAILED":
                PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                        updated.getOrderId(),
                        updated.getPaymentId(),
                        message,
                        LocalDateTime.now()
                );
                registerPaymentEventAfterCommit(failedEvent, TopicConstants.TOPIC_PAYMENT_FAILED);
                break;

            default:
                log.warn("⚠️ Unknown payment status: {} for paymentId={}", status, paymentId);
        }

        log.info("💳 Payment status updated: paymentId={}, status={}", paymentId, status);

        // 4️⃣ 返回响应
        return modelMapper.map(updated, PaymentResponse.class);
    }

    // --------------------------------------------------------------------
    // 🔍 查询支付详情
    // --------------------------------------------------------------------
    @Override
    public PaymentResponse getPaymentByPaymentId(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));
        return modelMapper.map(payment, PaymentResponse.class);
    }

    // --------------------------------------------------------------------
    // 🧩 通用事件注册与发送方法
    // --------------------------------------------------------------------
    private void registerPaymentEventAfterCommit(Object event, String topic) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendPaymentMessage(topic, event);
            }
        });
    }

    private void sendPaymentMessage(String topic, Object event) {
        try {
            kafkaTemplate.send(topic, event);
            log.info("[Kafka] Event sent → topic={}, payload={}", topic, event);
        } catch (Exception e) {
            log.error("Failed to send event to topic=" + topic, e);
        }
    }
}