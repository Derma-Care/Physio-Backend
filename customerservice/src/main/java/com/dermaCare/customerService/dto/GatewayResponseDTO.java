package com.dermaCare.customerService.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GatewayResponseDTO {
    private String razorpay_payment_id;
    private String razorpay_order_id;
    private String razorpay_signature;

    public boolean isEmpty() {
        return (razorpay_payment_id == null || razorpay_payment_id.isEmpty()) &&
               (razorpay_order_id == null || razorpay_order_id.isEmpty()) &&
               (razorpay_signature == null || razorpay_signature.isEmpty());
    }
}

