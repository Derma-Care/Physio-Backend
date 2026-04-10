package com.dermaCare.customerService.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public  class GatewayResponse {
    private String razorpay_payment_id;
    private String razorpay_order_id;
    private String razorpay_signature;

  
}