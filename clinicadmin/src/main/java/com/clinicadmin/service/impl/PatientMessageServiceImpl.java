package com.clinicadmin.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.PatientInfoDTO;
import com.clinicadmin.dto.PatientMessageDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.service.EmailService;
import com.clinicadmin.service.PatientMessageService;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Service
public class PatientMessageServiceImpl implements PatientMessageService {

    @Autowired
    private EmailService emailService;

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "savePatientMessageFallback")
    public Response savePatientMessage(PatientMessageDTO dto) {

        Response response = new Response();

        try {

            if (dto.getList() != null && !dto.getList().isEmpty()) {

                for (PatientInfoDTO patient : dto.getList()) {

                    if (patient.getPatientEmail() != null
                            && !patient.getPatientEmail().isBlank()) {

                        emailService.sendPatientEmail(
                                patient.getPatientEmail(),
                                patient.getPatientName(),
                                dto.getClinicName(),
                                dto.getBranchName(),
                                dto.getTitle(),
                                dto.getBody());
                    }
                }
            }

            response.setSuccess(true);
            response.setMessage("Emails sent successfully");
            response.setData(dto);
            response.setStatus(200);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }

        return response;
    }

    public Response savePatientMessageFallback(
            PatientMessageDTO dto,
            Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response buildRateLimitResponse(Exception ex) {
        Response response = new Response();
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again after some time.");
        response.setStatus(429);
        return response;
    }
}
