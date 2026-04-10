package com.dermaCare.customerService.service;

import com.dermaCare.customerService.dto.PaymentDTO;
import com.dermaCare.customerService.util.Response;

public interface PaymentService {
    Response createPayment(PaymentDTO paymentDTO);
    Response getAllPayments();
}


