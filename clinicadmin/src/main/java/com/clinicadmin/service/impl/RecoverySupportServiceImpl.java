package com.clinicadmin.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.RecoverySupportDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.RecoverySupport;
import com.clinicadmin.repository.RecoverySupportRepository;
import com.clinicadmin.service.RecoverySupportService;
import com.clinicadmin.service.S3Service;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RecoverySupportServiceImpl implements RecoverySupportService {

    @Autowired
    private RecoverySupportRepository repository;

    @Autowired
    private S3Service s3Service;

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "saveRecoverySupportFallback")
    public Response saveRecoverySupport(RecoverySupportDTO dto) {
        log.info("Saving recovery support clinicId={} name={}", dto.getClinicId(), dto.getName());

        Response response = new Response();

        RecoverySupport support = convertToEntity(dto);

        log.debug("Saving recovery support entity");
        repository.save(support);
        log.info("Recovery support saved successfully");

        response.setSuccess(true);
        response.setData(convertToDto(support));
        response.setMessage("Recovery support saved successfully");
        response.setStatus(HttpStatus.CREATED.value());

        return response;
    }

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllRecoverySupportsFallback")
    public Response getAllRecoverySupports() {
        log.info("Fetching all recovery supports");

        Response response = new Response();

        log.debug("Calling repository.findAll()");
        List<RecoverySupportDTO> data = repository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        response.setSuccess(true);
        response.setData(data);
        response.setMessage("Recovery support list fetched successfully");
        response.setStatus(HttpStatus.OK.value());

        return response;
    }

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getRecoverySupportByIdFallback")
    public Response getRecoverySupportById(String id) {
        log.info("Fetching recovery support id={}", id);

        Response response = new Response();

        Optional<RecoverySupport> optional = repository.findById(id);

        if (optional.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Recovery support not found");
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return response;
        }

        response.setSuccess(true);
        response.setData(convertToDto(optional.get()));
        response.setMessage("Recovery support fetched successfully");
        response.setStatus(HttpStatus.OK.value());

        return response;
    }

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updateRecoverySupportFallback")
    public Response updateRecoverySupport(String id, RecoverySupportDTO dto) {
        log.info("Updating recovery support id={}", id);

        Response response = new Response();

        Optional<RecoverySupport> optional = repository.findById(id);

        if (optional.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Recovery support not found");
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return response;
        }

        RecoverySupport support = optional.get();

        // update fields directly
        support.setClinicId(dto.getClinicId());
        support.setName(dto.getName());
        support.setDescription(dto.getDescription());
        support.setImage(dto.getImage());
        support.setCategory(dto.getCategory());

        repository.save(support);

        response.setSuccess(true);
        response.setData(convertToDto(support));
        response.setMessage("Recovery support updated successfully");
        response.setStatus(HttpStatus.OK.value());

        return response;
    }

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteRecoverySupportFallback")
    public Response deleteRecoverySupport(String id) {
        log.info("Deleting recovery support id={}", id);

        Response response = new Response();

        Optional<RecoverySupport> optional = repository.findById(id);

        if (optional.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Recovery support not found");
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return response;
        }

        log.debug("Deleting recovery support from repository id={}", id);
        repository.deleteById(id);
        log.info("Recovery support deleted successfully id={}", id);

        response.setSuccess(true);
        response.setMessage("Recovery support deleted successfully");
        response.setStatus(HttpStatus.OK.value());

        return response;
    }
    
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getRecoverySupportsByClinicIdFallback")
    public Response getRecoverySupportsByClinicId(String clinicId) {
        log.info("Fetching recovery supports clinicId={}", clinicId);

        Response response = new Response();

        List<RecoverySupportDTO> recoverySupports = repository.findByClinicId(clinicId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        response.setSuccess(true);
        response.setData(recoverySupports);
        response.setMessage("Recovery supports fetched successfully");
        response.setStatus(HttpStatus.OK.value());

        return response;
    }
    
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getRecoverySupportByClinicIdAndIdFallback")
    public  Response getRecoverySupportByClinicIdAndId(String clinicId, String id){
        log.info("Fetching recovery support clinicId={} id={}", clinicId, id);

        Response response = new Response();

        Optional<RecoverySupport> optional =
                repository.findByClinicIdAndId(clinicId, id);

        if (optional.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Recovery support not found");
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return response;
        }

        response.setSuccess(true);
        response.setData(convertToDto(optional.get()));
        response.setMessage("Recovery support fetched successfully");
        response.setStatus(HttpStatus.OK.value());

        return response;
    }
    
    private RecoverySupport convertToEntity(RecoverySupportDTO dto) {

        RecoverySupport support = new RecoverySupport();

        support.setClinicId(dto.getClinicId());
        support.setName(dto.getName());
        support.setDescription(dto.getDescription());
        support.setImage(dto.getImage());
        support.setCategory(dto.getCategory());

        return support;
    }

    private RecoverySupportDTO convertToDto(RecoverySupport support) {

        RecoverySupportDTO dto = new RecoverySupportDTO();

        dto.setId(support.getId());
        dto.setClinicId(support.getClinicId());
        dto.setName(support.getName());
        dto.setDescription(support.getDescription());
        dto.setCategory(support.getCategory());

        if (support.getImage() != null && !support.getImage().isBlank()) {
            log.debug("Generating signed URL for recovery support image id={}", support.getId());
            dto.setImage(s3Service.generateSignedUrl(support.getImage()));
        }

        return dto;
    }


    // ================= RATE LIMIT FALLBACKS =================

    public Response saveRecoverySupportFallback(RecoverySupportDTO dto, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public Response getAllRecoverySupportsFallback(Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public Response getRecoverySupportByIdFallback(String id, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public Response updateRecoverySupportFallback(
            String id,
            RecoverySupportDTO dto,
            Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public Response deleteRecoverySupportFallback(String id, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public Response getRecoverySupportsByClinicIdFallback(
            String clinicId,
            Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public Response getRecoverySupportByClinicIdAndIdFallback(
            String clinicId,
            String id,
            Exception ex) {
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