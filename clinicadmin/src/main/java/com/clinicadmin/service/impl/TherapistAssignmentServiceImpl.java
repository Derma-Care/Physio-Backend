package com.clinicadmin.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.TherapistAssignmentDTO;
import com.clinicadmin.entity.TherapistAssignment;
import com.clinicadmin.repository.TherapistAssignmentRepository;
import com.clinicadmin.service.TherapistAssignmentService;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TherapistAssignmentServiceImpl implements TherapistAssignmentService {

    @Autowired
    private TherapistAssignmentRepository repository;

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "assignTherapistFallback")
    public Response assignTherapist(TherapistAssignmentDTO dto) {

        log.info("Assign therapist request clinicId={} branchId={} therapistRecordId={}",
                dto.getClinicId(), dto.getBranchId(), dto.getTherapistRecordId());

        Response response = new Response();

        try {

            TherapistAssignment assignment = new TherapistAssignment();

            assignment.setClinicId(dto.getClinicId());
            assignment.setBranchId(dto.getBranchId());
            assignment.setTherapistRecordId(dto.getTherapistRecordId());
            assignment.setAssignTherapistId(dto.getAssignTherapistId());
            assignment.setAssignTherapistName(dto.getAssignTherapistName());
            assignment.setAssignedTherapistId(dto.getAssignedTherapistId());
            assignment.setAssignedTherapistName(dto.getAssignedTherapistName());
            assignment.setServices(dto.getServices());
            assignment.setAssignedStatus("true");
            assignment.setAssignedTo(false);

            log.debug("Saving therapist assignment");

            TherapistAssignment savedAssignment = repository.save(assignment);

            log.info("Therapist assigned successfully therapistRecordId={}",
                    savedAssignment.getTherapistRecordId());

            response.setSuccess(true);
            response.setData(convertToDto(savedAssignment));
            response.setMessage("Therapist assigned successfully");
            response.setStatus(HttpStatus.OK.value());

        } catch (Exception e) {

            log.error("Failed to assign therapist", e);

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAssignedTherapistDetailsFallback")
    public Response getAssignedTherapistDetails(String therapistRecordId) {

        log.info("Fetching assigned therapist details therapistRecordId={}", therapistRecordId);

        Response response = new Response();

        try {

            log.debug("Looking up therapist assignment");

            Optional<TherapistAssignment> optional =
                    repository.findByTherapistRecordId(therapistRecordId);

            if (optional.isEmpty()) {

                log.warn("Assignment not found therapistRecordId={}", therapistRecordId);

                response.setSuccess(false);
                response.setMessage("Assignment not found");
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return response;
            }

            response.setSuccess(true);
            response.setData(convertToDto(optional.get()));
            response.setMessage("Assignment fetched successfully");
            response.setStatus(HttpStatus.OK.value());

        } catch (Exception e) {

            log.error("Failed to fetch therapist assignment", e);

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updateAssignedStatusFallback")
    public Response updateAssignedStatus(
            String therapistRecordId,
            TherapistAssignmentDTO dto) {

        log.info("Updating assigned status therapistRecordId={} status={}",
                therapistRecordId, dto.getAssignedStatus());

        Response response = new Response();

        try {

            Optional<TherapistAssignment> optional =
                    repository.findByTherapistRecordId(therapistRecordId);

            if (optional.isEmpty()) {

                log.warn("Assignment not found therapistRecordId={}", therapistRecordId);

                response.setSuccess(false);
                response.setMessage("Assignment not found");
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return response;
            }

            TherapistAssignment assignment = optional.get();

            assignment.setAssignedStatus(dto.getAssignedStatus());

            log.debug("Saving updated assignment status");

            repository.save(assignment);

            log.info("Assigned status updated successfully therapistRecordId={}",
                    therapistRecordId);

            response.setSuccess(true);
            response.setData(convertToDto(assignment));
            response.setMessage("Assigned status updated successfully");
            response.setStatus(HttpStatus.OK.value());

        } catch (Exception e) {

            log.error("Failed to update assigned status", e);

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }

    private TherapistAssignmentDTO convertToDto(TherapistAssignment assignment) {
        TherapistAssignmentDTO dto = new TherapistAssignmentDTO();
        dto.setId(assignment.getId());
        dto.setClinicId(assignment.getClinicId());
        dto.setBranchId(assignment.getBranchId());
        dto.setTherapistRecordId(assignment.getTherapistRecordId());
        dto.setAssignTherapistId(assignment.getAssignTherapistId());
        dto.setAssignTherapistName(assignment.getAssignTherapistName());
        dto.setAssignedTherapistId(assignment.getAssignedTherapistId());
        dto.setAssignedTherapistName(assignment.getAssignedTherapistName());
        dto.setServices(assignment.getServices());
        dto.setAssignedStatus(assignment.getAssignedStatus());
        return dto;
    }

    // FALLBACK METHODS

    public Response assignTherapistFallback(
            TherapistAssignmentDTO dto,
            Exception ex) {

        log.error("Rate limiter triggered in assignTherapist", ex);
        return buildRateLimitResponse();
    }

    public Response getAssignedTherapistDetailsFallback(
            String therapistRecordId,
            Exception ex) {

        log.error("Rate limiter triggered in getAssignedTherapistDetails therapistRecordId={}",
                therapistRecordId, ex);
        return buildRateLimitResponse();
    }

    public Response updateAssignedStatusFallback(
            String therapistRecordId,
            TherapistAssignmentDTO dto,
            Exception ex) {

        log.error("Rate limiter triggered in updateAssignedStatus therapistRecordId={}",
                therapistRecordId, ex);
        return buildRateLimitResponse();
    }

    private Response buildRateLimitResponse() {

        Response response = new Response();
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again after some time.");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        return response;
    }
}
