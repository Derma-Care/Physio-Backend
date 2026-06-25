package com.clinicadmin.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.PatientEmailDTO;
import com.clinicadmin.service.EmailService;
import com.clinicadmin.service.PatientEmailService;
import com.clinicadmin.service.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientEmailServiceImpl
        implements PatientEmailService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PatientEmailServiceImpl.class);

    private final EmailService emailService;

    private final S3Service s3Service;

    @Override
    public void sendPatientEmail(
            PatientEmailDTO dto) {

        try {

            if (dto == null) {

                log.warn(
                        "Patient email request is empty");

                return;
            }

            if (dto.getPatientMail() == null
                    || dto.getPatientMail().isBlank()) {

                log.warn(
                        "Patient email is required");

                return;
            }

            if (dto.getPatientName() == null
                    || dto.getPatientName().isBlank()) {

                log.warn(
                        "Patient name is required");

                return;
            }

            if (dto.getPdfFile() == null
                    || dto.getPdfFile().isBlank()) {

                log.warn(
                        "PDF file is required");

                return;
            }

            String title =
                    dto.getTitle() == null
                    || dto.getTitle().isBlank()

                    ? "Patient Document"

                    : dto.getTitle();

            String body = """
                    Dear %s,

                    Please find the attached PDF document.

                    Thank you.

                    Regards,
                    CCMS Team
                    """
                    .formatted(
                            dto.getPatientName());

            // dto.getPdfFile() contains the S3 fileKey
            String signedUrl =
                    s3Service.generateSignedUrl(
                            dto.getPdfFile());

            // Send email with PDF attachment
            emailService.sendPatientPdfEmail(

                    dto.getPatientMail(),

                    dto.getPatientName(),

                    title,

                    body,

                    signedUrl);

            log.info(
                    "Patient PDF email sent successfully to {}",
                    dto.getPatientMail());

        } catch (Exception e) {

            log.error(
                    "Error while sending patient PDF email : {}",
                    e.getMessage(),
                    e);
        }
    }
}