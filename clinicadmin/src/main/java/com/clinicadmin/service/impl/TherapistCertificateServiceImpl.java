package com.clinicadmin.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.TherapistCertificateDTO;
import com.clinicadmin.entity.TherapistCertificate;
import com.clinicadmin.repository.TherapistCertificateRepository;
import com.clinicadmin.service.S3Service;
import com.clinicadmin.service.TherapistCertificateService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Service
public class TherapistCertificateServiceImpl
        implements TherapistCertificateService {

    @Autowired
    private TherapistCertificateRepository repository;

    @Autowired
    private S3Service s3Service;

    // CREATE
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "createCertificateFallback")
    public Response createCertificate(
            TherapistCertificateDTO dto) {

        Response response = new Response();

        // Store fileKey directly in DB — NOT signed URL
        // dto.getUpload() has fileKey from frontend
        // e.g. "certificates/uuid.jpg"
        TherapistCertificate entity = dtoToEntity(dto);

        repository.save(entity);

        response.setSuccess(true);
        response.setStatus(200);
        response.setMessage("Certificate created successfully");
        response.setData(entityToDto(entity));

        return response;
    }

    // GET ALL
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllCertificatesFallback")
    public Response getAllCertificates() {

        Response response = new Response();

        List<TherapistCertificateDTO> list =
                repository.findAll()
                        .stream()
                        .map(this::entityToDto)
                        .collect(Collectors.toList());

        response.setSuccess(true);
        response.setStatus(200);
        response.setMessage("Certificates fetched successfully");
        response.setData(list);

        return response;
    }

    // GET BY ID
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getCertificateByIdFallback")
    public Response getCertificateById(String id) {

        Response response = new Response();

        Optional<TherapistCertificate> optional =
                repository.findById(id);

        if (optional.isPresent()) {

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage("Certificate fetched successfully");
            response.setData(entityToDto(optional.get()));

        } else {

            response.setSuccess(false);
            response.setStatus(404);
            response.setMessage("Certificate not found");
            response.setData(null);
        }

        return response;
    }

    // GET BY CLINIC & BRANCH
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getCertificatesByClinicAndBranchFallback")
    public Response getCertificatesByClinicAndBranch(
            String clinicId,
            String branchId) {

        Response response = new Response();

        List<TherapistCertificateDTO> list =
                repository.findByClinicIdAndBranchId(
                                clinicId, branchId)
                        .stream()
                        .map(this::entityToDto)
                        .collect(Collectors.toList());

        response.setSuccess(true);
        response.setStatus(200);
        response.setMessage("Certificates fetched successfully");
        response.setData(list);

        return response;
    }

    // GET BY CLINIC + BRANCH + THERAPIST
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getCertificatesByClinicBranchAndTherapistFallback")
    public Response getCertificatesByClinicBranchAndTherapist(
            String clinicId,
            String branchId,
            String therapistId) {

        Response response = new Response();

        List<TherapistCertificateDTO> list =
                repository.findByClinicIdAndBranchIdAndTherapistId(
                        clinicId,
                        branchId,
                        therapistId)
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());

        response.setSuccess(true);
        response.setStatus(200);
        response.setMessage("Certificates fetched successfully");
        response.setData(list);

        return response;
    }



    // UPDATE
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updateCertificateFallback")
    public Response updateCertificate(
            String id,
            TherapistCertificateDTO dto) {

        Response response = new Response();

        Optional<TherapistCertificate> optional =
                repository.findById(id);

        if (optional.isPresent()) {

            TherapistCertificate entity = optional.get();

            entity.setClinicId(dto.getClinicId());
            entity.setBranchId(dto.getBranchId());
            entity.setCertificateName(dto.getCertificateName());
            entity.setIssueAuthority(dto.getIssueAuthority());

            // Only update upload if new fileKey was sent
            // Store fileKey directly — NOT signed URL
            if (dto.getUpload() != null
                    && !dto.getUpload().isBlank()) {
                entity.setUpload(
                    extractFileKey(dto.getUpload())
                );
            }
            // If null/blank → keep existing fileKey in DB

            repository.save(entity);

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage("Certificate updated successfully");
            response.setData(entityToDto(entity));

        } else {

            response.setSuccess(false);
            response.setStatus(404);
            response.setMessage("Certificate not found");
            response.setData(null);
        }

        return response;
    }

    // DELETE
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteCertificateFallback")
    public Response deleteCertificate(String id) {

        Response response = new Response();

        Optional<TherapistCertificate> optional =
                repository.findById(id);

        if (optional.isPresent()) {

            repository.deleteById(id);

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage("Certificate deleted successfully");
            response.setData(null);

        } else {

            response.setSuccess(false);
            response.setStatus(404);
            response.setMessage("Certificate not found");
            response.setData(null);
        }

        return response;
    }

    // ENTITY TO DTO
    private TherapistCertificateDTO entityToDto(
            TherapistCertificate entity) {

        TherapistCertificateDTO dto =
                new TherapistCertificateDTO();

        dto.setId(entity.getId());
        dto.setClinicId(entity.getClinicId());
        dto.setBranchId(entity.getBranchId());
        dto.setTherapistId(entity.getTherapistId());
        dto.setCertificateName(entity.getCertificateName());
        dto.setIssueAuthority(entity.getIssueAuthority());
        dto.setUploadDateTime(entity.getUploadDateTime());
        // DB has fileKey → generate fresh signed URL
        // for frontend on every request
        if (entity.getUpload() != null
                && !entity.getUpload().isBlank()) {
            String fileKey =
                extractFileKey(entity.getUpload());
            dto.setUpload(
                s3Service.generateSignedUrl(fileKey)
            );
        }

        return dto;
    }

    // DTO TO ENTITY
    private TherapistCertificate dtoToEntity(
            TherapistCertificateDTO dto) {

        TherapistCertificate entity =
                new TherapistCertificate();

        entity.setId(dto.getId());
        entity.setClinicId(dto.getClinicId());
        entity.setBranchId(dto.getBranchId());
        entity.setTherapistId(dto.getTherapistId());
        entity.setCertificateName(dto.getCertificateName());
        entity.setIssueAuthority(dto.getIssueAuthority());
        entity.setUploadDateTime(
                LocalDateTime.now());
        // Store only fileKey in DB
        // e.g. "certificates/uuid.jpg"
        entity.setUpload(
            extractFileKey(dto.getUpload())
        );

        return entity;
    }

    // ─────────────────────────────────────────────
    // Extract fileKey from full signed URL
    // Input:  "https://physiocare-prod-storage.s3.ap-south-1.amazonaws.com/certificates/uuid.png?X-Amz-..."
    // Output: "certificates/uuid.png"
    // If already a fileKey → returns as-is
    // ─────────────────────────────────────────────
    private String extractFileKey(String signedUrl) {

        if (signedUrl == null || signedUrl.isBlank()) {
            return null;
        }

        try {
            // Already a fileKey (not a full URL)
            if (!signedUrl.startsWith("http")) {
                return signedUrl;
            }

            // Remove base URL
            String withoutBase = signedUrl
                    .substring(signedUrl
                        .indexOf(".amazonaws.com/") + 15);

            // Remove query params "?X-Amz-..."
            return withoutBase.contains("?")
                    ? withoutBase.substring(
                        0, withoutBase.indexOf("?"))
                    : withoutBase;

        } catch (Exception e) {
            return signedUrl;
        }
    }


    // ================= RATE LIMIT FALLBACKS =================

    public Response createCertificateFallback(
            TherapistCertificateDTO dto,
            Exception ex) {
        return buildRateLimitResponse();
    }

    public Response getAllCertificatesFallback(
            Exception ex) {
        return buildRateLimitResponse();
    }

    public Response getCertificateByIdFallback(
            String id,
            Exception ex) {
        return buildRateLimitResponse();
    }

    public Response getCertificatesByClinicAndBranchFallback(
            String clinicId,
            String branchId,
            Exception ex) {
        return buildRateLimitResponse();
    }

    public Response getCertificatesByClinicBranchAndTherapistFallback(
            String clinicId,
            String branchId,
            String therapistId,
            Exception ex) {
        return buildRateLimitResponse();
    }

    public Response updateCertificateFallback(
            String id,
            TherapistCertificateDTO dto,
            Exception ex) {
        return buildRateLimitResponse();
    }

    public Response deleteCertificateFallback(
            String id,
            Exception ex) {
        return buildRateLimitResponse();
    }

    public Response buildRateLimitResponse() {
        Response response = new Response();
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again after some time.");
        response.setStatus(429);
        return response;
    }

}