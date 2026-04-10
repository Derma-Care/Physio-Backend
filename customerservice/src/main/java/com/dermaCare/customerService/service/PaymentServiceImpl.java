package com.dermaCare.customerService.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.dermaCare.customerService.dto.*;
import com.dermaCare.customerService.entity.GatewayResponse;
import com.dermaCare.customerService.entity.Payment;
import com.dermaCare.customerService.feignClient.AdminFeign;
import com.dermaCare.customerService.feignClient.BookingFeign;
import com.dermaCare.customerService.feignClient.ClinicAdminFeign;
import com.dermaCare.customerService.repository.CustomerRepository;
import com.dermaCare.customerService.repository.PaymentRepository;
import com.dermaCare.customerService.util.Response;
import com.dermaCare.customerService.util.ResponseStructure;
import feign.FeignException;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ClinicAdminFeign clinicAdminFeignClient;

    @Autowired
    private AdminFeign adminFeign;

    @Autowired
    private BookingFeign bookingFeignClient;

    @Autowired
    private CustomerRepository customerRepository;

    private final Random random = new Random();

    @Override
    public Response createPayment(PaymentDTO paymentDTO) {
        String customerId = paymentDTO.getUserId();
        if (customerId == null || customerId.isEmpty()) {
            return new Response("Invalid userId (customerId missing)", 400, false, null);
        }

        if (!customerRepository.existsByCustomerId(customerId)) {
            return new Response("Customer not found", 404, false, null);
        }

        // Step 1: Get Doctor and Clinic ID from ClinicAdmin
        String doctorId;
        String clinicId;
        try {
            ResponseEntity<Response> doctorResponseEntity = clinicAdminFeignClient.getDoctorById(paymentDTO.getDoctorId());
            Response doctorResponse = doctorResponseEntity.getBody();

            if (doctorResponse == null || doctorResponse.getData() == null) {
                return new Response("Doctor not found in ClinicAdmin Service", 404, false, null);
            }

            Object doctorData = doctorResponse.getData();

            if (doctorData instanceof DoctorsDTO dto) {
                doctorId = dto.getDoctorId();
                clinicId = dto.getHospitalId();
            } else if (doctorData instanceof Map<?, ?> dataMap) {
                doctorId = dataMap.get("doctorId") != null ? dataMap.get("doctorId").toString() : null;
                clinicId = dataMap.get("hospitalId") != null ? dataMap.get("hospitalId").toString() : null;
            } else {
                return new Response("Invalid doctor data structure from ClinicAdmin", 500, false, null);
            }

        } catch (FeignException e) {
            if (e.status() == 404) {
                return new Response("Doctor not found with ID: " + paymentDTO.getDoctorId(), 404, false, null);
            }
            if (e.status() == 400 || (e.status() == 500 && e.getMessage().toLowerCase().contains("invalid doctor id"))) {
                return new Response("Invalid Doctor ID: Please provide a valid doctor ID", 400, false, null);
            }
            return new Response(e.getMessage(), e.status(), false, null);
        }

        // Step 2: Validate Clinic from Admin Service
        try {
            Response clinicResponse = adminFeign.getClinicById(clinicId);
            if (clinicResponse == null || !clinicResponse.isSuccess()) {
                return new Response("Clinic validation failed via AdminService", 404, false, null);
            }
        } catch (FeignException e) {
            return new Response("Clinic validation failed via AdminService: " + e.getMessage(), e.status(), false, null);
        }

        // Step 3: Validate Booking from Booking Service
        String bookingId = paymentDTO.getBookingId();
        if (bookingId == null || bookingId.isEmpty()) {
            return new Response("Booking ID is required", 400, false, null);
        }

        try {
            ResponseEntity<ResponseStructure<BookingResponse>> bookingResponseEntity =
                bookingFeignClient.getBookedService(bookingId);
            ResponseStructure<BookingResponse> bookingResponse = bookingResponseEntity.getBody();

            if (bookingResponse == null || bookingResponse.getData() == null) {
                return new Response("Booking not found with ID: " + bookingId, 404, false, null);
            }

        } catch (FeignException e) {
            if (e.status() == 404) {
                return new Response("Booking not found with ID: " + bookingId, 404, false, null);
            }
            if (e.status() == 400 || (e.status() == 500 && e.getMessage().toLowerCase().contains("invalid booking id"))) {
                return new Response("Invalid Booking ID: Please provide a valid booking ID", 400, false, null);
            }
            return new Response("Booking Service error: " + e.getMessage(), e.status(), false, null);
        }

        // Step 4: Save Payment
        Payment payment = new Payment();
        payment.setTransactionId(generateTransactionId());
        payment.setTimestamp(LocalDateTime.now());
        payment.setUserId(customerId);
        payment.setDoctorId(doctorId);
        payment.setClinicId(clinicId);
        payment.setBookingId(bookingId);
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentMethod(paymentDTO.getPaymentMethod());
        payment.setPaymentStatus(paymentDTO.getPaymentStatus());
        payment.setCurrency(paymentDTO.getCurrency());
        payment.setUserEmail(paymentDTO.getUserEmail());
        payment.setUserMobile(paymentDTO.getUserMobile());
        payment.setBillingName(paymentDTO.getBillingName());
        payment.setBillingAddress(paymentDTO.getBillingAddress());

        if (paymentDTO.getGatewayResponse() != null && !paymentDTO.getGatewayResponse().isEmpty()) {
            payment.setGatewayResponse(mapToEntity(paymentDTO.getGatewayResponse()));
        } else {
            payment.setGatewayResponse(null);
        }

        Payment saved = paymentRepository.save(payment);
        return new Response("Payment created successfully", 201, true, saved);
    }

    @Override
    public Response getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();

        List<PaymentDTO> dtoList = payments.stream().map(payment -> {
            PaymentDTO dto = new PaymentDTO();
//            dto.setTransactionId(payment.getTransactionId());
            dto.setUserId(payment.getUserId());
            dto.setDoctorId(payment.getDoctorId());
            dto.setClinicId(payment.getClinicId());
            dto.setBookingId(payment.getBookingId());
            dto.setAmount(payment.getAmount());
            dto.setPaymentMethod(payment.getPaymentMethod());
            dto.setPaymentStatus(payment.getPaymentStatus());
            dto.setCurrency(payment.getCurrency());
            dto.setUserEmail(payment.getUserEmail());
            dto.setUserMobile(payment.getUserMobile());
            dto.setBillingName(payment.getBillingName());
            dto.setBillingAddress(payment.getBillingAddress());
//            dto.setTimestamp(payment.getTimestamp());

            if (payment.getGatewayResponse() != null) {
                GatewayResponseDTO gatewayDto = new GatewayResponseDTO();
                gatewayDto.setRazorpay_payment_id(payment.getGatewayResponse().getRazorpay_payment_id());
                gatewayDto.setRazorpay_order_id(payment.getGatewayResponse().getRazorpay_order_id());
                gatewayDto.setRazorpay_signature(payment.getGatewayResponse().getRazorpay_signature());
                dto.setGatewayResponse(gatewayDto);
            }

            return dto;
        }).toList();

        return new Response("Payments fetched successfully", 200, true, dtoList);
    }

    private String generateTransactionId() {
        StringBuilder sb = new StringBuilder("TNX");
        for (int i = 0; i < 5; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private GatewayResponse mapToEntity(GatewayResponseDTO dto) {
        GatewayResponse entity = new GatewayResponse();
        entity.setRazorpay_payment_id(dto.getRazorpay_payment_id());
        entity.setRazorpay_order_id(dto.getRazorpay_order_id());
        entity.setRazorpay_signature(dto.getRazorpay_signature());
        return entity;
    }
}