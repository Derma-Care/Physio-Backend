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
import org.springframework.cache.annotation.Cacheable;
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
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
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
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "onboardCustomerFallback")
	public Response onboardCustomer(CustomerOnbordingDTO dto) {
		log.info("Entering onboardCustomer");
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
			log.error("Exception in method", e);
			response.setSuccess(false);
			response.setMessage("Error during onboarding: " + e.getMessage());
			response.setStatus(500);
		}

		return response;
	}

	// ----------------- READ ALL -----------------
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllCustomersFallback")
	public Response getAllCustomers() {
		log.info("Entering getAllCustomers");
		Response response = new Response();
		try {
			List<CustomerOnbordingDTO> customers = onboardingRepository.findAll().stream().map(this::convertToDTO)
					.collect(Collectors.toList());

			response.setSuccess(true);
			response.setMessage(customers.isEmpty() ? "No customers found" : "Customers retrieved successfully");
			response.setData(customers);
			response.setStatus(200);
		} catch (Exception e) {
			log.error("Exception in method", e);
			response.setSuccess(false);
			response.setMessage("Error fetching customers: " + e.getMessage());
			response.setStatus(500);
		}
		return response;
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getCustomerByIdFallback")
	public Response getCustomerById(String id) {
		log.info("Entering getCustomerById");
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
			log.error("Exception in method", e);
			response.setSuccess(false);
			response.setMessage("Error fetching customer: " + e.getMessage());
			response.setStatus(500);
		}
		return response;
	}

	
	@Override
	 @Secured({"ROLE_CLINICADMIN","ROLE_BOOKINGSERVICE"})
	@Cacheable(value = "customers",key = "#mobilenumber+ '_' +name")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "CustomerByMobilenumberAndNameRateLimitFallback")
	public Map<String,String> getCustomerByMobilenumberAndName(String mobilenumber,String name) {
		log.info("Entering getCustomerByMobilenumberAndName");		
		Map<String,String> details = new LinkedHashMap<>();
		try {
			Optional<CustomerOnbording> optional = onboardingRepository.findByMobileNumberAndFullName(mobilenumber, name);
			if (optional.isPresent()) {
				details.put("customerId", optional.get().getCustomerId());
				details.put("patientId", optional.get().getPatientId());
				////System.out.println(details); 
				return details;
			} else {
				return null;
			}
		} catch (Exception e) {
			log.error("Exception in method", e);
			return null;
		}
		
	}

	
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getCustomerByMobiileNumberFallback")
	public Response getCustomerByMobiileNumber(String mobilenumber) {
		log.info("Entering getCustomerByMobiileNumber");
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
			log.error("Exception in method", e);
			response.setSuccess(false);
			response.setMessage("Error fetching customer: " + e.getMessage());
			response.setStatus(500);
		}
		return response;
	}
	
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getCustomerByMobileNumberAndClinicIdFallback")
	public CustomerOnbordingDTO getCustomerByMobileNumberAndClinicId(String mobilenumber,String clinicId) {
		log.info("Entering getCustomerByMobileNumberAndClinicId");	
		try {
			CustomerOnbording optional = onboardingRepository.findByMobileNumberAndHospitalId(mobilenumber,clinicId);
			//System.out.println(optional);
			if (optional != null) {
				return convertToDTO(optional);
			} else {
				return null;
			}
		} catch (Exception e) {
			log.error("Exception in method", e);
			return null;
		}}
	
	
	
	// ----------------- UPDATE -----------------
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updateCustomerFallback")
	public Response updateCustomer(String customerId, CustomerOnbordingDTO dto) {
		log.info("Entering updateCustomer");
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
			log.error("Exception in method", e);
			response.setSuccess(false);
			response.setMessage("Error updating customer: " + e.getMessage());
			response.setStatus(500);
		}

		return response;
	}

	// ----------------- DELETE -----------------
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteCustomerFallback")
	public Response deleteCustomer(String id) {
		log.info("Entering deleteCustomer");
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
			log.error("Exception in method", e);
			response.setSuccess(false);
			response.setMessage("Error deleting customer: " + e.getMessage());
			response.setStatus(500);
		}

		return response;
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getCustomersByHospitalIdFallback")
	public Response getCustomersByHospitalId(String hospitalId,String branchId) {
		log.info("Entering getCustomersByHospitalId");
	    Response response = new Response();
	    try {
	        List<CustomerOnbordingDTO> customers = onboardingRepository.findByHospitalIdAndBranchId(hospitalId, branchId)
	                .stream()
	                .map(this::convertToDTO)
	                .collect(Collectors.toList());

	        response.setSuccess(true);
	        response.setMessage(customers.isEmpty() ? "No customers found for hospitalId: " + hospitalId : "Customers retrieved successfully");
	        response.setData(customers);
	        response.setStatus(200);
	    } catch (Exception e) {
			log.error("Exception in method", e);
	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);
	    }
	    return response;
	}

	
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getCustomersByPatientIdFallback")
	public Response getCustomersByPatientId(String patientId,String clinicId) {
		log.info("Entering getCustomersByPatientId");
	    Response response = new Response();
	    try {
	        CustomerOnbording customers = onboardingRepository.findByPatientIdAndHospitalId(patientId,clinicId);
	      //  System.out.println(customers);
	        if(customers != null) {      
	        response.setSuccess(true);
	        response.setMessage("Customers retrieved successfully");
	        response.setData(new ObjectMapper().convertValue(customers,CustomerOnbordingDTO.class ));
	        response.setStatus(200);
	    }else {
	    	 response.setSuccess(false);
		        response.setMessage("Customers Object Not Found");
		        response.setStatus(200);
	    }}catch (Exception e) {
			log.error("Exception in method", e);
	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);
	    }
	    return response;
	}

	
	
	
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getCustomersByBranchIdFallback")
	public Response getCustomersByBranchId(String branchId) {
		log.info("Entering getCustomersByBranchId");
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
			log.error("Exception in method", e);
	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);
	    }
	    return response;
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getCustomersByHospitalIdAndBranchIdFallback")
	public Response getCustomersByHospitalIdAndBranchId(String hospitalId, String branchId) {
		log.info("Entering getCustomersByHospitalIdAndBranchId");
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
			log.error("Exception in method", e);
	        response.setSuccess(false);
	        response.setMessage("Error fetching customers: " + e.getMessage());
	        response.setStatus(500);
	    }
	    return response;
	}

	
	


	 @Secured({"ROLE_CLINICADMIN","ROLE_NOTIFICATIONSERVICE"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "customerDeviceIdFallback")
	public String customerDeviceId(String customerId) {
		log.info("Entering customerDeviceId");
		try {
			Optional<CustomerCredentials> cs = credentialsRepository.findByUserName(customerId);	
			if(cs.isPresent()) {
				return cs.get().getDeviceId();
			}else {
				return null;
			}
		}catch (Exception e) {
			log.error("Exception in method", e);
			return null;
		}
	}

	// ----------------- RESET PASSWORD -----------------
//	@Override
//	public Response resetPassword(ChangeDoctorPasswordDTO dto) {
		///log.info("Entering resetPassword");
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
		//	log.error("Exception in method", e);
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
	
	 @Secured({"ROLE_CLINICADMIN","ROLE_NOTIFICATIONSERVICE"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getCustomerByTokenFallback")
	public CustomerOnbordingDTO getCustomerByToken(String token){
		log.info("Entering getCustomerByToken");
		try {	
			CustomerOnbording cstmr = onboardingRepository.findByDeviceId(token);
			if(cstmr != null) {
		    CustomerOnbordingDTO cusmrdto = new ObjectMapper().convertValue(cstmr, CustomerOnbordingDTO.class);
			return cusmrdto;}
			else {
				return null;
			}
		}catch (FeignException e) {
			log.error("Exception in method", e);	
			return null;	
		}}
	
	
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "bookingByInputFallback")
	public List<BookingInfoByInput> bookingByInput(String input,String clinicId) {
		log.info("Entering bookingByInput");
		   BookingInfoByInput bkng = new BookingInfoByInput();
		   CustomerOnbordingDTO b = null;
		   List<BookingInfoByInput> lst = new ArrayList<>();
		   List<CustomerOnbordingDTO> customerOnbordingDTO = null;
	       try {	        	
	        	b = getCustomerByMobileNumberAndClinicId(input,clinicId);
	   		   //Sysout
	        	if(b != null) {
	        	bkng.setAge(b.getAge());
		        bkng.setClinicId(b.getHospitalId());
		        bkng.setCustomerId(b.getCustomerId());
		        bkng.setGender(b.getGender());
		        bkng.setMobileNumber(b.getMobileNumber());
		        bkng.setName(b.getFullName());
		        bkng.setPatientAddress(b.getAddress());
		        bkng.setPatientId(b.getPatientId());
		        bkng.setPatientMobileNumber(b.getMobileNumber());
		        bkng.setDob(b.getDateOfBirth());
		        bkng.setRelation(null);	
		        lst.add(bkng);}	       
		    	if(b == null){
	        	 Response res = getCustomersByPatientId(input,clinicId);			   
			      b = new ObjectMapper().convertValue(res.getData(), CustomerOnbordingDTO.class);		    	     
			      if(b != null) {
			        bkng.setAge(b.getAge());
			        bkng.setClinicId(b.getHospitalId());
			        bkng.setCustomerId(b.getCustomerId());
			        bkng.setGender(b.getGender());
			        bkng.setMobileNumber(b.getMobileNumber());
			        bkng.setName(b.getFullName());
			        bkng.setPatientAddress(b.getAddress());
			        bkng.setPatientId(b.getPatientId());
			        bkng.setPatientMobileNumber(b.getMobileNumber());
			        bkng.setDob(b.getDateOfBirth());
			        bkng.setRelation(null);
			        lst.add(bkng);}		       
		        }if(b == null){	
		        customerOnbordingDTO = onboardingRepository.findByFullNameContainingIgnoreCaseAndHospitalId(input,clinicId);
		        ///System.out.println(customerOnbordingDTO);
		        for(CustomerOnbordingDTO dto : customerOnbordingDTO) {
		        BookingInfoByInput bookingInfoByInput = new BookingInfoByInput();
		        bookingInfoByInput.setAge(dto.getAge());
		        bookingInfoByInput.setClinicId(dto.getHospitalId());
		        bookingInfoByInput.setCustomerId(dto.getCustomerId());
		        bookingInfoByInput.setGender(dto.getGender());
		        bookingInfoByInput.setMobileNumber(dto.getMobileNumber());
		        bookingInfoByInput.setName(dto.getFullName());
		        bookingInfoByInput.setPatientAddress(dto.getAddress());
		        bookingInfoByInput.setPatientId(dto.getPatientId());
		        bookingInfoByInput.setPatientMobileNumber(dto.getMobileNumber());
		        bookingInfoByInput.setDob(dto.getDateOfBirth());
		        bookingInfoByInput.setRelation(null);
		        lst.add(bookingInfoByInput);}}
	       }catch (Exception e) {
			log.error("Exception in method", e);
	        //System.err.println("Error fetching bookings: " + e.getMessage());
	        System.out.println(e.getMessage());; // safe fallback
	    }
	    return lst;
	}



    // Generated fallbacks
    private Response buildRateLimitResponse(){
      Response r=new Response();
      r.setSuccess(false);r.setStatus(429);r.setMessage("Too many requests. Please try again later.");return r;
    }

    public Response onboardCustomerFallback(CustomerOnbordingDTO dto, Exception ex){log.error("Rate limit triggered in onboardCustomer", ex); return buildRateLimitResponse();}

    public Response getAllCustomersFallback(Exception ex){log.error("Rate limit triggered in getAllCustomers", ex); return buildRateLimitResponse();}

    public Response getCustomerByIdFallback(String id, Exception ex){log.error("Rate limit triggered in getCustomerById", ex); return buildRateLimitResponse();}

    public Response getCustomerByMobiileNumberFallback(String mobilenumber, Exception ex){log.error("Rate limit triggered in getCustomerByMobiileNumber", ex); return buildRateLimitResponse();}

    public CustomerOnbordingDTO getCustomerByMobileNumberAndClinicIdFallback(String mobilenumber,String clinicId, Exception ex){log.error("Rate limit triggered in getCustomerByMobileNumberAndClinicId", ex); return null;}

    public Response updateCustomerFallback(String customerId, CustomerOnbordingDTO dto, Exception ex){log.error("Rate limit triggered in updateCustomer", ex); return buildRateLimitResponse();}

    public Response deleteCustomerFallback(String id, Exception ex){log.error("Rate limit triggered in deleteCustomer", ex); return buildRateLimitResponse();}

    public Response getCustomersByHospitalIdFallback(String hospitalId,String branchId, Exception ex){log.error("Rate limit triggered in getCustomersByHospitalId", ex); return buildRateLimitResponse();}

    public Response getCustomersByPatientIdFallback(String patientId,String clinicId, Exception ex){log.error("Rate limit triggered in getCustomersByPatientId", ex); return buildRateLimitResponse();}

    public Response getCustomersByBranchIdFallback(String branchId, Exception ex){log.error("Rate limit triggered in getCustomersByBranchId", ex); return buildRateLimitResponse();}

    public Response getCustomersByHospitalIdAndBranchIdFallback(String hospitalId, String branchId, Exception ex){log.error("Rate limit triggered in getCustomersByHospitalIdAndBranchId", ex); return buildRateLimitResponse();}

    public String customerDeviceIdFallback(String customerId, Exception ex){log.error("Rate limit triggered in customerDeviceId", ex); return null;}

    public CustomerOnbordingDTO getCustomerByTokenFallback(String token, Exception ex){log.error("Rate limit triggered in getCustomerByToken", ex); return null;}

    public List<BookingInfoByInput> bookingByInputFallback(String input,String clinicId, Exception ex){log.error("Rate limit triggered in bookingByInput", ex); return null;}

}