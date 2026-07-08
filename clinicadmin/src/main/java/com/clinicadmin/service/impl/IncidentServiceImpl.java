package com.clinicadmin.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.IncidentDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.Incident;
import com.clinicadmin.enumclasses.IncidentStatus;
import com.clinicadmin.repository.IncidentRepository;
import com.clinicadmin.service.IncidentService;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IncidentServiceImpl implements IncidentService {
	@Autowired
	private IncidentRepository incidentRepository;

	@Override
	 @Secured("ROLE_CLINICADMIN")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "createIncidentFallback")
	public Response createIncident(IncidentDTO dto) {
		log.info("Creating incident title={} raisedBy={}", dto.getTitle(), dto.getRaisedBy());
		Response response = new Response();
		if (dto.getTitle() == null || dto.getTitle().isEmpty()) {
			response.setSuccess(false);
			response.setMessage("Title is required");
			response.setStatus(400);
			return response;
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy h:mma");

		String now = LocalDateTime.now().format(formatter);
		Incident incident = Incident.builder().title(dto.getTitle()).description(dto.getDescription())
				.status(IncidentStatus.NEW).raisedBy(dto.getRaisedBy()).assignedTo(dto.getAssignedTo())
				.priority(dto.getPriority()).createdAt(now).updatedAt(now).build();
		log.debug("Saving incident to repository");
		incident = incidentRepository.save(incident);
		log.info("Incident created successfully id={}", incident.getId());
		IncidentDTO covetedDTO = convertToDTO(incident);

		response.setSuccess(true);
		response.setData(covetedDTO);
		response.setMessage("Incident created successfully");
		response.setStatus(200);
		return response;
	}
	@Override
	 @Secured("ROLE_CLINICADMIN")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllIncidentsFallback")
	public Response getAllIncidents() {
		log.info("Fetching all incidents");
	    Response response = new Response();
	    try {
	        log.debug("Calling incidentRepository.findAll()");
	        List<Incident> incidents = incidentRepository.findAll();
	        log.info("Fetched {} incidents", incidents.size());
	        if (incidents.isEmpty()) {
	            response.setSuccess(true);
	            response.setData(Collections.emptyList());
	            response.setMessage("No incidents found");
	            response.setStatus(200);
	            return response;
	        }

	        List<IncidentDTO> dtos = incidents.stream()
	                .map(this::convertToDTO)
	                .collect(Collectors.toList());

	        response.setSuccess(true);
	        response.setData(dtos);
	        response.setMessage("Incidents fetched successfully"); // ✅ fixed
	        response.setStatus(200);
	        return response;

	    } catch (Exception e) {
	        log.error("Failed while fetching incidents", e);
	        response.setSuccess(false);
	        response.setMessage("Error occurred while getting list of incidents: " + e.getMessage());
	        response.setStatus(500);
	        return response;
	    }
	}


	@Override
	 @Secured("ROLE_CLINICADMIN")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "updateIncidentStatuFallback")
	public Response UpdateIncidentStatu(String id, String status) {
		log.info("Updating incident status id={} status={}", id, status);
		Response response = new Response();
		try {
			Optional<Incident> optiinalIncident = incidentRepository.findById(id);
			if (optiinalIncident.isEmpty()) {
				response.setSuccess(true);
				response.setMessage("Incident is not found with this id :" + id);
				response.setStatus(200);
				return response;
			}
			Incident incident = optiinalIncident.get();
			try {
				incident.setStatus(IncidentStatus.valueOf(status.toUpperCase()));
			} catch (Exception e) {
				response.setSuccess(true);
				response.setMessage("Invalid status value: " + status);
				response.setStatus(200);
				return response;
			}
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy h:mma");

			String now = LocalDateTime.now().format(formatter);
			incident.setUpdatedAt(now);
			log.debug("Saving updated incident id={}", id);
			Incident updatedIncident = incidentRepository.save(incident);
			log.info("Incident status updated successfully id={}", id);
			IncidentDTO updatedIncidentDTO = convertToDTO(updatedIncident);
			response.setSuccess(true);
			response.setData(updatedIncidentDTO);
			response.setMessage("Status updated successfully");
			response.setStatus(200);
			return response;
		} catch (Exception e) {
			log.error("Failed while updating incident status id={}", id, e);
			response.setSuccess(false);
			response.setMessage("error occured while updating incident status :" + e.getMessage());
			response.setStatus(500);
			return response;
		}

	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteIncidentFallback")
	public Response deleteIncident(String id) {
		log.info("Deleting incident id={}", id);
		Response response = new Response();
		try {
			Optional<Incident> incident = incidentRepository.findById(id);
			if (incident.isEmpty()) {
				response.setSuccess(true);
				response.setMessage("Incident is not found with this id :" + id);
				response.setStatus(200);
				return response;
			}
			log.debug("Deleting incident from repository id={}", id);
			incidentRepository.deleteById(id);
			log.info("Incident deleted successfully id={}", id);
			response.setSuccess(true);
			response.setMessage("Incident deleted successfully");
			response.setStatus(200);
			return response;
		} catch (Exception e) {
			log.error("Failed while updating incident status id={}", id, e);
			response.setSuccess(false);
			response.setMessage("error occured while deleting incident using incidentId :" + e.getMessage());
			response.setStatus(500);
			return response;

		}

	}

//-------------------Mapper for incident-----------------------------
	private IncidentDTO convertToDTO(Incident incident) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy h:mma");

		return IncidentDTO.builder().id(incident.getId()).title(incident.getTitle())
				.description(incident.getDescription()).status(incident.getStatus().toString())
				.raisedBy(incident.getRaisedBy()).assignedTo(incident.getAssignedTo()).priority(incident.getPriority())
				.createdAt(incident.getCreatedAt() != null ? incident.getCreatedAt().formatted(formatter) : null)
				.updatedAt(incident.getUpdatedAt() != null ? incident.getUpdatedAt().formatted(formatter) : null)
				.build();
	}
	
	public Response createIncidentFallback(IncidentDTO dto, Exception ex) {
	    log.error("Rate limit triggered in createIncident", ex);
	    return buildRateLimitResponse(ex);
	}

	public Response getAllIncidentsFallback(Exception ex) {
	    log.error("Rate limit triggered in getAllIncidents", ex);
	    return buildRateLimitResponse(ex);
	}

	public Response updateIncidentStatuFallback(String id, String status, Exception ex) {
	    log.error("Rate limit triggered in UpdateIncidentStatu id={} status={}", id, status, ex);
	    return buildRateLimitResponse(ex);
	}

	public Response deleteIncidentFallback(String id, Exception ex) {
	    log.error("Rate limit triggered in deleteIncident id={}", id, ex);
	    return buildRateLimitResponse(ex);
	}

	public Response buildRateLimitResponse(Exception ex) {

	    Response response = new Response();

	    response.setSuccess(false);
	    response.setStatus(429);
	    response.setMessage("Too many requests. Please try again later.");
	    response.setData(null);

	    return response;
	}

}
