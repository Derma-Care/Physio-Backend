package com.chiselon.physiotherapydoctor.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.chiselon.physiotherapydoctor.dto.ListOfMedicinesDTO;
import com.chiselon.physiotherapydoctor.dto.Response;
import com.chiselon.physiotherapydoctor.entity.ListOfMedicines;
import com.chiselon.physiotherapydoctor.repository.ListOfMedicinesRepository;
import com.chiselon.physiotherapydoctor.service.ListOfMedicinesService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListOfMedicinesServiceImpl implements ListOfMedicinesService {

	private final ListOfMedicinesRepository repository;

	// ✅ Create
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "createFallback")
	@Secured("ROLE_DOCTOR")
	public Response create(ListOfMedicinesDTO dto) {

	    long startTime = System.currentTimeMillis();

	    log.info("Entered create() with clinicId : {}", dto.getClinicId());

	    try {

	        log.debug("Converting DTO to entity for clinicId : {}", dto.getClinicId());

	        ListOfMedicines entity = convertToEntity(dto);

	        log.debug("Saving medicine list to repository");

	        ListOfMedicines saved = repository.save(entity);

	        log.info("Medicine list created successfully with id : {}", saved.getId());

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info("create() completed successfully in {} ms", executionTime);

	        return new Response(
	                true,
	                convertToDTO(saved),
	                "Medicine list created successfully",
	                HttpStatus.CREATED.value());

	    } catch (Exception e) {

	        log.error("Exception occurred while creating medicine list. Error : {}",
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Failed to create medicine list : " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());
	    }
	}
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updateFallback")
	@Secured("ROLE_DOCTOR")
	public Response update(String id, ListOfMedicinesDTO dto) {

	    long startTime = System.currentTimeMillis();

	    log.info("Entered update() with id : {}", id);

	    try {

	        log.debug("Fetching medicine list from repository with id : {}", id);

	        ListOfMedicines existing = repository.findById(id)
	                .orElseThrow(() -> {
	                    log.warn("Medicine list not found with id : {}", id);
	                    return new RuntimeException(
	                            "Medicine list not found with id: " + id);
	                });

	        existing.setClinicId(dto.getClinicId());
	        existing.setListOfMedicines(dto.getListOfMedicines());

	        log.debug("Saving updated medicine list with id : {}", id);

	        ListOfMedicines updated = repository.save(existing);

	        log.info("Medicine list updated successfully with id : {}", id);

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info("update() completed successfully in {} ms", executionTime);

	        return new Response(
	                true,
	                convertToDTO(updated),
	                "Medicine list updated successfully",
	                HttpStatus.OK.value());

	    } catch (Exception e) {

	        log.error("Exception occurred while updating medicine list id : {}. Error : {}",
	                id,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Failed to update medicine list : " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());
	    }
	}
	
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "deleteFallback")
	@Secured("ROLE_DOCTOR")
	public Response delete(String id) {

	    long startTime = System.currentTimeMillis();

	    log.info("Entered delete() with id : {}", id);

	    try {

	        log.debug("Checking medicine list existence with id : {}", id);

	        if (!repository.existsById(id)) {

	            log.warn("Medicine list not found with id : {}", id);

	            return new Response(
	                    false,
	                    null,
	                    "Medicine list not found with id: " + id,
	                    HttpStatus.NOT_FOUND.value());
	        }

	        log.info("Deleting medicine list with id : {}", id);

	        repository.deleteById(id);

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info("delete() completed successfully in {} ms", executionTime);

	        return new Response(
	                true,
	                null,
	                "Medicine list deleted successfully",
	                HttpStatus.OK.value());

	    } catch (Exception e) {

	        log.error("Exception occurred while deleting medicine list id : {}. Error : {}",
	                id,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Failed to delete medicine list : " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());
	    }
	}
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getByIdFallback")
	@Secured("ROLE_DOCTOR")
	public Response getById(String id) {

	    long startTime = System.currentTimeMillis();

	    log.info("Entered getById() with id : {}", id);

	    try {

	        log.debug("Fetching medicine list from repository with id : {}", id);

	        Optional<ListOfMedicines> optional = repository.findById(id);

	        if (optional.isPresent()) {

	            log.info("Medicine list found with id : {}", id);

	            long executionTime = System.currentTimeMillis() - startTime;

	            log.info("getById() completed successfully in {} ms", executionTime);

	            return new Response(
	                    true,
	                    convertToDTO(optional.get()),
	                    "Medicine list fetched successfully",
	                    HttpStatus.OK.value());
	        }

	        log.warn("Medicine list not found with id : {}", id);

	        return new Response(
	                false,
	                null,
	                "Medicine list not found with id: " + id,
	                HttpStatus.NOT_FOUND.value());

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching medicine list id : {}. Error : {}",
	                id,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Failed to fetch medicine list : " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());
	    }
	}
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getAllFallback")
	@Secured("ROLE_DOCTOR")
	public Response getAll() {

	    long startTime = System.currentTimeMillis();

	    log.info("Entered getAll()");

	    try {

	        log.debug("Fetching all medicine lists from repository");

	        List<ListOfMedicinesDTO> medicines =
	                repository.findAll()
	                        .stream()
	                        .map(this::convertToDTO)
	                        .collect(Collectors.toList());

	        log.info("Retrieved {} medicine lists",
	                medicines.size());

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info("getAll() completed successfully in {} ms",
	                executionTime);

	        return new Response(
	                true,
	                medicines,
	                "All medicine lists fetched successfully",
	                HttpStatus.OK.value());

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching all medicine lists. Error : {}",
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Failed to fetch medicine lists : " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());
	    }
	}
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getByClinicIdFallback")
	@Secured("ROLE_DOCTOR")
	public Response getByClinicId(String clinicId) {

	    long startTime = System.currentTimeMillis();

	    log.info("Entered getByClinicId() with clinicId : {}", clinicId);

	    try {

	        log.debug("Fetching medicine lists for clinicId : {}", clinicId);

	        List<ListOfMedicinesDTO> medicines =
	                repository.findByClinicId(clinicId)
	                        .stream()
	                        .map(this::convertToDTO)
	                        .collect(Collectors.toList());

	        log.info("Retrieved {} medicine lists for clinicId : {}",
	                medicines.size(),
	                clinicId);

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info("getByClinicId() completed successfully in {} ms",
	                executionTime);

	        return new Response(
	                true,
	                medicines,
	                "Medicine lists fetched for clinicId: " + clinicId,
	                HttpStatus.OK.value());

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching medicine lists for clinicId : {}. Error : {}",
	                clinicId,
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Failed to fetch medicine lists : " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());
	    }
	}
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "addOrSearchMedicineFallback")
	@Secured("ROLE_DOCTOR")
	public Response addOrSearchMedicine(ListOfMedicinesDTO dto) {

	    long startTime = System.currentTimeMillis();

	    log.info("Entered addOrSearchMedicine() with clinicId : {}",
	            dto.getClinicId());

	    try {

	        String clinicId = dto.getClinicId();
	        List<String> medicinesToAdd = dto.getListOfMedicines();

	        if (clinicId == null || clinicId.isEmpty()
	                || medicinesToAdd == null
	                || medicinesToAdd.isEmpty()) {

	            log.warn("Invalid request. clinicId or medicine list is empty");

	            return new Response(
	                    false,
	                    null,
	                    "clinicId and listOfMedicines are required",
	                    HttpStatus.BAD_REQUEST.value());
	        }

	        log.debug("Fetching medicine lists for clinicId : {}", clinicId);

	        List<ListOfMedicines> lists =
	                repository.findByClinicId(clinicId);

	        log.info("Found {} medicine list records for clinicId : {}",
	                lists.size(),
	                clinicId);

	        ListOfMedicines targetList;

	        if (lists.isEmpty()) {

	            log.info("No medicine list found. Creating new list for clinicId : {}",
	                    clinicId);

	            targetList = new ListOfMedicines();
	            targetList.setClinicId(clinicId);
	            targetList.setListOfMedicines(
	                    new ArrayList<>(medicinesToAdd));

	        } else {

	            targetList = lists.get(0);

	            if (targetList.getListOfMedicines() == null) {
	                targetList.setListOfMedicines(new ArrayList<>());
	            }

	            for (String med : medicinesToAdd) {

	                if (!targetList.getListOfMedicines().contains(med)) {

	                    log.debug("Adding new medicine : {}", med);

	                    targetList.getListOfMedicines().add(med);
	                }
	            }
	        }

	        log.debug("Saving medicine list for clinicId : {}", clinicId);

	        ListOfMedicines saved = repository.save(targetList);

	        log.info("Medicine list saved successfully with id : {}",
	                saved.getId());

	        long executionTime = System.currentTimeMillis() - startTime;

	        log.info("addOrSearchMedicine() completed successfully in {} ms",
	                executionTime);

	        return new Response(
	                true,
	                convertToDTO(saved),
	                "Medicine list updated/added successfully",
	                HttpStatus.CREATED.value());

	    } catch (Exception e) {

	        log.error("Exception occurred while adding/searching medicines. clinicId : {} Error : {}",
	                dto.getClinicId(),
	                e.getMessage(),
	                e);

	        return new Response(
	                false,
	                null,
	                "Failed to process medicine list : " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR.value());
	    }
	}
	
	

	// ✅ Helper methods for conversion
	private ListOfMedicinesDTO convertToDTO(ListOfMedicines entity) {
		return new ListOfMedicinesDTO(entity.getId(), entity.getClinicId(), entity.getListOfMedicines());
	}

	private ListOfMedicines convertToEntity(ListOfMedicinesDTO dto) {
		return new ListOfMedicines(dto.getId(), dto.getClinicId(), dto.getListOfMedicines());
	}


    private Response buildRateLimitResponse(Exception ex) {
        return new Response(false, null,
                "Rate limit exceeded. Please try again later.",
                429);
    }

    public Response createFallback(ListOfMedicinesDTO dto, Exception ex) { return buildRateLimitResponse(ex); }
    public Response updateFallback(String id, ListOfMedicinesDTO dto, Exception ex) { return buildRateLimitResponse(ex); }
    public Response deleteFallback(String id, Exception ex) { return buildRateLimitResponse(ex); }
    public Response getByIdFallback(String id, Exception ex) { return buildRateLimitResponse(ex); }
    public Response getAllFallback(Exception ex) { return buildRateLimitResponse(ex); }
    public Response getByClinicIdFallback(String clinicId, Exception ex) { return buildRateLimitResponse(ex); }
    public Response addOrSearchMedicineFallback(ListOfMedicinesDTO dto, Exception ex) { return buildRateLimitResponse(ex); }

}