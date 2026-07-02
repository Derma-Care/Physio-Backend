package com.clinicadmin.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.BookingInfoByInput;
import com.clinicadmin.dto.CustomerLoginDTO;
import com.clinicadmin.dto.CustomerOnbordingDTO;
import com.clinicadmin.dto.CustomerResponseDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.CustomerCredentials;
import com.clinicadmin.entity.CustomerOnbording;
import com.clinicadmin.repository.CustomerCredentialsRepository;
import com.clinicadmin.repository.CustomerOnboardingRepository;
import com.clinicadmin.service.CustomerOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class CustomerOnboardingServiceImpl implements CustomerOnboardingService {

	@Autowired
	private CustomerOnboardingRepository onboardingRepository;

	@Autowired
	private CustomerCredentialsRepository credentialsRepository;

	@Autowired
	private SequenceGeneratorService sequenceGeneratorService;
	
//	@Autowired
//	private MongoTemplate mongoTemplate;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	// ----------------- CREATE (ONBOARD) -----------------
	@Override
	 @Secured("ROLE_CLINICADMIN")
	public Response onboardCustomer(CustomerOnbordingDTO dto) {
		Response response = new Response();

		try {
			Optional<CustomerOnbording> existingCustomer =
	                onboardingRepository.findByMobileNumber(dto.getMobileNumber());

	        if (existingCustomer.isPresent()) {
	            response.setSuccess(false);
	            response.setMessage("Mobile number already exists");
	            response.setStatus(400);
	            return response;
	        }
			// Generate unique IDs
			long customerSeq = sequenceGeneratorService.getNextSequence(dto.getBranchId() + "_customer");
			long patientSeq = sequenceGeneratorService
					.getNextSequence( dto.getBranchId() + "_patient");

			String customerId =  dto.getBranchId()+"_CR_" + String.format("%05d", customerSeq);
			String patientId =  dto.getBranchId() + "_PT_" + String.format("%05d", patientSeq);
			
			// Generate Referral Code
			String prefix = dto.getFullName()
			                   .replaceAll("\\s+", "")
			                   .toUpperCase()
			                   .substring(0, Math.min(3, dto.getFullName().length()));

			long referralSeq = sequenceGeneratorService.getNextSequence("referral_code_seq");
			String referralCode = prefix + "_" + String.format("%05d", referralSeq);

			// Convert DTO -> Entity
			CustomerOnbording entity = convertToEntity(dto);
			entity.setCustomerId(customerId);
			entity.setPatientId(patientId);
			entity.setReferralCode(referralCode);

		    onboardingRepository.save(entity);
			

			// Save credentials
			CustomerCredentials credentials = new CustomerCredentials();
			credentials.setUserName(customerId);
			credentials.setPassword(passwordEncoder.encode(dto.getMobileNumber())); // mobile = default password
			credentials.setHospitalId(dto.getHospitalId());
			credentials.setBranchId(dto.getBranchId());
			credentials.setHospitalName(dto.getHospitalName());
			credentialsRepository.save(credentials);

			CustomerOnbordingDTO resDTO = convertToDTO(entity);
			resDTO.setUserName(customerId);
			resDTO.setPassword(dto.getMobileNumber());

			response.setSuccess(true);
			response.setMessage("Customer onboarded successfully");
			response.setData(resDTO);
			response.setStatus(201);

		} catch (Exception e) {
			response.setSuccess(false);
			response.setMessage("Error during onboarding: " + e.getMessage());
			response.setStatus(500);
		}

		return response;
	}

	// ----------------- READ ALL -----------------
	@Override
	@Secured("ROLE_CLINICADMIN")
	public Response getAllCustomers() {

	    log.info("Request received to fetch all customers.");

	    Response response = new Response();

	    try {
	        log.debug("Fetching customer records from database.");

	        List<CustomerOnbordingDTO> customers = onboardingRepository.findAll()
	                .stream()
	                .map(this::convertToDTO)
	                .collect(Collectors.toList());

	        log.info("Successfully fetched {} customer(s) from database.", customers.size());

	        response.setSuccess(true);
	        response.setMessage(customers.isEmpty()
	                ? "No customers found"
	                : "Customers retrieved successfully");
	        response.setData(customers);
	        response.setStatus(200);

	        log.info("Returning response with status: {}", response.getStatus());

	    } catch (Exception e) {
	        log.error("Exception occurred while fetching customers.", e);

	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);

	        log.error("Failed to fetch customers. Response status: {}", response.getStatus());
	    }

	    return response;
	}


	@Override
	@Secured("ROLE_CLINICADMIN")
	public Response getCustomerById(String id) {

	    log.info("Request received to fetch customer with ID: {}", id);

	    Response response = new Response();

	    try {
	        log.debug("Searching customer in database with ID: {}", id);

	        Optional<CustomerOnbording> optional = onboardingRepository.findByCustomerId(id);

	        if (optional.isPresent()) {

	            log.info("Customer found with ID: {}", id);

	            response.setSuccess(true);
	            response.setMessage("Customer found successfully");
	            response.setData(convertToDTO(optional.get()));
	            response.setStatus(200);

	            log.info("Returning customer details for ID: {}", id);

	        } else {

	            log.warn("Customer not found with ID: {}", id);

	            response.setSuccess(false);
	            response.setMessage("Customer not found with ID: " + id);
	            response.setStatus(404);
	        }

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching customer with ID: {}", id, e);

	        response.setSuccess(false);
	        response.setMessage("Error fetching customer: " + e.getMessage());
	        response.setStatus(500);

	        log.error("Failed to fetch customer with ID: {}. Response status: {}", id, response.getStatus());
	    }

	    return response;
	}

	

	@Override
	@Secured({"ROLE_CLINICADMIN","ROLE_BOOKINGSERVICE"})
	public Map<String, String> getCustomerByMobilenumberAndName(String mobilenumber, String name) {

	    log.info("Request received to fetch customer by mobile number: {} and name: {}", mobilenumber, name);

	    Map<String, String> details = new LinkedHashMap<>();

	    try {
	        log.debug("Searching customer in database with mobile number: {} and name: {}", mobilenumber, name);

	        Optional<CustomerOnbording> optional =
	                onboardingRepository.findByMobileNumberAndFullName(mobilenumber, name);

	        if (optional.isPresent()) {

	            details.put("customerId", optional.get().getCustomerId());
	            details.put("patientId", optional.get().getPatientId());

	            log.info("Customer found. CustomerId: {}, PatientId: {}",
	                    optional.get().getCustomerId(),
	                    optional.get().getPatientId());

	            return details;

	        } else {

	            log.warn("No customer found with mobile number: {} and name: {}", mobilenumber, name);
	            return null;
	        }

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching customer with mobile number: {} and name: {}",
	                mobilenumber, name, e);

	        return null;
	    }
	}

	

	@Override
	@Secured("ROLE_CLINICADMIN")
	public Response getCustomerByMobiileNumber(String mobilenumber) {

	    log.info("Request received to fetch customer with mobile number: {}", mobilenumber);

	    Response response = new Response();

	    try {
	        log.debug("Searching customer in database with mobile number: {}", mobilenumber);

	        Optional<CustomerOnbording> optional = onboardingRepository.findByMobileNumber(mobilenumber);

	        if (optional.isPresent()) {

	            log.info("Customer found with mobile number: {}", mobilenumber);

	            response.setSuccess(true);
	            response.setMessage("Customer found successfully");
	            response.setData(convertToDTO(optional.get()));
	            response.setStatus(200);

	            log.info("Returning customer details for mobile number: {}", mobilenumber);

	        } else {

	            log.warn("Customer not found with mobile number: {}", mobilenumber);

	            response.setSuccess(false);
	            response.setMessage("Customer not found with mobile number: " + mobilenumber);
	            response.setStatus(404);
	        }

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching customer with mobile number: {}", mobilenumber, e);

	        response.setSuccess(false);
	        response.setMessage("Error fetching customer: " + e.getMessage());
	        response.setStatus(500);

	        log.error("Failed to fetch customer with mobile number: {}. Response status: {}",
	                mobilenumber, response.getStatus());
	    }

	    return response;
	}
	

	@Override
	@Secured("ROLE_CLINICADMIN")
	public CustomerOnbordingDTO getCustomerByMobileNumberAndClinicId(String mobilenumber, String clinicId) {

	    log.info("Request received to fetch customer with mobile number: {} and clinicId: {}", mobilenumber, clinicId);

	    try {

	        log.debug("Searching customer in database with mobile number: {} and clinicId: {}",
	                mobilenumber, clinicId);

	        CustomerOnbording customer =
	                onboardingRepository.findByMobileNumberAndHospitalId(mobilenumber, clinicId);

	        if (customer != null) {

	            log.info("Customer found with mobile number: {} and clinicId: {}",
	                    mobilenumber, clinicId);

	            return convertToDTO(customer);

	        } else {

	            log.warn("Customer not found with mobile number: {} and clinicId: {}",
	                    mobilenumber, clinicId);

	            return null;
	        }

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching customer with mobile number: {} and clinicId: {}",
	                mobilenumber, clinicId, e);

	        return null;
	    }
	}
	
	
	
	// ----------------- UPDATE -----------------

	@Override
	@Secured("ROLE_CLINICADMIN")
	public Response updateCustomer(String customerId, CustomerOnbordingDTO dto) {

	    log.info("Request received to update customer with ID: {}", customerId);

	    Response response = new Response();

	    try {

	        log.debug("Searching customer in database with ID: {}", customerId);

	        Optional<CustomerOnbording> optional = onboardingRepository.findByCustomerId(customerId);

	        if (optional.isEmpty()) {

	            log.warn("Customer not found with ID: {}", customerId);

	            response.setSuccess(false);
	            response.setMessage("Customer not found");
	            response.setStatus(404);
	            return response;
	        }

	        CustomerOnbording entity = optional.get();

	        log.debug("Updating customer details for ID: {}", customerId);

	        // Null checks before updating fields
	        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
	            entity.setFullName(dto.getFullName());
	        }
	        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
	            entity.setEmail(dto.getEmail());
	        }
	        if (dto.getMobileNumber() != null && !dto.getMobileNumber().isBlank()) {
	            entity.setMobileNumber(dto.getMobileNumber());
	        }
	        if (dto.getGender() != null && !dto.getGender().isBlank()) {
	            entity.setGender(dto.getGender());
	        }
	        if (dto.getDateOfBirth() != null && !dto.getDateOfBirth().isBlank()) {
	            entity.setDateOfBirth(dto.getDateOfBirth());
	        }
	        if (dto.getAge() != null && !dto.getAge().isBlank()) {
	            entity.setAge(dto.getAge());
	        }
	        if (dto.getAddress() != null) {
	            entity.setAddress(dto.getAddress());
	        }
	        if (dto.getHospitalId() != null && !dto.getHospitalId().isBlank()) {
	            entity.setHospitalId(dto.getHospitalId());
	        }
	        if (dto.getHospitalName() != null && !dto.getHospitalName().isBlank()) {
	            entity.setHospitalName(dto.getHospitalName());
	        }
	        if (dto.getBranchId() != null && !dto.getBranchId().isBlank()) {
	            entity.setBranchId(dto.getBranchId());
	        }
	        if (dto.getCustomerId() != null && !dto.getCustomerId().isBlank()) {
	            entity.setCustomerId(dto.getCustomerId());
	        }
	        if (dto.getPatientId() != null && !dto.getPatientId().isBlank()) {
	            entity.setPatientId(dto.getPatientId());
	        }

	        entity.setUpdatedDate(LocalDate.now().toString());

	        log.debug("Saving updated customer with ID: {}", customerId);

	        onboardingRepository.save(entity);

	        log.info("Customer updated successfully with ID: {}", customerId);

	        response.setSuccess(true);
	        response.setMessage("Customer updated successfully");
	        response.setData(convertToDTO(entity));
	        response.setStatus(200);

	        log.info("Returning success response for customer update. Status: {}", response.getStatus());

	    } catch (Exception e) {

	        log.error("Exception occurred while updating customer with ID: {}", customerId, e);

	        response.setSuccess(false);
	        response.setMessage("Error updating customer: " + e.getMessage());
	        response.setStatus(500);

	        log.error("Failed to update customer with ID: {}. Response status: {}",
	                customerId, response.getStatus());
	    }

	    return response;
	}
	// ----------------- DELETE -----------------

	@Override
	@Secured("ROLE_CLINICADMIN")
	public Response deleteCustomer(String id) {

	    log.info("Request received to delete customer with ID: {}", id);

	    Response response = new Response();

	    try {

	        log.debug("Searching customer in database with ID: {}", id);

	        Optional<CustomerOnbording> optional = onboardingRepository.findByCustomerId(id);

	        if (optional.isEmpty()) {

	            log.warn("Customer not found with ID: {}", id);

	            response.setSuccess(false);
	            response.setMessage("Customer not found");
	            response.setStatus(404);
	            return response;
	        }

	        CustomerOnbording entity = optional.get();

	        log.debug("Deleting customer record with ID: {}", id);
	        onboardingRepository.deleteByCustomerId(id);

	        log.debug("Deleting customer credentials for username: {}", entity.getCustomerId());
	        credentialsRepository.deleteByUserName(entity.getCustomerId());

	        log.info("Customer and associated credentials deleted successfully. Customer ID: {}", id);

	        response.setSuccess(true);
	        response.setMessage("Customer deleted successfully");
	        response.setStatus(200);

	        log.info("Returning success response for customer deletion. Status: {}", response.getStatus());

	    } catch (Exception e) {

	        log.error("Exception occurred while deleting customer with ID: {}", id, e);

	        response.setSuccess(false);
	        response.setMessage("Error deleting customer: " + e.getMessage());
	        response.setStatus(500);

	        log.error("Failed to delete customer with ID: {}. Response status: {}", id, response.getStatus());
	    }

	    return response;
	}


	@Override
	@Secured("ROLE_CLINICADMIN")
	public Response getCustomersByHospitalId(String hospitalId, String branchId) {

	    log.info("Request received to fetch customers for Hospital ID: {} and Branch ID: {}", hospitalId, branchId);

	    Response response = new Response();

	    try {

	        log.debug("Fetching customers from database for Hospital ID: {} and Branch ID: {}", hospitalId, branchId);

	        List<CustomerOnbordingDTO> customers = onboardingRepository
	                .findByHospitalIdAndBranchId(hospitalId, branchId)
	                .stream()
	                .map(this::convertToDTO)
	                .collect(Collectors.toList());

	        log.info("Retrieved {} customer(s) for Hospital ID: {} and Branch ID: {}",
	                customers.size(), hospitalId, branchId);

	        response.setSuccess(true);
	        response.setMessage(customers.isEmpty()
	                ? "No customers found for hospitalId: " + hospitalId
	                : "Customers retrieved successfully");
	        response.setData(customers);
	        response.setStatus(200);

	        log.info("Returning response with status: {}", response.getStatus());

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching customers for Hospital ID: {} and Branch ID: {}",
	                hospitalId, branchId, e);

	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);

	        log.error("Failed to fetch customers. Response status: {}", response.getStatus());
	    }

	    return response;
	}
	

	@Override
	@Secured("ROLE_CLINICADMIN")
	public Response getCustomersByPatientId(String patientId, String clinicId) {

	    log.info("Request received to fetch customer with Patient ID: {} and Clinic ID: {}", patientId, clinicId);

	    Response response = new Response();

	    try {

	        log.debug("Searching customer in database with Patient ID: {} and Clinic ID: {}",
	                patientId, clinicId);

	        CustomerOnbording customer =
	                onboardingRepository.findByPatientIdAndHospitalId(patientId, clinicId);

	        if (customer != null) {

	            log.info("Customer found with Patient ID: {} and Clinic ID: {}",
	                    patientId, clinicId);

	            response.setSuccess(true);
	            response.setMessage("Customers retrieved successfully");
	            response.setData(new ObjectMapper().convertValue(customer, CustomerOnbordingDTO.class));
	            response.setStatus(200);

	            log.info("Returning customer details successfully. Status: {}", response.getStatus());

	        } else {

	            log.warn("No customer found with Patient ID: {} and Clinic ID: {}",
	                    patientId, clinicId);

	            response.setSuccess(false);
	            response.setMessage("Customers Object Not Found");
	            response.setStatus(200);
	        }

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching customer with Patient ID: {} and Clinic ID: {}",
	                patientId, clinicId, e);

	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);

	        log.error("Failed to fetch customer. Response status: {}", response.getStatus());
	    }

	    return response;
	}

	
	
	

	@Override
	@Secured("ROLE_CLINICADMIN")
	public Response getCustomersByBranchId(String branchId) {

	    log.info("Request received to fetch customers for Branch ID: {}", branchId);

	    Response response = new Response();

	    try {

	        log.debug("Fetching customers from database for Branch ID: {}", branchId);

	        List<CustomerOnbordingDTO> customers = onboardingRepository.findByBranchId(branchId)
	                .stream()
	                .map(this::convertToDTO)
	                .collect(Collectors.toList());

	        log.info("Retrieved {} customer(s) for Branch ID: {}", customers.size(), branchId);

	        response.setSuccess(true);
	        response.setMessage(customers.isEmpty()
	                ? "No customers found for branchId: " + branchId
	                : "Customers retrieved successfully");
	        response.setData(customers);
	        response.setStatus(200);

	        log.info("Returning response with status: {}", response.getStatus());

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching customers for Branch ID: {}", branchId, e);

	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);

	        log.error("Failed to fetch customers for Branch ID: {}. Response status: {}",
	                branchId, response.getStatus());
	    }

	    return response;
	}


	@Override
	@Secured("ROLE_CLINICADMIN")
	public Response getCustomersByHospitalIdAndBranchId(String hospitalId, String branchId) {

	    log.info("Request received to fetch customers for Hospital ID: {} and Branch ID: {}",
	            hospitalId, branchId);

	    Response response = new Response();

	    try {

	        log.debug("Fetching customers from database for Hospital ID: {} and Branch ID: {}",
	                hospitalId, branchId);

	        List<CustomerOnbordingDTO> customers = onboardingRepository
	                .findByHospitalIdAndBranchId(hospitalId, branchId)
	                .stream()
	                .map(this::convertToDTO)
	                .collect(Collectors.toList());

	        log.info("Retrieved {} customer(s) for Hospital ID: {} and Branch ID: {}",
	                customers.size(), hospitalId, branchId);

	        response.setSuccess(true);
	        response.setMessage(customers.isEmpty()
	                ? "No customers found for hospitalId: " + hospitalId + " and branchId: " + branchId
	                : "Customers retrieved successfully");
	        response.setData(customers);
	        response.setStatus(200);

	        log.info("Returning response with status: {}", response.getStatus());

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching customers for Hospital ID: {} and Branch ID: {}",
	                hospitalId, branchId, e);

	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);

	        log.error("Failed to fetch customers for Hospital ID: {} and Branch ID: {}. Response status: {}",
	                hospitalId, branchId, response.getStatus());
	    }

	    return response;
	}

	
	
	// ----------------- LOGIN -----------------
//	@Override
//	public Response login(CustomerLoginDTO dto) {
//		Response response = new Response();
//
//		try {
//			Optional<CustomerCredentials> optional = credentialsRepository.findByUserName(dto.getUserName());
//			if (optional.isEmpty()) {
//				response.setSuccess(false);
//				response.setMessage("Invalid username");
//				response.setStatus(401);
//				return response;
//			}
//
//			CustomerCredentials credentials = optional.get();
//
//			// check password
//			if (!passwordEncoder.matches(dto.getPassword(), credentials.getPassword())) {
//				response.setSuccess(false);
//				response.setMessage("Invalid password");
//				response.setStatus(401);
//				return response;
//			}
//
//			// fetch customer onboarding details using userName (or customerId if you store
//			// it in credentials)
//			Optional<CustomerOnbording> customerOpt = onboardingRepository.findByCustomerId(credentials.getUserName());
//
//			if (customerOpt.isEmpty()) {
//				response.setSuccess(false);
//				response.setMessage("Customer profile not found");
//				response.setStatus(404);
//				return response;
//			}
//
//			CustomerOnbording customer = customerOpt.get();
//
//			customer.setDeviceId(dto.getDeviceId());
//			CustomerOnbording cs = onboardingRepository.save(customer);
//
//			// map to response DTO
//			CustomerResponseDTO resDTO = new CustomerResponseDTO();
//			resDTO.setUserName(credentials.getUserName());
//			resDTO.setCustomerName(customer.getFullName());
//			resDTO.setCustomerId(customer.getCustomerId());
//			resDTO.setPatientId(customer.getPatientId());
//			resDTO.setDeviceId(cs.getDeviceId());
//			resDTO.setHospitalName(customer.getHospitalName());
//			resDTO.setHospitalId(customer.getHospitalId());
//			resDTO.setBranchId(customer.getBranchId());
//
//			// final response
//			response.setSuccess(true);
//			response.setMessage("Login successful");
//			response.setData(resDTO);
//			response.setStatus(200);
//
//		} catch (Exception e) {
//			response.setSuccess(false);
//			response.setMessage("Login error: " + e.getMessage());
//			response.setStatus(500);
//		}
//
//		return response;
//	}


	@Override
	@Secured({"ROLE_CLINICADMIN", "ROLE_NOTIFICATIONSERVICE"})
	public String customerDeviceId(String customerId) {

	    log.info("Request received to fetch device ID for Customer ID: {}", customerId);

	    try {

	        log.debug("Searching customer credentials for Customer ID: {}", customerId);

	        Optional<CustomerCredentials> cs = credentialsRepository.findByUserName(customerId);

	        if (cs.isPresent()) {

	            log.info("Device ID found for Customer ID: {}", customerId);
	            return cs.get().getDeviceId();

	        } else {

	            log.warn("Customer credentials not found for Customer ID: {}", customerId);
	            return null;
	        }

	    } catch (Exception e) {

	        log.error("Exception occurred while fetching device ID for Customer ID: {}", customerId, e);
	        return null;
	    }
	}
	// ----------------- RESET PASSWORD -----------------
//	@Override
//	public Response resetPassword(ChangeDoctorPasswordDTO dto) {
//		Response response = new Response();
//
//		try {
//			Optional<CustomerCredentials> optional = credentialsRepository.findByUserName(dto.getUserName());
//			if (optional.isEmpty()) {
//				response.setSuccess(false);
//				response.setMessage("Invalid username");
//				response.setStatus(404);
//				return response;
//			}
//
//			CustomerCredentials credentials = optional.get();
//
//			if (!passwordEncoder.matches(dto.getCurrentPassword(), credentials.getPassword())) {
//				response.setSuccess(false);
//				response.setMessage("Old password is incorrect");
//				response.setStatus(400);
//				return response;
//			}
//
//			credentials.setPassword(passwordEncoder.encode(newPassword));
//			credentialsRepository.save(credentials);
//
//			response.setSuccess(true);
//			response.setMessage("Password updated successfully");
//			response.setStatus(200);
//
//		} catch (Exception e) {
//			response.setSuccess(false);
//			response.setMessage("Reset password error: " + e.getMessage());
//			response.setStatus(500);
//		}
//
//		return response;
//	}

	// ------------------ DTO ↔ Entity Conversion ------------------
	private CustomerOnbording convertToEntity(CustomerOnbordingDTO dto) {
		CustomerOnbording entity = new CustomerOnbording();
		entity.setId(dto.getId());
		entity.setMobileNumber(dto.getMobileNumber());
		entity.setEmail(dto.getEmail());
		entity.setFullName(dto.getFullName());
		entity.setDateOfBirth(dto.getDateOfBirth());
		entity.setGender(dto.getGender());
		entity.setAge(dto.getAge());
		entity.setAddress(dto.getAddress());
		entity.setHospitalId(dto.getHospitalId());
		entity.setHospitalName(dto.getHospitalName());
		entity.setBranchId(dto.getBranchId());
		entity.setCustomerId(dto.getCustomerId());
		entity.setPatientId(dto.getPatientId());
		entity.setDeviceId(dto.getDeviceId());
//		entity.setReferralCode(dto.getReferralCode());
		entity.setReferredBy(dto.getReferredBy());
		entity.setCreatedBy(dto.getCreatedBy());
		entity.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toString());
		return entity;
	}

	private CustomerOnbordingDTO convertToDTO(CustomerOnbording entity) {
		CustomerOnbordingDTO dto = new CustomerOnbordingDTO();
		dto.setId(entity.getId());
		dto.setMobileNumber(entity.getMobileNumber());
		dto.setEmail(entity.getEmail());
		dto.setFullName(entity.getFullName());
		dto.setGender(entity.getGender());
		dto.setDateOfBirth(entity.getDateOfBirth());
		dto.setAge(entity.getAge());
		dto.setAddress(entity.getAddress());
		dto.setHospitalId(entity.getHospitalId());
		dto.setHospitalName(entity.getHospitalName());
		dto.setBranchId(entity.getBranchId());
		dto.setCustomerId(entity.getCustomerId());
		dto.setPatientId(entity.getPatientId());
		dto.setDeviceId(entity.getDeviceId());
		dto.setReferralCode(entity.getReferralCode());
		dto.setReferredBy(entity.getReferredBy());
		dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedDate(entity.getUpdatedDate());
		return dto;
	}
	

	@Override
	@Secured({"ROLE_CLINICADMIN", "ROLE_NOTIFICATIONSERVICE"})
	public CustomerOnbordingDTO getCustomerByToken(String token) {

	    log.info("Request received to fetch customer using device token.");

	    try {

	        log.debug("Searching customer in database using device token.");

	        CustomerOnbording customer = onboardingRepository.findByDeviceId(token);

	        if (customer != null) {

	            log.info("Customer found for the provided device token.");

	            CustomerOnbordingDTO customerDTO =
	                    new ObjectMapper().convertValue(customer, CustomerOnbordingDTO.class);

	            log.info("Returning customer details successfully.");

	            return customerDTO;

	        } else {

	            log.warn("No customer found for the provided device token.");
	            return null;
	        }

	    } catch (FeignException e) {

	        log.error("FeignException occurred while fetching customer using device token.", e);
	        return null;

	    } catch (Exception e) {

	        log.error("Unexpected exception occurred while fetching customer using device token.", e);
	        return null;
	    }
	}
	
	

	@Override
	@Secured("ROLE_CLINICADMIN")
	public List<BookingInfoByInput> bookingByInput(String input, String clinicId) {

	    log.info("Request received to search booking details. Input: {}, Clinic ID: {}", input, clinicId);

	    BookingInfoByInput bkng = new BookingInfoByInput();
	    CustomerOnbordingDTO customer = null;
	    List<BookingInfoByInput> result = new ArrayList<>();
	    List<CustomerOnbordingDTO> customers = null;

	    try {

	        // Search by Mobile Number
	        log.debug("Searching customer by mobile number.");
	        customer = getCustomerByMobileNumberAndClinicId(input, clinicId);

	        if (customer != null) {

	            log.info("Customer found using mobile number.");

	            bkng.setAge(customer.getAge());
	            bkng.setClinicId(customer.getHospitalId());
	            bkng.setCustomerId(customer.getCustomerId());
	            bkng.setGender(customer.getGender());
	            bkng.setMobileNumber(customer.getMobileNumber());
	            bkng.setName(customer.getFullName());
	            bkng.setPatientAddress(customer.getAddress());
	            bkng.setPatientId(customer.getPatientId());
	            bkng.setPatientMobileNumber(customer.getMobileNumber());
	            bkng.setDob(customer.getDateOfBirth());
	            bkng.setRelation(null);

	            result.add(bkng);
	        }

	        // Search by Patient ID
	        if (customer == null) {

	            log.debug("Customer not found by mobile number. Searching by Patient ID.");

	            Response res = getCustomersByPatientId(input, clinicId);
	            customer = new ObjectMapper().convertValue(res.getData(), CustomerOnbordingDTO.class);

	            if (customer != null) {

	                log.info("Customer found using Patient ID.");

	                bkng.setAge(customer.getAge());
	                bkng.setClinicId(customer.getHospitalId());
	                bkng.setCustomerId(customer.getCustomerId());
	                bkng.setGender(customer.getGender());
	                bkng.setMobileNumber(customer.getMobileNumber());
	                bkng.setName(customer.getFullName());
	                bkng.setPatientAddress(customer.getAddress());
	                bkng.setPatientId(customer.getPatientId());
	                bkng.setPatientMobileNumber(customer.getMobileNumber());
	                bkng.setDob(customer.getDateOfBirth());
	                bkng.setRelation(null);

	                result.add(bkng);
	            }
	        }

	        // Search by Name
	        if (customer == null) {

	            log.debug("Customer not found by Patient ID. Searching by customer name.");

	            customers = onboardingRepository
	                    .findByFullNameContainingIgnoreCaseAndHospitalId(input, clinicId);

	            log.info("Found {} customer(s) using customer name.", customers.size());

	            for (CustomerOnbordingDTO dto : customers) {

	                BookingInfoByInput bookingInfo = new BookingInfoByInput();

	                bookingInfo.setAge(dto.getAge());
	                bookingInfo.setClinicId(dto.getHospitalId());
	                bookingInfo.setCustomerId(dto.getCustomerId());
	                bookingInfo.setGender(dto.getGender());
	                bookingInfo.setMobileNumber(dto.getMobileNumber());
	                bookingInfo.setName(dto.getFullName());
	                bookingInfo.setPatientAddress(dto.getAddress());
	                bookingInfo.setPatientId(dto.getPatientId());
	                bookingInfo.setPatientMobileNumber(dto.getMobileNumber());
	                bookingInfo.setDob(dto.getDateOfBirth());
	                bookingInfo.setRelation(null);

	                result.add(bookingInfo);
	            }
	        }

	        log.info("Booking search completed successfully. Total records found: {}", result.size());

	    } catch (Exception e) {

	        log.error("Exception occurred while searching booking details. Input: {}, Clinic ID: {}",
	                input, clinicId, e);
	    }

	    return result;
	}
}
