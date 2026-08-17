package com.chiselon.clinicadmin.service.impl;

import com.chiselon.clinicadmin.dto.PrivacyPolicyDTO;
import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.entity.PrivacyPolicy;
import com.chiselon.clinicadmin.repository.PrivacyPolicyRepository;
import com.chiselon.clinicadmin.service.PrivacyPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PrivacyPolicyServiceImpl implements PrivacyPolicyService {

    @Autowired
    private PrivacyPolicyRepository repository;

    // Create / Save new policy
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "createPolicyFallback")
    public Response createPolicy(PrivacyPolicyDTO dto) {
        log.info("Creating privacy policy clinicId={}", dto.getClinicId());
        PrivacyPolicy entity = toEntity(dto);
        log.debug("Saving privacy policy");
        PrivacyPolicy saved = repository.save(entity);
        log.info("Privacy policy created successfully id={}", saved.getId());

        return Response.builder()
                .success(true)
                .data(toDTO(saved))
                .message("Policy created successfully")
                .status(HttpStatus.OK.value())
                .build();
    }

    // Read all policies
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllPoliciesFallback")
    public Response getAllPolicies() {
        log.info("Fetching all privacy policies");
        log.debug("Calling repository.findAll()");
        List<PrivacyPolicyDTO> dtos = repository.findAll()
                .stream()
                .map(this::toDTO) // Helper converts entity → DTO
                .collect(Collectors.toList());

        return Response.builder()
                .success(true)
                .data(dtos)
                .message("Policies retrieved successfully")
                .status(HttpStatus.OK.value())
                .build();
    }

    // Read single policy by ID
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getPolicyByIdFallback")
    public Response getPolicyById(String id) {
        log.info("Fetching privacy policy id={}", id);
        return repository.findById(id)
                .map(policy -> Response.builder()
                        .success(true)
                        .data(toDTO(policy)) // Helper converts entity → DTO
                        .message("Policy retrieved successfully")
                        .status(HttpStatus.OK.value())
                        .build())
                .orElse(Response.builder()
                        .success(false)
                        .message("Policy not found with id: " + id)
                        .status(HttpStatus.NOT_FOUND.value())
                        .build());
    }
    
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getPoliciesByClinicIdFallback")
    public Response getPoliciesByClinicId(String clinicId) {
        log.info("Fetching privacy policies clinicId={}", clinicId);
        Response response = new Response();
        log.debug("Fetching policies by clinicId={}", clinicId);
        List<PrivacyPolicyDTO> policies = repository.findByClinicId(clinicId);
        log.info("Fetched {} policies", policies.size());
        response.setSuccess(true);
        response.setData(policies);
        response.setMessage(policies.isEmpty() 
            ? "No privacy policies found for clinicId: " + clinicId
            : "Policies fetched successfully");
        response.setStatus(200);
        return response;
    }


    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updatePolicyFallback")
    public Response updatePolicy(PrivacyPolicyDTO dto) {
        log.info("Updating privacy policy id={}", dto.getId());
        if (dto.getId() == null) {
            return Response.builder()
                    .success(false)
                    .message("Policy ID is missing")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        PrivacyPolicy existingPolicy = repository.findById(dto.getId()).orElse(null);

        if (existingPolicy == null) {
            return Response.builder()
                    .success(false)
                    .message("Policy not found with ID: " + dto.getId())
                    .status(HttpStatus.NOT_FOUND.value())
                    .build();
        }

        if (dto.getPrivacyPolicy() != null && !dto.getPrivacyPolicy().isEmpty()) {
            existingPolicy.setPrivacyPolicy(dto.getPrivacyPolicy());
        } else {
            return Response.builder()
                    .success(false)
                    .message("No privacyPolicy provided to update")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        // Save updated entity
        log.debug("Saving updated privacy policy id={}", dto.getId());
        PrivacyPolicy updated = repository.save(existingPolicy);
        log.info("Privacy policy updated successfully id={}", updated.getId());

        return Response.builder()
                .success(true)
                .data(toDTO(updated))
                .message("Policy updated successfully")
                .status(HttpStatus.OK.value())
                .build();
    }



    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deletePolicyFallback")
    public Response deletePolicy(String id) {
        log.info("Deleting privacy policy id={}", id);
        if (repository.existsById(id)) {
            log.debug("Deleting privacy policy from repository id={}", id);
            repository.deleteById(id);
            log.info("Privacy policy deleted successfully id={}", id);
            return Response.builder()
                    .success(true)
                    .message("Policy deleted successfully")
                    .status(HttpStatus.OK.value())
                    .build();
        } else {
            return Response.builder()
                    .success(false)
                    .message("Policy not found with id: " + id)
                    .status(HttpStatus.NOT_FOUND.value())
                    .build();
        }
    }
    private PrivacyPolicyDTO toDTO(PrivacyPolicy entity) {
        if (entity == null) return null;
        return new PrivacyPolicyDTO(entity.getId().toString(), entity.getClinicId(), entity.getPrivacyPolicy());
    }


    private PrivacyPolicy toEntity(PrivacyPolicyDTO dto) {
        if (dto == null) return null;
        return new PrivacyPolicy(dto.getId(),dto.getClinicId() ,dto.getPrivacyPolicy());
             }


    // ================= RATE LIMIT FALLBACKS =================

    public Response createPolicyFallback(PrivacyPolicyDTO dto, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public Response getAllPoliciesFallback(Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public Response getPolicyByIdFallback(String id, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public Response getPoliciesByClinicIdFallback(String clinicId, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public Response updatePolicyFallback(PrivacyPolicyDTO dto, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public Response deletePolicyFallback(String id, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
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