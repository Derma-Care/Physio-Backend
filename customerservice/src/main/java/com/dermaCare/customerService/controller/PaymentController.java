package com.dermaCare.customerService.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dermaCare.customerService.dto.PaymentDTO;
import com.dermaCare.customerService.service.PaymentService;
import com.dermaCare.customerService.util.Response;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/addpayment")
    public ResponseEntity<Response> createPayment(@RequestBody PaymentDTO paymentDTO) {
        Response response = paymentService.createPayment(paymentDTO);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/getallpayments")
    public ResponseEntity<Response> getAllPayments() {
        Response response = paymentService.getAllPayments();
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}

