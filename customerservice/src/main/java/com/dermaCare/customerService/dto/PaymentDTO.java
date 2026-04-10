package com.dermaCare.customerService.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDTO {
    private String userId;
    private String doctorId;
    private String clinicId;
    private String bookingId;
    private double amount;
    private String paymentMethod;
    private String paymentStatus;
    private String currency;
    private String userEmail;
    private String userMobile;
    private String billingName;
    private String billingAddress;
    private GatewayResponseDTO gatewayResponse;

   
}

