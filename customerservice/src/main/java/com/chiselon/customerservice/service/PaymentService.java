package com.chiselon.customerservice.service;

import com.chiselon.customerservice.dto.PaymentDTO;
import com.chiselon.customerservice.util.Response;

public interface PaymentService {
    Response createPayment(PaymentDTO paymentDTO);
    Response getAllPayments();
}


