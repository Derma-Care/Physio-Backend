package com.chiselon.clinicadmin.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.chiselon.clinicadmin.dto.PatientInfoDTO;
import com.chiselon.clinicadmin.dto.PatientMessageDTO;
import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.service.EmailService;
import com.chiselon.clinicadmin.service.PatientMessageService;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientMessageServiceImpl implements PatientMessageService {

    @Autowired
    private EmailService emailService;

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "savePatientMessageFallback")
    public Response savePatientMessage(PatientMessageDTO dto) {

        log.info("Patient message request received clinicName={} branchName={} title={}",
                dto.getClinicName(),
                dto.getBranchName(),
                dto.getTitle());

        Response response = new Response();

        try {

            if (dto.getList() == null || dto.getList().isEmpty()) {

                log.warn("No patients available for email notification clinicName={} branchName={}",
                        dto.getClinicName(),
                        dto.getBranchName());

                response.setSuccess(false);
                response.setMessage("No patients found");
                response.setStatus(400);
                return response;
            }

            log.info("Sending emails to {} patients", dto.getList().size());

            for (PatientInfoDTO patient : dto.getList()) {

                if (patient.getPatientEmail() == null
                        || patient.getPatientEmail().isBlank()) {

                    log.warn("Skipping patient due to missing email patientName={}",
                            patient.getPatientName());
                    continue;
                }

                log.debug("Sending email to patientName={} email={}",
                        patient.getPatientName(),
                        patient.getPatientEmail());

                emailService.sendPatientEmail(
                        patient.getPatientEmail(),
                        patient.getPatientName(),
                        dto.getClinicName(),
                        dto.getBranchName(),
                        dto.getTitle(),
                        dto.getBody());

                log.info("Email sent successfully patientName={} email={}",
                        patient.getPatientName(),
                        patient.getPatientEmail());
            }

            response.setSuccess(true);
            response.setMessage("Emails sent successfully");
            response.setData(dto);
            response.setStatus(200);

            log.info("Patient email campaign completed successfully clinicName={} branchName={}",
                    dto.getClinicName(),
                    dto.getBranchName());

        } catch (Exception e) {

            log.error("Failed to send patient emails clinicName={} branchName={}",
                    dto.getClinicName(),
                    dto.getBranchName(),
                    e);

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }

        return response;
    }

    public Response savePatientMessageFallback(
            PatientMessageDTO dto,
            Exception ex) {

        log.error("Rate limiter triggered in savePatientMessage clinicName={} branchName={}",
                dto != null ? dto.getClinicName() : null,
                dto != null ? dto.getBranchName() : null,
                ex);

        return buildRateLimitResponse(ex);
    }

    public Response buildRateLimitResponse(Exception ex) {

        log.error("Returning rate limit response", ex);

        Response response = new Response();
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again after some time.");
        response.setStatus(429);
        return response;
    }
}
