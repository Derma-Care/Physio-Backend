package com.clinicadmin.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.CustomConsentFormDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
//import com.clinicadmin.dto.SubServicesDto;
import com.clinicadmin.entity.CustomConsentForm;
//import com.clinicadmin.feignclient.ServiceFeignClient;
import com.clinicadmin.repository.CustomConsentFormRepository;
import com.clinicadmin.service.CustomConsentFormService;

import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Service
@Slf4j
public class CustomConsentFormServiceImpl implements CustomConsentFormService {

	@Autowired
	private CustomConsentFormRepository customConsentFormRepository;

	// ------------------------------- Get Consent Form
	// -------------------------------
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getConsentFormFallback")
	public Response getConsentForm(String hospitalId, String consentFormType) {
		log.info("Fetching consent form hospitalId={} consentFormType={}", hospitalId, consentFormType);
		if (hospitalId == null || hospitalId.trim().isEmpty()) {
			return buildErrorResponse("Hospital ID cannot be null or empty", 400);
		}
		if (!isValidConsentFormType(consentFormType)) {
			return buildErrorResponse("Invalid Consent Form Type. Allowed values: 1 (Generic), 2 (Procedure)", 400);
		}

		try {
			if (consentFormType.equals("1")) {
			    CustomConsentForm form = customConsentFormRepository
			            .findByHospitalIdAndConsentFormType(hospitalId, "1")
			            .orElse(null);

			    if (form == null) {
			        return buildErrorResponse("Generic Consent Form not found for this hospital", 200);
			    }

			    return buildSuccessResponse(mapToDTO(form), "Generic Consent Form retrieved successfully");

			} else if (consentFormType.equals("2")) {
			    List<CustomConsentForm> formsList = customConsentFormRepository
			            .findAllByHospitalIdAndConsentFormType(hospitalId, "2");

			    if (formsList.isEmpty()) {
			        return buildErrorResponse("Procedure Consent Forms not found for this hospital", 200);
			    }

			    List<CustomConsentFormDTO> formsListDTO =
			            formsList.stream().map(this::mapToDTO).collect(Collectors.toList());

			    return buildSuccessResponse(formsListDTO, "Procedure Consent Forms retrieved successfully");

			} else {
			    return buildErrorResponse("Use getProcedureConsentForm API for procedure type", 400);
			}

		} catch (Exception ex) {
			log.error("Error while fetching Consent Form", ex);
			return buildErrorResponse("Error while fetching Consent Form: " + ex.getMessage(), 500);
		}
	}

	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getProcedureConsentFormFallback")
	public Response getProcedureConsentForm(String hospitalId, String subServiceId) {
		log.info("Fetching procedure consent form hospitalId={} subServiceId={}", hospitalId, subServiceId);
		if (hospitalId == null || hospitalId.trim().isEmpty()) {
			return buildErrorResponse("Hospital ID cannot be null or empty", 400);
		}
		if (subServiceId == null || subServiceId.trim().isEmpty()) {
			return buildErrorResponse("SubService ID cannot be null or empty", 400);
		}

		try {
			CustomConsentForm form = customConsentFormRepository
					.findByHospitalIdAndSubServiceid(hospitalId, subServiceId).orElse(null);
			if (form == null) {
				return buildErrorResponse("Procedure Consent Form not found for SubService: " + subServiceId, 404);
			}
			return buildSuccessResponse(mapToDTO(form), "Procedure Consent Form retrieved successfully");
		} catch (Exception ex) {
			log.error("Error while fetching Procedure Consent Form", ex);
			return buildErrorResponse("Error while fetching Procedure Consent Form: " + ex.getMessage(), 500);
		}
	}

	// ------------------------------- Helper Methods
	// -------------------------------
	private boolean isValidConsentFormType(String type) {
		return type != null && (type.equals("1") || type.equals("2"));
	}

//	private SubServicesDto validateAndGetSubService(String hospitalId, String subServiceId) {
//		ResponseEntity<ResponseStructure<SubServicesDto>> subServiceResponse = serviceFeignClient
//				.getSubServiceByServiceId(hospitalId, subServiceId);
//
//		if (subServiceResponse != null && subServiceResponse.getBody() != null) {
//			return subServiceResponse.getBody().getData();
//		}
//		return null;
//	}

	private CustomConsentFormDTO mapToDTO(CustomConsentForm form) {
		return new CustomConsentFormDTO(form.getId(), form.getHospitalId(), form.getSubServiceid(),
				form.getSubServiceName(), form.getConsentFormType(), form.getConsentFormQuestions());
	}

	private Response buildErrorResponse(String message, int status) {
		Response response = new Response();
		response.setSuccess(false);
		response.setMessage(message);
		response.setStatus(status);
		return response;
	}

	private Response buildSuccessResponse(Object data, String message) {
		Response response = new Response();
		response.setSuccess(true);
		response.setData(data);
		response.setMessage(message);
		response.setStatus(200);
		return response;
	}

//    ---------------------------get all conset forms----------------------------------
	// ------------------------------- Get All Consent Forms by Hospital
	// -------------------------------
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllConsentFormsByHospitalFallback")
	public Response getAllConsentFormsByHospital(String hospitalId) {
		log.info("Fetching all consent forms hospitalId={}", hospitalId);
		if (hospitalId == null || hospitalId.trim().isEmpty()) {
			return buildErrorResponse("Hospital ID cannot be null or empty", 400);
		}

		try {
			// Fetch all consent forms for the hospital
			List<CustomConsentForm> forms = customConsentFormRepository.findByHospitalId(hospitalId);

			if (forms == null || forms.isEmpty()) {
				return buildErrorResponse("No Consent Forms found for Hospital ID: " + hospitalId, 404);
			}

			// Map entities to DTOs
			List<CustomConsentFormDTO> formDTOs = forms.stream().map(this::mapToDTO).toList();

			return buildSuccessResponse(formDTOs, "All Consent Forms retrieved successfully");
		} catch (Exception ex) {
			log.error("Error while fetching all Consent Forms for Hospital: {}", hospitalId, ex);
			return buildErrorResponse("Error while fetching Consent Forms: " + ex.getMessage(), 500);
		}
	}
	// ------------------------------- Delete Consent Form by ID -------------------------------
	@Override
	 @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteConsentFormByIdFallback")
	public Response deleteConsentFormById(String formId) {
		log.info("Deleting consent form formId={}", formId);
	    if (formId == null || formId.trim().isEmpty()) {
	        return buildErrorResponse("Consent Form ID cannot be null or empty", 400);
	    }

	    try {
	        return customConsentFormRepository.findById(formId)
	                .map(form -> {
	                    customConsentFormRepository.deleteById(formId);
	                    return buildSuccessResponse(null, "Consent Form deleted successfully");
	                })
	                .orElseGet(() -> buildErrorResponse("Consent Form not found for ID: " + formId, 404));
	    } catch (Exception ex) {
	        log.error("Error while deleting Consent Form with ID: {}", formId, ex);
	        return buildErrorResponse("Error while deleting Consent Form: " + ex.getMessage(), 500);
	    }
	}

	
    
    

    // ================= RATE LIMIT FALLBACK METHODS =================

    public Response getConsentFormFallback(String hospitalId, String consentFormType, Exception ex) {
        log.error("Rate limiter triggered in getConsentForm hospitalId={} consentFormType={}", hospitalId, consentFormType, ex);
        return buildErrorResponse("Too many requests. Please try again later.", 429);
    }

    public Response getProcedureConsentFormFallback(String hospitalId, String subServiceId, Exception ex) {
        log.error("Rate limiter triggered in getProcedureConsentForm hospitalId={} subServiceId={}", hospitalId, subServiceId, ex);
        return buildErrorResponse("Too many requests. Please try again later.", 429);
    }

    public Response getAllConsentFormsByHospitalFallback(String hospitalId, Exception ex) {
        log.error("Rate limiter triggered in getAllConsentFormsByHospital hospitalId={}", hospitalId, ex);
        return buildErrorResponse("Too many requests. Please try again later.", 429);
    }

    public Response deleteConsentFormByIdFallback(String formId, Exception ex) {
        log.error("Rate limiter triggered in deleteConsentFormById formId={}", formId, ex);
        return buildErrorResponse("Too many requests. Please try again later.", 429);
    }
}
