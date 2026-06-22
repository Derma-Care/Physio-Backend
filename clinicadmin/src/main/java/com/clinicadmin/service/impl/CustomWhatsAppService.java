package com.clinicadmin.service.impl;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

import com.clinicadmin.dto.CustomNotificationRequest;
import com.clinicadmin.dto.CustomNotificationRequest.PatientEntry;

@Service
@Slf4j
public class CustomWhatsAppService {

    private final WebClient webClient;

    @Value("${whatsapp.auth-key}")
    private String authKey;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.custom-notification-template-id}")
    private String templateId;

    public CustomWhatsAppService(
            WebClient.Builder webClientBuilder,
            @Value("${whatsapp.base-url}") String baseUrl
    ) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    // =====================================================
    // PUBLIC API
    // =====================================================

    public void sendToAll(CustomNotificationRequest request) {

        if (request.getList() == null || request.getList().isEmpty()) {
            log.warn("CustomWhatsApp skipped — no patients in list");
            return;
        }

        for (PatientEntry patient : request.getList()) {
            try {
                sendToOne(patient, request);
            } catch (Exception ex) {
                log.error("CustomWhatsApp failed patientId={} error={}",
                        patient.getPatientId(), ex.getMessage(), ex);
            }
        }
    }

    // =====================================================
    // PER-PATIENT SEND
    // =====================================================

    private void sendToOne(PatientEntry patient, CustomNotificationRequest request) {

        String mobile = normalizeMobile(patient.getMobileNumber());

        if (!isValidMobile(mobile)) {
            log.warn("CustomWhatsApp skipped — invalid mobile patientId={}",
                    patient.getPatientId());
            return;
        }

        String variables = buildVariables(request);

        String response = webClient.get()
                .uri(uri -> uri
                        .path("/dev/whatsapp")
                        .queryParam("authorization", authKey)
                        .queryParam("message_id", templateId)
                        .queryParam("phone_number_id", phoneNumberId)
                        .queryParam("numbers", mobile)
                        .queryParam("variables_values", variables)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .retry(1)
                .block();

        log.info("CustomWhatsApp sent patientId={} mobile={} response={}",
                patient.getPatientId(), mobile, response);
    }

    // =====================================================
    // VARIABLE BUILDER
    // =====================================================

    private String buildVariables(CustomNotificationRequest request) {
        return String.join("|",
                safe(request.getTitle(), "Notification"),  // Header {{1}}
                safe(request.getBody(), ""),               // Body {{1}}
                safe(request.getClinicName(), "Clinic"),   // Body {{2}}
                safe(request.getBranchName(), "Branch")    // Body {{3}}
        );
    }

    // =====================================================
    // MOBILE UTILS
    // =====================================================

    private String normalizeMobile(String mobile) {
        return mobile == null ? null : mobile.replaceAll("\\D", "");
    }

    private boolean isValidMobile(String mobile) {
        return mobile != null && mobile.matches("^[6-9]\\d{9}$");
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }
}