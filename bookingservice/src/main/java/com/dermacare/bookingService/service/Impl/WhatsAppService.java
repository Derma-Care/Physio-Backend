package com.dermacare.bookingService.service.Impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.dermacare.bookingService.dto.BookingRequset;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WhatsAppService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://www.fast2sms.com")
            .build();

    @Value("${whatsapp.auth-key}")
    private String authKey;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.message-id}")
    private String messageId;

    // ✅ Template name — must match exactly in Fast2SMS dashboard
    private static final String BOOKING_TEMPLATE = "booking_confirmation";

    // =========================
    // SEND BOOKING CONFIRMATION
    // =========================
    public void sendBookingConfirmation(BookingRequset booking) {

        // Get mobile — patientMobileNumber first, fallback to mobileNumber
        String mobile = booking.getPatientMobileNumber();
        if (mobile == null || mobile.trim().isEmpty()) {
            mobile = booking.getMobileNumber();
        }
        if (mobile == null || mobile.trim().isEmpty()) {
            log.warn("No mobile number found, skipping WhatsApp for booking: {}",
                booking.getBookingId());
            return;
        }

        // Normalize to 10 digits
        String cleanMobile = mobile.replaceAll("[^0-9]", "");
        if (cleanMobile.startsWith("91") && cleanMobile.length() == 12) {
            cleanMobile = cleanMobile.substring(2);
        }

     // Build variables — order must match template
     // {{1}} = Patient Name 
     // {{2}} = Doctor Name 
     // {{3}} = Date 
     // {{4}} = Time 
     // {{5}} = Booking ID
     // {{6}} = Clinic Name
     String variables = String.join("|",
         nullSafe(booking.getName(),        "Patient"),
         nullSafe(booking.getDoctorName(),  "Doctor"),
         nullSafe(booking.getServiceDate(), ""),
         nullSafe(booking.getServicetime(), ""),
         nullSafe(booking.getBookingId(),   ""),
         nullSafe(booking.getClinicName(),  "Clinic")
     );

        final String finalMobile    = cleanMobile;
        final String finalVariables = variables;

        log.info("Sending WhatsApp booking confirmation | template: {} | to: {} | variables: {}",
            BOOKING_TEMPLATE, finalMobile, finalVariables);

        try {
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/dev/whatsapp")
                    .queryParam("authorization",    authKey)
                    .queryParam("message_id",       messageId)
                    .queryParam("phone_number_id",  phoneNumberId)
                    .queryParam("numbers",          finalMobile)
                    .queryParam("variables_values", finalVariables)
                    .build())
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .map(body -> new RuntimeException("Fast2SMS error: " + body))
                )
                .bodyToMono(String.class)
                .block();

            log.info("WhatsApp booking confirmation sent successfully: {}", response);

        } catch (Exception e) {
            log.error("WhatsApp booking confirmation failed: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // =========================
    // NULL SAFE HELPER
    // =========================
    private String nullSafe(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }
}
