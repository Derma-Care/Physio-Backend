package com.pharmacyManagement.service.impl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.dto.SupplierDTO;
import com.pharmacyManagement.dto.SupplierLoginRequest;
import com.pharmacyManagement.entity.Supplier;
import com.pharmacyManagement.repository.SupplierRepository;
import com.pharmacyManagement.service.SupplierService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

	private static final Logger log = LoggerFactory.getLogger(SupplierServiceImpl.class);

	private final SupplierRepository supplierRepository;
	private final EmailService emailService;

	// ---------------------------------------------------------
	// CREATE SUPPLIER
	// ---------------------------------------------------------
	@Override
	public Response addSupplier(SupplierDTO dto) {

		log.info("Request received to add supplier: {}", dto);
		Response response = new Response();

		try {
			// -------- VALIDATIONS --------
			if (dto == null) {
				return buildError(response, "Supplier details cannot be null", HttpStatus.BAD_REQUEST);
			}

			if (dto.getSupplierName() == null || dto.getSupplierName().trim().isEmpty()) {
				return buildError(response, "Supplier name is required", HttpStatus.BAD_REQUEST);
			}

			if (dto.getContactDetails() == null ||
				dto.getContactDetails().getMobileNumber() == null ||
				dto.getContactDetails().getMobileNumber().trim().isEmpty()) 
			{
				return buildError(response, "Primary mobile number is required", HttpStatus.BAD_REQUEST);
			}

			// -------- DUPLICATE CHECKS --------
			if (supplierRepository.existsBySupplierNameIgnoreCase(dto.getSupplierName())) {
				return buildError(response, "Supplier name already exists: " + dto.getSupplierName(),
						HttpStatus.CONFLICT);
			}

			if (supplierRepository.existsByContactDetailsMobileNumber(dto.getContactDetails().getMobileNumber())) {
				return buildError(response, "Mobile number already exists: " + dto.getContactDetails().getMobileNumber(),
						HttpStatus.CONFLICT);
			}

			if (dto.getContactDetails().getEmail() != null &&
				supplierRepository.existsByContactDetailsEmail(dto.getContactDetails().getEmail())) {
				return buildError(response, "Email already exists: " + dto.getContactDetails().getEmail(),
						HttpStatus.CONFLICT);
			}

			// -------- SAVE SUPPLIER --------
			Supplier supplier = mapToEntity(dto);

			// Generate Supplier ID
			String supplierId = generateSupplierId();
			supplier.setSupplierId(supplierId);

			// Username = Supplier ID
			supplier.setUserName(supplierId);

			// Generate Password
			String password = generatePassword();
			supplier.setPassword(password);

			supplierRepository.save(supplier);

			// Send credentials to email
			Map<String, String> emailData = new HashMap<>();
			emailData.put("subject", "Supplier Account Created");
			emailData.put("message", "Your supplier account has been created successfully.");
			emailData.put("username", supplier.getUserName());
			emailData.put("password", password);

			emailService.sendEmail(
			        supplier.getContactDetails().getEmail(),
			        emailData
			);

			log.info("Supplier added successfully: {}", supplier.getSupplierId());

			response.setSuccess(true);
			response.setMessage("Supplier added successfully");
			response.setData(mapToDTO(supplier));
			response.setStatus(HttpStatus.CREATED.value());

		} catch (Exception ex) {
			log.error("Error while adding supplier", ex);
			return buildError(response, "An error occurred while adding supplier",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	// ---------------------------------------------------------
	// UPDATE SUPPLIER
	// ---------------------------------------------------------
	@Override
	public Response updateSupplier(String supplierId, SupplierDTO dto) {

		log.info("Request received to update supplier: {}", supplierId);
		Response response = new Response();

		try {

			if (supplierId == null || supplierId.isEmpty()) {
				return buildError(response, "Supplier ID cannot be null or empty", HttpStatus.BAD_REQUEST);
			}

			Supplier existing = supplierRepository.findById(supplierId).orElse(null);

			if (existing == null) {
				return buildError(response, "Supplier not found with ID: " + supplierId, HttpStatus.NOT_FOUND);
			}

			// -------- Update fields --------
			if (dto.getSupplierName() != null)
				existing.setSupplierName(dto.getSupplierName());

			if (dto.getGstNumber() != null)
				existing.setGstNumber(dto.getGstNumber());

			if (dto.getRegistrationNumber() != null)
				existing.setRegistrationNumber(dto.getRegistrationNumber());

			if (dto.getTinNumber() != null)
				existing.setTinNumber(dto.getTinNumber());
			
			if (dto.getCstNumber() != null)
				existing.setCstNumber(dto.getCstNumber());

			if (dto.getForm20B() != null)
				existing.setForm20B(dto.getForm20B());

			if (dto.getForm21B() != null)
				existing.setForm21B(dto.getForm21B());

			if (dto.getAddress() != null)
				existing.setAddress(dto.getAddress());

			if (dto.getCity() != null)
				existing.setCity(dto.getCity());

			if (dto.getArea() != null)
				existing.setArea(dto.getArea());

			existing.setNonLocalSupplier(dto.isNonLocalSupplier());

			// -------- Contact Details Update --------
			if (dto.getContactDetails() != null)
				existing.setContactDetails(dto.getContactDetails());

			existing.setActive(dto.isActive());
			existing.setPreviousAdjustment(dto.getPreviousAdjustment());
			existing.setDuePaidBill(dto.getDuePaidBill());

			supplierRepository.save(existing);

			log.info("Supplier updated successfully: {}", supplierId);

			response.setSuccess(true);
			response.setMessage("Supplier updated successfully");
			response.setData(mapToDTO(existing));
			response.setStatus(HttpStatus.OK.value());

		} catch (Exception ex) {
			log.error("Error while updating supplier {}", supplierId, ex);
			return buildError(response, "An error occurred while updating supplier",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	// ---------------------------------------------------------
	// GET SUPPLIER BY ID
	// ---------------------------------------------------------
	@Override
	public Response getSupplierById(String supplierId) {

		log.info("Fetching supplier details for ID: {}", supplierId);
		Response response = new Response();

		try {

			if (supplierId == null || supplierId.isEmpty()) {
				return buildError(response, "Supplier ID cannot be empty", HttpStatus.BAD_REQUEST);
			}

			Supplier supplier = supplierRepository.findById(supplierId).orElse(null);

			if (supplier == null) {
				return buildError(response, "Supplier not found with ID: " + supplierId, HttpStatus.NOT_FOUND);
			}

			response.setSuccess(true);
			response.setMessage("Supplier retrieved successfully");
			response.setData(mapToDTO(supplier));
			response.setStatus(HttpStatus.OK.value());

		} catch (Exception ex) {
			log.error("Error fetching supplier {}", supplierId, ex);
			return buildError(response, "An error occurred while fetching supplier",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	// ---------------------------------------------------------
	// GET ALL SUPPLIERS
	// ---------------------------------------------------------
	@Override
	public Response getAllSuppliers() {

		log.info("Fetching all suppliers");
		Response response = new Response();

		try {

			List<SupplierDTO> list = supplierRepository.findAll()
					.stream()
					.map(this::mapToDTO)
					.collect(Collectors.toList());

			response.setSuccess(true);
			response.setMessage("Suppliers fetched successfully");
			response.setData(list);
			response.setStatus(HttpStatus.OK.value());

		} catch (Exception ex) {
			log.error("Error fetching suppliers", ex);
			return buildError(response, "An error occurred while fetching suppliers",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	// ---------------------------------------------------------
	// DELETE SUPPLIER
	// ---------------------------------------------------------
	@Override
	public Response deleteSupplier(String supplierId) {

		log.info("Request received to delete supplier: {}", supplierId);
		Response response = new Response();

		try {

			if (supplierId == null || supplierId.isEmpty()) {
				return buildError(response, "Supplier ID cannot be null or empty", HttpStatus.BAD_REQUEST);
			}

			if (!supplierRepository.existsById(supplierId)) {
				return buildError(response, "Supplier not found: " + supplierId, HttpStatus.NOT_FOUND);
			}

			supplierRepository.deleteById(supplierId);

			response.setSuccess(true);
			response.setMessage("Supplier deleted successfully");
			response.setStatus(HttpStatus.OK.value());

		} catch (Exception ex) {
			log.error("Error deleting supplier {}", supplierId, ex);
			return buildError(response, "An error occurred while deleting supplier",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	// ---------------------------------------------------------
	// MAPPING METHODS
	// ---------------------------------------------------------
	private Supplier mapToEntity(SupplierDTO dto) {

		Supplier supplier = new Supplier();

		supplier.setSupplierName(dto.getSupplierName());
		supplier.setGstNumber(dto.getGstNumber());
		supplier.setRegistrationNumber(dto.getRegistrationNumber());
		supplier.setCstNumber(dto.getCstNumber());
		supplier.setTinNumber(dto.getTinNumber());
		supplier.setForm20B(dto.getForm20B());
		supplier.setForm21B(dto.getForm21B());
		supplier.setAddress(dto.getAddress());
		supplier.setCity(dto.getCity());
		supplier.setArea(dto.getArea());
		supplier.setNonLocalSupplier(dto.isNonLocalSupplier());
		supplier.setActive(dto.isActive());
		supplier.setContactDetails(dto.getContactDetails());
		supplier.setUserName(dto.getUserName());
		supplier.setPassword(dto.getPassword());
		supplier.setClinicId(dto.getClinicId());
		supplier.setBranchId(dto.getBranchId());
		supplier.setPreviousAdjustment(dto.getPreviousAdjustment());
		supplier.setDuePaidBill(dto.getDuePaidBill());

		return supplier;
	}

	private SupplierDTO mapToDTO(Supplier supplier) {

		SupplierDTO dto = new SupplierDTO();

		dto.setSupplierId(supplier.getSupplierId());
		dto.setSupplierName(supplier.getSupplierName());
		dto.setGstNumber(supplier.getGstNumber());
		dto.setRegistrationNumber(supplier.getRegistrationNumber());
		dto.setCstNumber(supplier.getCstNumber());
		dto.setTinNumber(supplier.getTinNumber());
		dto.setForm20B(supplier.getForm20B());
		dto.setForm21B(supplier.getForm21B());
		dto.setAddress(supplier.getAddress());
		dto.setCity(supplier.getCity());
		dto.setArea(supplier.getArea());
		dto.setNonLocalSupplier(supplier.isNonLocalSupplier());
		dto.setActive(supplier.isActive());
		dto.setContactDetails(supplier.getContactDetails());
		dto.setUserName(supplier.getUserName());
		dto.setUserName(supplier.getUserName());
		dto.setPassword(supplier.getPassword());
		dto.setClinicId(supplier.getClinicId());
		dto.setBranchId(supplier.getBranchId());
		dto.setPreviousAdjustment(supplier.getPreviousAdjustment());
		dto.setDuePaidBill(supplier.getDuePaidBill());
		
		

		return dto;
	}
	@Override
	public Response supplierLogin(SupplierLoginRequest request) {

	    Response response = new Response();

	    try {

	        Optional<Supplier> optionalSupplier =
	                supplierRepository.findByUserName(request.getUserName());

	        if (optionalSupplier.isEmpty()) {
	            response.setSuccess(false);
	            response.setMessage("Invalid username");
	            response.setStatus(HttpStatus.NOT_FOUND.value());
	            response.setData(null);
	            return response;
	        }

	        Supplier supplier = optionalSupplier.get();

	        if (!supplier.getPassword().equals(request.getPassword())) {
	            response.setSuccess(false);
	            response.setMessage("Invalid password");
	            response.setStatus(HttpStatus.UNAUTHORIZED.value());
	            response.setData(null);
	            return response;
	        }

	        // Login Response Data
	        Map<String, Object> loginData = new HashMap<>();
	        loginData.put("supplierId", supplier.getSupplierId());
	        loginData.put("supplierName", supplier.getSupplierName());
	        loginData.put("contactPerson", supplier.getContactDetails().getContactPerson());
	        loginData.put("mobileNumber", supplier.getContactDetails().getMobileNumber());
	        loginData.put("clinicId", supplier.getClinicId());
	        loginData.put("branchId", supplier.getBranchId());

	        response.setSuccess(true);
	        response.setMessage("Login successful. Welcome back, " + supplier.getSupplierName() + ".");
	        response.setStatus(HttpStatus.OK.value());
	        response.setData(loginData);

	    } catch (Exception e) {
	        log.error("Supplier login error", e);
	        response.setSuccess(false);
	        response.setMessage("Login failed due to server error");
	        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
	        response.setData(null);
	    }

	    return response;
	}
	
	@Override
	public Response getSuppliersByClinicAndBranch(String clinicId, String branchId) {

	    Response response = new Response();

	    try {

	        List<Supplier> suppliers =
	                supplierRepository.findByClinicIdAndBranchId(clinicId, branchId);

	        if (suppliers.isEmpty()) {
	            response.setSuccess(false);
	            response.setMessage("No suppliers found");
	            response.setStatus(HttpStatus.NOT_FOUND.value());
	            response.setData(null);
	            return response;
	        }

	        response.setSuccess(true);
	        response.setMessage("Suppliers fetched successfully");
	        response.setStatus(HttpStatus.OK.value());
	        response.setData(suppliers);

	    } catch (Exception e) {
	        log.error("Error fetching suppliers", e);

	        response.setSuccess(false);
	        response.setMessage("Error fetching suppliers");
	        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
	        response.setData(null);
	    }

	    return response;
	}

	// ---------------------------------------------------------
	// UTILITY METHOD
	// ---------------------------------------------------------
	private Response buildError(Response r, String message, HttpStatus status) {
		r.setSuccess(false);
		r.setMessage(message);
		r.setStatus(status.value());
		return r;
	}
	
	private String generateSupplierId() {
	    return "SUP-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}
	private String generatePassword() {
	    return java.util.UUID.randomUUID().toString().substring(0,8);
	}
}
