package com.payment.gitforum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payment.gitforum.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
}
