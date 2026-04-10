package com.clinicadmin.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
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

@Service
public class CustomerOnboardingServiceImpl implements CustomerOnboardingService {

	@Autowired
	private CustomerOnboardingRepository onboardingRepository;

	@Autowired
	private CustomerCredentialsRepository credentialsRepository;

	@Autowired
	private SequenceGeneratorService sequenceGeneratorService;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	// ----------------- CREATE (ONBOARD) -----------------
	@Override
	public Response onboardCustomer(CustomerOnbordingDTO dto) {
		Response response = new Response();

		try {
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
	public Response getAllCustomers() {
		Response response = new Response();
		try {
			List<CustomerOnbordingDTO> customers = onboardingRepository.findAll().stream().map(this::convertToDTO)
					.collect(Collectors.toList());

			response.setSuccess(true);
			response.setMessage(customers.isEmpty() ? "No customers found" : "Customers retrieved successfully");
			response.setData(customers);
			response.setStatus(200);
		} catch (Exception e) {
			response.setSuccess(false);
			response.setMessage("Error fetching customers: " + e.getMessage());
			response.setStatus(500);
		}
		return response;
	}

	@Override
	public Response getCustomerById(String id) {
		Response response = new Response();
		try {
			Optional<CustomerOnbording> optional = onboardingRepository.findByCustomerId(id);
			if (optional.isPresent()) {
				response.setSuccess(true);
				response.setMessage("Customer found successfully");
				response.setData(convertToDTO(optional.get()));
				response.setStatus(200);
			} else {
				response.setSuccess(false);
				response.setMessage("Customer not found with ID: " + id);
				response.setStatus(404);
			}
		} catch (Exception e) {
			response.setSuccess(false);
			response.setMessage("Error fetching customer: " + e.getMessage());
			response.setStatus(500);
		}
		return response;
	}

	
	@Override
	public Response getCustomerByMobiileNumber(String mobilenumber) {
		Response response = new Response();
		try {
			Optional<CustomerOnbording> optional = onboardingRepository.findByMobileNumber(mobilenumber);
			if (optional.isPresent()) {
				response.setSuccess(true);
				response.setMessage("Customer found successfully");
				response.setData(convertToDTO(optional.get()));
				response.setStatus(200);
			} else {
				response.setSuccess(false);
				response.setMessage("Customer not found with ID: " + mobilenumber);
				response.setStatus(404);
			}
		} catch (Exception e) {
			response.setSuccess(false);
			response.setMessage("Error fetching customer: " + e.getMessage());
			response.setStatus(500);
		}
		return response;
	}
	// ----------------- UPDATE -----------------
	@Override
	public Response updateCustomer(String customerId, CustomerOnbordingDTO dto) {
		Response response = new Response();

		try {
			Optional<CustomerOnbording> optional = onboardingRepository.findByCustomerId(customerId);
			if (optional.isEmpty()) {
				response.setSuccess(false);
				response.setMessage("Customer not found");
				response.setStatus(404);
				return response;
			}

			CustomerOnbording entity = optional.get();

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

			onboardingRepository.save(entity);

			response.setSuccess(true);
			response.setMessage("Customer updated successfully");
			response.setData(convertToDTO(entity));
			response.setStatus(200);

		} catch (Exception e) {
			response.setSuccess(false);
			response.setMessage("Error updating customer: " + e.getMessage());
			response.setStatus(500);
		}

		return response;
	}

	// ----------------- DELETE -----------------
	@Override
	public Response deleteCustomer(String id) {
		Response response = new Response();

		try {
			Optional<CustomerOnbording> optional = onboardingRepository.findByCustomerId(id);
			if (optional.isEmpty()) {
				response.setSuccess(false);
				response.setMessage("Customer not found");
				response.setStatus(404);
				return response;
			}

			CustomerOnbording entity = optional.get();
			onboardingRepository.deleteByCustomerId(id);

			// Delete credentials also
			credentialsRepository.deleteByUserName(entity.getCustomerId());

			response.setSuccess(true);
			response.setMessage("Customer deleted successfully");
			response.setStatus(200);

		} catch (Exception e) {
			response.setSuccess(false);
			response.setMessage("Error deleting customer: " + e.getMessage());
			response.setStatus(500);
		}

		return response;
	}

	@Override
	public Response getCustomersByHospitalId(String hospitalId) {
	    Response response = new Response();
	    try {
	        List<CustomerOnbordingDTO> customers = onboardingRepository.findByHospitalId(hospitalId)
	                .stream()
	                .map(this::convertToDTO)
	                .collect(Collectors.toList());

	        response.setSuccess(true);
	        response.setMessage(customers.isEmpty() ? "No customers found for hospitalId: " + hospitalId : "Customers retrieved successfully");
	        response.setData(customers);
	        response.setStatus(200);
	    } catch (Exception e) {
	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);
	    }
	    return response;
	}

	
	@Override
	public Response getCustomersByPatientId(String patientId,String clinicId) {
	    Response response = new Response();
	    try {
	        CustomerOnbording customers = onboardingRepository.findByPatientIdAndBranchId(patientId,clinicId);
	        if(customers != null) {      
	        response.setSuccess(true);
	        response.setMessage("Customers retrieved successfully");
	        response.setData(new ObjectMapper().convertValue(customers,CustomerOnbordingDTO.class ));
	        response.setStatus(200);
	    }else {
	    	 response.setSuccess(false);
		        response.setMessage("Customers Object Not Found");
		        response.setStatus(200);
	    }}catch(Exception e) {
	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);
	    }
	    return response;
	}

	
	
	
	@Override
	public Response getCustomersByBranchId(String branchId) {
	    Response response = new Response();
	    try {
	        List<CustomerOnbordingDTO> customers = onboardingRepository.findByBranchId(branchId)
	                .stream()
	                .map(this::convertToDTO)
	                .collect(Collectors.toList());

	        response.setSuccess(true);
	        response.setMessage(customers.isEmpty() ? "No customers found for branchId: " + branchId : "Customers retrieved successfully");
	        response.setData(customers);
	        response.setStatus(200);
	    } catch (Exception e) {
	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);
	    }
	    return response;
	}

	@Override
	public Response getCustomersByHospitalIdAndBranchId(String hospitalId, String branchId) {
	    Response response = new Response();
	    try {
	        List<CustomerOnbordingDTO> customers = onboardingRepository.findByHospitalIdAndBranchId(hospitalId, branchId)
	                .stream()
	                .map(this::convertToDTO)
	                .collect(Collectors.toList());

	        response.setSuccess(true);
	        response.setMessage(customers.isEmpty() ? 
	            "No customers found for hospitalId: " + hospitalId + " and branchId: " + branchId 
	            : "Customers retrieved successfully");
	        response.setData(customers);
	        response.setStatus(200);
	    } catch (Exception e) {
	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);
	    }
	    return response;
	}

	
	
	// ----------------- LOGIN -----------------
	@Override
	public Response login(CustomerLoginDTO dto) {
		Response response = new Response();

		try {
			Optional<CustomerCredentials> optional = credentialsRepository.findByUserName(dto.getUserName());
			if (optional.isEmpty()) {
				response.setSuccess(false);
				response.setMessage("Invalid username");
				response.setStatus(401);
				return response;
			}

			CustomerCredentials credentials = optional.get();

			// check password
			if (!passwordEncoder.matches(dto.getPassword(), credentials.getPassword())) {
				response.setSuccess(false);
				response.setMessage("Invalid password");
				response.setStatus(401);
				return response;
			}

			// fetch customer onboarding details using userName (or customerId if you store
			// it in credentials)
			Optional<CustomerOnbording> customerOpt = onboardingRepository.findByCustomerId(credentials.getUserName());

			if (customerOpt.isEmpty()) {
				response.setSuccess(false);
				response.setMessage("Customer profile not found");
				response.setStatus(404);
				return response;
			}

			CustomerOnbording customer = customerOpt.get();

			customer.setDeviceId(dto.getDeviceId());
			CustomerOnbording cs = onboardingRepository.save(customer);

			// map to response DTO
			CustomerResponseDTO resDTO = new CustomerResponseDTO();
			resDTO.setUserName(credentials.getUserName());
			resDTO.setCustomerName(customer.getFullName());
			resDTO.setCustomerId(customer.getCustomerId());
			resDTO.setPatientId(customer.getPatientId());
			resDTO.setDeviceId(cs.getDeviceId());
			resDTO.setHospitalName(customer.getHospitalName());
			resDTO.setHospitalId(customer.getHospitalId());
			resDTO.setBranchId(customer.getBranchId());

			// final response
			response.setSuccess(true);
			response.setMessage("Login successful");
			response.setData(resDTO);
			response.setStatus(200);

		} catch (Exception e) {
			response.setSuccess(false);
			response.setMessage("Login error: " + e.getMessage());
			response.setStatus(500);
		}

		return response;
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
	
	
	public CustomerOnbordingDTO getCustomerByToken(String token){
		try {	
			CustomerOnbording cstmr = onboardingRepository.findByDeviceId(token);
			if(cstmr != null) {
		    CustomerOnbordingDTO cusmrdto = new ObjectMapper().convertValue(cstmr, CustomerOnbordingDTO.class);
			return cusmrdto;}
			else {
				return null;
			}
		}catch(FeignException e) {	
			return null;	
		}}
}
