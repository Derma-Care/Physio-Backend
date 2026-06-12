package com.dermacare.bookingService.service.Impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.dermacare.bookingService.dto.BookingRequset;
import com.dermacare.bookingService.dto.BranchDTO;
import com.dermacare.bookingService.feign.AdminServiceClient;
import com.dermacare.bookingService.util.ResponseStructure;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WhatsAppService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://www.fast2sms.com")
            .build();

    @Autowired
    private AdminServiceClient adminServiceClient;

    @Value("${whatsapp.auth-key}")
    private String authKey;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.message-id}")
    private String messageId;

    private static final String BOOKING_TEMPLATE =
            "appointment_confirmation";

    public void sendBookingConfirmation(BookingRequset booking) {

        try {

            // =====================================================
            // MOBILE NUMBER
            // =====================================================

            String mobile = booking.getPatientMobileNumber();

            if (mobile == null || mobile.trim().isEmpty()) {
                mobile = booking.getMobileNumber();
            }

            if (mobile == null || mobile.trim().isEmpty()) {

                log.warn(
                        "Mobile number not found for booking {}",
                        booking.getBookingId());

                return;
            }

            String normalizedMobile =
                    mobile.replaceAll("[^0-9]", "");

            if (normalizedMobile.startsWith("91")
                    && normalizedMobile.length() == 12) {

                normalizedMobile =
                        normalizedMobile.substring(2);
            }

            final String cleanMobile = normalizedMobile;

            // =====================================================
            // FETCH BRANCH DETAILS FROM ADMIN SERVICE
            // =====================================================

            BranchDTO branch = null;

            try {

                log.info(
                        "Fetching branch details for branchId={}",
                        booking.getBranchId());

                ResponseEntity<ResponseStructure<BranchDTO>> response =
                        adminServiceClient.getBranchById(
                                booking.getBranchId());

                if (response != null
                        && response.getBody() != null
                        && response.getBody().getData() != null) {

                    branch = response.getBody().getData();

                    log.info("Branch Details : {}", branch);

                } else {

                    log.warn(
                            "Branch details not found for branchId={}",
                            booking.getBranchId());
                }

            } catch (Exception e) {

                log.error(
                        "Unable to fetch branch details for branchId {} : {}",
                        booking.getBranchId(),
                        e.getMessage(),
                        e);
            }

            // =====================================================
            // DEFAULT VALUES
            // =====================================================

            String clinicName = "Clinic";
            String branchName = "";
            String whatsappNumber = "";
            String email = "";
            String locationUrl = "";

            if (branch != null) {

                clinicName = nullSafe(
                        branch.getHospitalName(),
                        "Clinic");

                branchName = nullSafe(
                        branch.getBranchName(),
                        "");

                whatsappNumber = nullSafe(
                        branch.getContactNumber(),
                        "");

                email = nullSafe(
                        branch.getEmail(),
                        "");

                if (branch.getLatitude() != null
                        && !branch.getLatitude().trim().isEmpty()
                        && branch.getLongitude() != null
                        && !branch.getLongitude().trim().isEmpty()) {

                	locationUrl =
                		    "https://www.google.com/maps/search/?api=1&query="
                		    + URLEncoder.encode(branch.getHospitalName()
                		    + " " + branch.getAddress(),
                		    StandardCharsets.UTF_8);
                	
//                	locationUrl =
//                		    "https://www.google.com/maps/search/?api=1&query="
//                		    + branch.getLatitude()
//                		    + ","
//                		    + branch.getLongitude();
                }

                log.info(
                        "Hospital={}, Branch={}, WhatsApp={}, Email={}",
                        clinicName,
                        branchName,
                        whatsappNumber,
                        email);
            }

            // =====================================================
            // TEMPLATE VARIABLES
            // =====================================================

            final String variables = String.join("|",

                    clinicName,                                  // {{1}}
                    nullSafe(booking.getName(), "Patient"),      // {{2}}
                    nullSafe(booking.getDoctorName(), "Doctor"), // {{3}}
                    nullSafe(booking.getServiceDate(), ""),      // {{4}}
                    nullSafe(booking.getServicetime(), ""),      // {{5}}
                    nullSafe(booking.getBookingId(), ""),        // {{6}}
                    branchName,                                  // {{7}}
                    whatsappNumber,                              // {{8}}
                    email,                                       // {{9}}
                    locationUrl                                  // {{10}}
            );

            log.info(
                    "Sending WhatsApp | Template={} | BookingId={} | Mobile={}",
                    BOOKING_TEMPLATE,
                    booking.getBookingId(),
                    cleanMobile);

            log.info(
                    "WhatsApp Variables={}",
                    variables);

            // =====================================================
            // FAST2SMS API
            // =====================================================

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/dev/whatsapp")
                            .queryParam("authorization", authKey)
                            .queryParam("message_id", messageId)
                            .queryParam("phone_number_id", phoneNumberId)
                            .queryParam("numbers", cleanMobile)
                            .queryParam("variables_values", variables)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info(
                    "WhatsApp booking confirmation sent successfully : {}",
                    response);

        } catch (Exception e) {

            log.error(
                    "WhatsApp booking confirmation failed : {}",
                    e.getMessage(),
                    e);

            throw new RuntimeException(
                    "WhatsApp booking confirmation failed : "
                            + e.getMessage());
        }
    }

    private String nullSafe(
            String value,
            String defaultValue) {

        return (value != null
                && !value.trim().isEmpty())
                ? value.trim()
                : defaultValue;
    }
}