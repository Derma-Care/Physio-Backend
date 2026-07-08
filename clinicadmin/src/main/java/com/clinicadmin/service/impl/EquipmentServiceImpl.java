package com.clinicadmin.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import com.clinicadmin.dto.EquipmentDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.Equipment;
import com.clinicadmin.repository.EquipmentRepository;
import com.clinicadmin.service.EquipmentService;
import com.clinicadmin.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository repository;
    
    private final S3Service s3Service;

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "createEquipmentFallback")
    public Response createEquipment(EquipmentDTO dto) {
        log.info("Creating equipment clinicId={} branchId={} name={}", dto.getClinicId(), dto.getBranchId(), dto.getName());

        Equipment equipment = convertToEntity(dto);

        log.debug("Saving equipment entity to repository");
        Equipment savedEquipment = repository.save(equipment);
        log.info("Equipment created successfully equipmentId={}", savedEquipment.getEquipmentId());

        Response response = new Response();
        response.setSuccess(true);
        response.setMessage("Equipment Created Successfully");
        response.setStatus(201);
        response.setData(convertToDto(savedEquipment));

        return response;
    }
  
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getEquipmentByIdFallback")
    public Response getEquipmentById(String equipmentId) {
        log.info("Fetching equipment equipmentId={}", equipmentId);

        log.debug("Looking up equipment by id");
        Equipment equipment = repository.findById(equipmentId).orElse(null);

        Response response = new Response();

        if (equipment == null) {
            response.setSuccess(false);
            response.setMessage("Equipment Not Found");
            response.setStatus(404);
            return response;
        }

        response.setSuccess(true);
        response.setMessage("Equipment Retrieved Successfully");
        response.setStatus(200);
        response.setData(convertToDto(equipment));

        return response;
    }


 
    		@Override
    		 @Secured("ROLE_CLINICADMIN")
    		  @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllEquipmentFallback")
    		public Response getAllEquipment() {
            log.info("Fetching all equipment records");

    		    List<EquipmentDTO> equipmentList = repository.findAll()
    		            .stream()
    		            .map(this::convertToDto)
    		            .collect(Collectors.toList());

    		    Response response = new Response();

    		    if (equipmentList.isEmpty()) {
    		        response.setSuccess(false);
    		        response.setMessage("No Equipment Records Found");
    		        response.setStatus(404);
    		        return response;
    		    }

    		    response.setSuccess(true);
    		    response.setMessage("Equipment List Retrieved Successfully");
    		    response.setStatus(200);
    		    response.setData(equipmentList);

    		    return response;
    		}

    	   
      				@Override
    				 @Secured("ROLE_CLINICADMIN")			
    				  @RateLimiter(name = "clinicAdminService", fallbackMethod = "getEquipmentByIdFallback")
    				public Response getEquipmentByClinicIdAndBranchId(
    				        String clinicId,
    				        String branchId) {

    				    List<EquipmentDTO> equipmentList = repository
    				            .findByClinicIdAndBranchId(clinicId, branchId)
    				            .stream()
    				            .map(this::convertToDto)
    				            .collect(Collectors.toList());

    				    Response response = new Response();

    				    if (equipmentList.isEmpty()) {
    				        response.setSuccess(false);
    				        response.setMessage("No Equipment Records Found");
    				        response.setStatus(404);
    				        return response;
    				    }

    				    response.setSuccess(true);
    				    response.setMessage("Equipment List Retrieved Successfully");
    				    response.setStatus(200);
    				    response.setData(equipmentList);

    				    return response;
    				}
    			


 @Override
 @Secured("ROLE_CLINICADMIN")
 @RateLimiter(name = "clinicAdminService", fallbackMethod = "updateEquipmentFallback")
 public Response updateEquipment(
    				        String equipmentId,
    				        EquipmentDTO dto) {

    				    log.info("Updating equipment equipmentId={}", equipmentId);
                    log.debug("Looking up equipment equipmentId={}", equipmentId);
                    Equipment existing = repository.findById(equipmentId).orElse(null);

    				    Response response = new Response();

    				    if (existing == null) {
    				        response.setSuccess(false);
    				        response.setMessage("Equipment Not Found");
    				        response.setStatus(404);
    				        return response;
    				    }

    				    existing.setClinicId(dto.getClinicId());
    				    existing.setBranchId(dto.getBranchId());
    				    existing.setName(dto.getName());
    				    existing.setCategory(dto.getCategory());
    				    existing.setType(dto.getType());
    				    existing.setBrand(dto.getBrand());
    				    existing.setModel(dto.getModel());
    				    existing.setSerialNo(dto.getSerialNo());
    				    existing.setStatus(dto.getStatus());
    				    existing.setDepartment(dto.getDepartment());

    				    existing.setPurchaseDate(dto.getPurchaseDate());
    				    existing.setWarrantyExpiry(dto.getWarrantyExpiry());
    				    existing.setAmcStartDate(dto.getAmcStartDate());
    				    existing.setAmcEndDate(dto.getAmcEndDate());

    				    existing.setPurchaseCost(dto.getPurchaseCost());
    				    existing.setCurrentValue(dto.getCurrentValue());

    				    existing.setNextServiceDate(dto.getNextServiceDate());
    				    existing.setLastServiceDate(dto.getLastServiceDate());

    				    existing.setAssignedStaff(dto.getAssignedStaff());

    				    if (dto.getImageUrl() != null
    				            && !dto.getImageUrl().isBlank()) {
    				        existing.setImageUrl(dto.getImageUrl());
    				    }

    				    existing.setNotes(dto.getNotes());
    				    existing.setVendorDetails(dto.getVendorDetails());

    				    log.debug("Saving updated equipment equipmentId={}", equipmentId);
                    Equipment updated = repository.save(existing);
                    log.info("Equipment updated successfully equipmentId={}", updated.getEquipmentId());

    				    response.setSuccess(true);
    				    response.setMessage("Equipment Updated Successfully");
    				    response.setStatus(200);
    				    response.setData(convertToDto(updated));

    				    return response;
    				}

   
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteEquipmentFallback")
    public Response deleteEquipment(String equipmentId) {
        log.info("Deleting equipment equipmentId={}", equipmentId);

        Equipment equipment = repository.findById(equipmentId).orElse(null);

        if (equipment == null) {

            Response response = new Response();
            response.setSuccess(false);
            response.setMessage("Equipment Not Found");
            response.setStatus(404);

            return response;
        }

        log.debug("Deleting equipment from repository equipmentId={}", equipmentId);
        repository.delete(equipment);
        log.info("Equipment deleted successfully equipmentId={}", equipmentId);

        Response response = new Response();
        response.setSuccess(true);
        response.setMessage("Equipment Deleted Successfully");
        response.setStatus(200);
        response.setData(equipmentId);

        return response;
    }
 


    // ===========================
    // Convert DTO -> Entity
    // ===========================

    private Equipment convertToEntity(
            EquipmentDTO dto) {

        Equipment equipment = new Equipment();

        equipment.setEquipmentId(dto.getEquipmentId());
        equipment.setClinicId(dto.getClinicId());
        equipment.setBranchId(dto.getBranchId());
        equipment.setName(dto.getName());
        equipment.setCategory(dto.getCategory());
        equipment.setType(dto.getType());
        equipment.setBrand(dto.getBrand());
        equipment.setModel(dto.getModel());
        equipment.setSerialNo(dto.getSerialNo());
        equipment.setStatus(dto.getStatus());
        equipment.setDepartment(dto.getDepartment());

        equipment.setPurchaseDate(dto.getPurchaseDate());
        equipment.setWarrantyExpiry(dto.getWarrantyExpiry());
        equipment.setAmcStartDate(dto.getAmcStartDate());
        equipment.setAmcEndDate(dto.getAmcEndDate());

        equipment.setPurchaseCost(dto.getPurchaseCost());
        equipment.setCurrentValue(dto.getCurrentValue());

        equipment.setNextServiceDate(dto.getNextServiceDate());
        equipment.setLastServiceDate(dto.getLastServiceDate());

        equipment.setAssignedStaff(dto.getAssignedStaff());
        equipment.setImageUrl(dto.getImageUrl());
        equipment.setNotes(dto.getNotes());

        equipment.setVendorDetails(dto.getVendorDetails());

        return equipment;
    }

    // ===========================
    // Convert Entity -> DTO
    // ===========================

    private EquipmentDTO convertToDto(
            Equipment equipment) {

        EquipmentDTO dto = new EquipmentDTO();

        dto.setEquipmentId(equipment.getEquipmentId());
        dto.setClinicId(equipment.getClinicId());
        dto.setBranchId(equipment.getBranchId());
        dto.setName(equipment.getName());
        dto.setCategory(equipment.getCategory());
        dto.setType(equipment.getType());
        dto.setBrand(equipment.getBrand());
        dto.setModel(equipment.getModel());
        dto.setSerialNo(equipment.getSerialNo());
        dto.setStatus(equipment.getStatus());
        dto.setDepartment(equipment.getDepartment());

        dto.setPurchaseDate(equipment.getPurchaseDate());
        dto.setWarrantyExpiry(equipment.getWarrantyExpiry());
        dto.setAmcStartDate(equipment.getAmcStartDate());
        dto.setAmcEndDate(equipment.getAmcEndDate());

        dto.setPurchaseCost(equipment.getPurchaseCost());
        dto.setCurrentValue(equipment.getCurrentValue());

        dto.setNextServiceDate(equipment.getNextServiceDate());
        dto.setLastServiceDate(equipment.getLastServiceDate());

        dto.setAssignedStaff(equipment.getAssignedStaff());

        dto.setNotes(equipment.getNotes());

        dto.setVendorDetails(equipment.getVendorDetails());

        // Generate signed URL
        if (equipment.getImageUrl() != null
                && !equipment.getImageUrl().isBlank()) {

            dto.setImageUrl(
                    s3Service.generateSignedUrl(
                            equipment.getImageUrl()));
        }

        dto.setNotes(equipment.getNotes());

        dto.setVendorDetails(equipment.getVendorDetails());

        return dto;
    
    }


    
    // ================= RATE LIMITER FALLBACK METHODS =================

    public Response createEquipmentFallback(EquipmentDTO dto, Exception ex){
        log.error("Rate limit triggered in createEquipment", ex);
        return buildRateLimitResponse(ex);
    }

    public Response getEquipmentByIdFallback(String equipmentId, Exception ex){
        log.error("Rate limit triggered in getEquipmentById equipmentId={}", equipmentId, ex);
        return buildRateLimitResponse(ex);
    }

    public Response getAllEquipmentFallback(Exception ex){
        log.error("Rate limit triggered in getAllEquipment", ex);
        return buildRateLimitResponse(ex);
    }

    public Response getEquipmentByClinicIdAndBranchIdFallback(String clinicId,String branchId, Exception ex){
        log.error("Rate limit triggered in getEquipmentByClinicIdAndBranchId clinicId={} branchId={}", clinicId, branchId, ex);
        return buildRateLimitResponse(ex);
    }

    public Response updateEquipmentFallback(String equipmentId, EquipmentDTO dto, Exception ex){
        log.error("Rate limit triggered in updateEquipment equipmentId={}", equipmentId, ex);
        return buildRateLimitResponse(ex);
    }

    public Response deleteEquipmentFallback(String equipmentId, Exception ex){
        log.error("Rate limit triggered in deleteEquipment equipmentId={}", equipmentId, ex);
        return buildRateLimitResponse(ex);
    }

    public Response buildRateLimitResponse(Exception ex) {
        return Response.builder()
                .success(false)
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .message("Too many requests. Please try again later.")
                .build();
    }

private String generateEquipmentId() {
    return "EQU-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
}
}
