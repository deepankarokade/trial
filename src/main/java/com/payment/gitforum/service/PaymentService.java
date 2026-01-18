package com.payment.gitforum.service;

import org.springframework.stereotype.Service;

import com.payment.gitforum.entity.Payment;
import com.payment.gitforum.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

@Service
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // Dummy Payment
    public Payment createPayment(Long amount, String currency) {
        Payment payment = new Payment();

        payment.setAmount(amount);
        payment.setCurrency(currency);

        return paymentRepository.save(payment);
    }

    // Create Razorpay Order
    public Map<String, Object> createRazorpayOrder(Long amount, String currency) throws RazorpayException {

        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment = paymentRepository.save(payment);

        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount);
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", "rcpt_" + payment.getId());

        Order order = razorpayClient.orders.create(orderRequest);

        payment.setRazorpayOrderId(order.get("id"));
        paymentRepository.save(payment);

        Map<String, Object> response = new HashMap();
        response.put("paymentId", payment.getId());
        response.put("razorpayOrderId", order.get("id"));
        response.put("amount", payment.getAmount());
        response.put("currency", payment.getCurrency());
        response.put("key", keyId);

        return response;
    }

    public boolean verifyPaymentSignature(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {

        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            String generatedSignature = Base64.getEncoder().encodeToString(hash);

            return generatedSignature.equals(razorpaySignature);

        } catch (Exception e) {
            return false;
        }
    }

    public void markPaymentStatus(
            String razorpayOrderId,
            String razorpayPaymentId) {

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus("SUCCESS");

        paymentRepository.save(payment);
    }
}

// Payment service working