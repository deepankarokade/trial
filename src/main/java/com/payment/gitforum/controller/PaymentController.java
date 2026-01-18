package com.payment.gitforum.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.payment.gitforum.service.PaymentService;
import com.razorpay.RazorpayException;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    
    @Profile("test")
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrderTest(
            @RequestParam Long amount) throws RazorpayException {
        return ResponseEntity.ok(
            paymentService.createRazorpayOrder(amount, "INR")
        );
    }
    
    @PostMapping("/verify")
    
}
