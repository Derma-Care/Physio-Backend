package com.clinicadmin.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.EquipmentDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.Equipment;
import com.clinicadmin.repository.EquipmentRepository;
import com.clinicadmin.service.EquipmentService;
import com.clinicadmin.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository repository;
    
    private final S3Service s3Service;


    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response createEquipment(EquipmentDTO dto) {

        log.info("Received request to create equipment.");

        try {
            log.debug("Converting EquipmentDTO to Equipment entity.");

            Equipment equipment = convertToEntity(dto);

            log.debug("Saving equipment to the database.");

            Equipment savedEquipment = repository.save(equipment);

            log.info("Equipment created successfully with Equipment ID: {}", savedEquipment.getEquipmentId());

            Response response = new Response();
            response.setSuccess(true);
            response.setMessage("Equipment Created Successfully");
            response.setStatus(201);
            response.setData(convertToDto(savedEquipment));

            log.info("Returning success response for create equipment request.");

            return response;

        } catch (Exception e) {
            log.error("Exception occurred while creating equipment: {}", e.getMessage(), e);
            throw e;
        }
    }
   


    


    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response getEquipmentById(String equipmentId) {

        log.info("Received request to fetch equipment with ID: {}", equipmentId);

        try {
            log.debug("Searching for equipment in the database.");

            Equipment equipment = repository.findById(equipmentId).orElse(null);

            Response response = new Response();

            if (equipment == null) {
                log.warn("Equipment not found with ID: {}", equipmentId);

                response.setSuccess(false);
                response.setMessage("Equipment Not Found");
                response.setStatus(404);
                return response;
            }

            log.info("Equipment found with ID: {}", equipmentId);

            response.setSuccess(true);
            response.setMessage("Equipment Retrieved Successfully");
            response.setStatus(200);
            response.setData(convertToDto(equipment));

            log.info("Equipment details returned successfully for ID: {}", equipmentId);

            return response;

        } catch (Exception e) {
            log.error("Exception occurred while fetching equipment with ID: {}. Error: {}",
                    equipmentId, e.getMessage(), e);
            throw e;
        }
    }


    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response getAllEquipment() {

        log.info("Received request to fetch all equipment records.");

        try {
            log.debug("Fetching all equipment records from the database.");

            List<EquipmentDTO> equipmentList = repository.findAll()
                    .stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            Response response = new Response();

            if (equipmentList.isEmpty()) {
                log.warn("No equipment records found in the database.");

                response.setSuccess(false);
                response.setMessage("No Equipment Records Found");
                response.setStatus(404);
                return response;
            }

            log.info("Successfully retrieved {} equipment record(s).", equipmentList.size());

            response.setSuccess(true);
            response.setMessage("Equipment List Retrieved Successfully");
            response.setStatus(200);
            response.setData(equipmentList);

            log.info("Returning equipment list successfully.");

            return response;

        } catch (Exception e) {
            log.error("Exception occurred while fetching equipment list: {}", e.getMessage(), e);
            throw e;
        }
    }
    	

    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response getEquipmentByClinicIdAndBranchId(
            String clinicId,
            String branchId) {

        log.info("Received request to fetch equipment for ClinicId: {} and BranchId: {}",
                clinicId, branchId);

        try {
            log.debug("Fetching equipment records from the database.");

            List<EquipmentDTO> equipmentList = repository
                    .findByClinicIdAndBranchId(clinicId, branchId)
                    .stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            Response response = new Response();

            if (equipmentList.isEmpty()) {
                log.warn("No equipment records found for ClinicId: {} and BranchId: {}",
                        clinicId, branchId);

                response.setSuccess(false);
                response.setMessage("No Equipment Records Found");
                response.setStatus(404);
                return response;
            }

            log.info("Successfully retrieved {} equipment record(s) for ClinicId: {} and BranchId: {}",
                    equipmentList.size(), clinicId, branchId);

            response.setSuccess(true);
            response.setMessage("Equipment List Retrieved Successfully");
            response.setStatus(200);
            response.setData(equipmentList);

            log.info("Returning equipment list successfully.");

            return response;

        } catch (Exception e) {
            log.error("Exception occurred while fetching equipment for ClinicId: {} and BranchId: {}. Error: {}",
                    clinicId, branchId, e.getMessage(), e);
            throw e;
        }
    }
    			



    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response updateEquipment(String equipmentId, EquipmentDTO dto) {

        log.info("Received request to update equipment. EquipmentId: {}", equipmentId);

        try {
            log.debug("Searching for equipment with ID: {}", equipmentId);

            Equipment existing = repository.findById(equipmentId).orElse(null);

            Response response = new Response();

            if (existing == null) {
                log.warn("Equipment not found with ID: {}", equipmentId);

                response.setSuccess(false);
                response.setMessage("Equipment Not Found");
                response.setStatus(404);
                return response;
            }

            log.debug("Updating equipment details for ID: {}", equipmentId);

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

            if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
                log.debug("Updating image URL for Equipment ID: {}", equipmentId);
                existing.setImageUrl(dto.getImageUrl());
            }

            existing.setNotes(dto.getNotes());
            existing.setVendorDetails(dto.getVendorDetails());

            log.debug("Saving updated equipment to the database.");

            Equipment updated = repository.save(existing);

            log.info("Equipment updated successfully. EquipmentId: {}", updated.getEquipmentId());

            response.setSuccess(true);
            response.setMessage("Equipment Updated Successfully");
            response.setStatus(200);
            response.setData(convertToDto(updated));

            log.info("Returning success response for updated equipment. EquipmentId: {}", equipmentId);

            return response;

        } catch (Exception e) {
            log.error("Exception occurred while updating equipment. EquipmentId: {}, Error: {}",
                    equipmentId, e.getMessage(), e);
            throw e;
        }
    }


    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response deleteEquipment(String equipmentId) {

        log.info("Received request to delete equipment. EquipmentId: {}", equipmentId);

        try {
            log.debug("Searching for equipment with ID: {}", equipmentId);

            Equipment equipment = repository.findById(equipmentId).orElse(null);

            if (equipment == null) {

                log.warn("Equipment not found with ID: {}", equipmentId);

                Response response = new Response();
                response.setSuccess(false);
                response.setMessage("Equipment Not Found");
                response.setStatus(404);

                return response;
            }

            log.debug("Deleting equipment with ID: {}", equipmentId);

            repository.delete(equipment);

            log.info("Equipment deleted successfully. EquipmentId: {}", equipmentId);

            Response response = new Response();
            response.setSuccess(true);
            response.setMessage("Equipment Deleted Successfully");
            response.setStatus(200);
            response.setData(equipmentId);

            log.info("Returning success response for delete equipment request.");

            return response;

        } catch (Exception e) {
            log.error("Exception occurred while deleting equipment. EquipmentId: {}, Error: {}",
                    equipmentId, e.getMessage(), e);
            throw e;
        }
    }
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
}