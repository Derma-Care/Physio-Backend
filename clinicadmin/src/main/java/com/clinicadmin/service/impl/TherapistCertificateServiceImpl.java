package com.clinicadmin.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.TherapistCertificateDTO;
import com.clinicadmin.entity.TherapistCertificate;
import com.clinicadmin.repository.TherapistCertificateRepository;
import com.clinicadmin.service.S3Service;
import com.clinicadmin.service.TherapistCertificateService;

@Service
public class TherapistCertificateServiceImpl
        implements TherapistCertificateService {

    @Autowired
    private TherapistCertificateRepository repository;

    @Autowired
    private S3Service s3Service;  // ← inject S3Service

    // CREATE
    @Override
    public Response createCertificate(
            TherapistCertificateDTO dto) {

        Response response = new Response();

        // If upload contains a fileKey (not base64),
        // convert it to a signed GET URL before saving
        if (dto.getUpload() != null
                && !dto.getUpload().isBlank()) {
            String signedUrl =
                s3Service.generateSignedUrl(
                    dto.getUpload());   // fileKey → signed URL
            dto.setUpload(signedUrl);
        }

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

    // UPDATE
    @Override
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

            // Only update upload if a new fileKey was sent
            if (dto.getUpload() != null
                    && !dto.getUpload().isBlank()) {
                String signedUrl =
                    s3Service.generateSignedUrl(
                        dto.getUpload());  // fileKey → signed URL
                entity.setUpload(signedUrl);
            }
            // If dto.upload is null/blank → keep existing upload in DB

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

        TherapistCertificateDTO dto = new TherapistCertificateDTO();

        dto.setId(entity.getId());
        dto.setClinicId(entity.getClinicId());
        dto.setBranchId(entity.getBranchId());
        dto.setTherapistId(entity.getTherapistId());
        dto.setCertificateName(entity.getCertificateName());
        dto.setIssueAuthority(entity.getIssueAuthority());
        dto.setUpload(entity.getUpload());

        return dto;
    }

    // DTO TO ENTITY
    private TherapistCertificate dtoToEntity(
            TherapistCertificateDTO dto) {

        TherapistCertificate entity = new TherapistCertificate();

        entity.setId(dto.getId());
        entity.setClinicId(dto.getClinicId());
        entity.setBranchId(dto.getBranchId());
        entity.setTherapistId(dto.getTherapistId());
        entity.setCertificateName(dto.getCertificateName());
        entity.setIssueAuthority(dto.getIssueAuthority());
        entity.setUpload(dto.getUpload());

        return entity;
    }
}