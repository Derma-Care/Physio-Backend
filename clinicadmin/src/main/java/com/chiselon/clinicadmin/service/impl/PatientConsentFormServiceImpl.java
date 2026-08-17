package com.chiselon.clinicadmin.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.chiselon.clinicadmin.dto.BookingResponse;
import com.chiselon.clinicadmin.dto.ClinicDTO;
import com.chiselon.clinicadmin.dto.PatientConsentFormDTO;
import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.entity.Doctors;
import com.chiselon.clinicadmin.entity.PatientConsentForm;
import com.chiselon.clinicadmin.feignclient.AdminServiceClient;
import com.chiselon.clinicadmin.feignclient.BookingFeign;
import com.chiselon.clinicadmin.repository.DoctorsRepository;
import com.chiselon.clinicadmin.repository.PatientConsentFormRepository;
import com.chiselon.clinicadmin.service.PatientConsentFormService;
import com.chiselon.clinicadmin.utils.Base64CompressionUtil;
import com.chiselon.clinicadmin.utils.KeyCloakTokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientConsentFormServiceImpl implements PatientConsentFormService {
	
	@Autowired
	private PatientConsentFormRepository patientConsentFormRepository;

	@Autowired
	private DoctorsRepository doctorsRepository;

	@Autowired
	private BookingFeign bookingFeign;
	
	@Autowired	
	private KeyCloakTokenStore keyCloakTokenStore;
	
	@Autowired
	private AdminServiceClient adminServiceClient;

	@Autowired
	private ObjectMapper objectMapper;
	
	

	@Override
	@Secured("ROLE_CLINICADMIN")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getPatientDetailsForFormUsingBookingFallback")
	public Response getPatientDetailsForFormUsingBooking(String bookingId, String patientId, String mobileNumber) {
		log.info("Generating patient consent form bookingId={} patientId={}", bookingId, patientId);
		Response response = new Response();
		log.debug("Calling booking service for patient details");
		ResponseEntity<Response> responseEntity = bookingFeign.getPatientDetailsForConsentForm(keyCloakTokenStore.getAccess_token(),bookingId, patientId,
				mobileNumber);
		Response resData = responseEntity.getBody();

		if (resData == null || resData.getData() == null) {
			response.setSuccess(false);
			response.setMessage("Patient details not found");
			response.setStatus(404);

			return response;
		}
		
		BookingResponse bookingDto = objectMapper.convertValue(resData.getData(), BookingResponse.class);

		log.debug("Fetching doctor details doctorId={}", bookingDto.getDoctorId());
		Doctors doctordata = doctorsRepository.findByDoctorId(bookingDto.getDoctorId()).orElse(null);
		if (doctordata == null) {
			response.setSuccess(false);
			response.setMessage("Doctor not found");
			response.setStatus(404);
			return response;

		}

 ResponseEntity<Response> clinicData =	adminServiceClient.getClinicById(keyCloakTokenStore.getAccess_token(),bookingDto.getClinicId());
 ClinicDTO clinics = objectMapper.convertValue(clinicData.getBody().getData(), ClinicDTO.class);
	if (clinics == null) {
		response.setSuccess(false);
		response.setMessage("Clinic not found with this hospitalId"+bookingDto.getClinicId());
		response.setStatus(404);
		return response;

	}
		

		String decompressedSignature = Base64CompressionUtil.decompressBase64(doctordata.getDoctorSignature());

		ResponseEntity<Response> adminRes = adminServiceClient.getClinicById(keyCloakTokenStore.getAccess_token(),bookingDto.getClinicId());
		Response clinicRes = adminRes.getBody();
        ClinicDTO clincDTO = objectMapper.convertValue(clinicRes.getData(), ClinicDTO.class);
        String decompressedLogo =  Base64CompressionUtil.decompressBase64(clinics.getHospitalLogo());
        
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String currentDate = LocalDate.now().format(formatter);

		// Create entity object
		PatientConsentForm formdata = new PatientConsentForm();
		formdata.setFullName(bookingDto.getName());
		formdata.setDateOfBirth(bookingDto.getAge());
		formdata.setContactNumber(bookingDto.getMobileNumber());
		formdata.setAddress(bookingDto.getPatientAddress());
		formdata.setProcedureName(null);
		formdata.setProcedureDate(bookingDto.getServiceDate());
		formdata.setPhysicianName(bookingDto.getDoctorName());
		formdata.setInformedAboutProcedure(true);
		formdata.setUnderstandsRisks(true);
		formdata.setInformedOfAlternatives(true);
		formdata.setHadQuestionsAnswered(true);
		formdata.setNoGuaranteesGiven(true);
		formdata.setAnesthesiaConsent(true);
		formdata.setUnderstandsAnesthesiaRisks(true);
		formdata.setConsentForRecording(true);
		formdata.setNoConsentForRecording(true);
		formdata.setUnderstandsWithdrawalRight(true);
		formdata.setConsentGiven(true);
		formdata.setPhysicianSignature(Base64CompressionUtil.compressBase64(decompressedSignature));
		formdata.setPhysicianSignedDate(currentDate);
		formdata.setHospitalId(bookingDto.getClinicId());
		formdata.setHospitalName(bookingDto.getClinicName());
		formdata.setHospitalLogo(decompressedLogo);

		// Save entity
		log.debug("Saving patient consent form");
		PatientConsentForm savedForm = patientConsentFormRepository.save(formdata);
		log.info("Patient consent form created successfully id={}", savedForm.getId());

		// Convert saved entity to DTO
		PatientConsentFormDTO formDTO = objectMapper.convertValue(savedForm, PatientConsentFormDTO.class);
		response.setSuccess(false);
		response.setData(formDTO);
		response.setMessage("Consent form saved and returned successfully");
		response.setStatus(200);
		return response;
	}

	@Override
	@Secured("ROLE_CLINICADMIN")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "updatePatientConsentFormFallback")
	public Response updatePatientConsentForm(String id, PatientConsentFormDTO dto) {
		log.info("Updating patient consent form id={}", id);
		Response response = new Response();

		if (id == null || id.isEmpty()) {
			response.setSuccess(false);
			response.setMessage("Consent form ID is required for update.");
			response.setStatus(400);
			return response;
		}

		// Fetch existing form
		PatientConsentForm existingForm = patientConsentFormRepository.findById(id).get();
		if (existingForm == null) {
			response.setSuccess(false);
			response.setMessage("Patient consent form not found for ID: " + id);
			response.setStatus(404);
			return response;
		}

		// Update only non-null fields from DTO
		if (dto.getFullName() != null)
			existingForm.setFullName(dto.getFullName());
		if (dto.getDateOfBirth() != null)
			existingForm.setDateOfBirth(dto.getDateOfBirth());
		if (dto.getContactNumber() != null)
			existingForm.setContactNumber(dto.getContactNumber());
		if (dto.getAddress() != null)
			existingForm.setAddress(dto.getAddress());
		if (dto.getMedicalRecordNumber() != null)
			existingForm.setMedicalRecordNumber(dto.getMedicalRecordNumber());

		if (dto.getProcedureName() != null)
			existingForm.setProcedureName(dto.getProcedureName());
		if (dto.getProcedureDate() != null)
			existingForm.setProcedureDate(dto.getProcedureDate());
		if (dto.getPhysicianName() != null)
			existingForm.setPhysicianName(dto.getPhysicianName());

		existingForm.setInformedAboutProcedure(dto.isInformedAboutProcedure());
		existingForm.setUnderstandsRisks(dto.isUnderstandsRisks());
		existingForm.setInformedOfAlternatives(dto.isInformedOfAlternatives());
		existingForm.setHadQuestionsAnswered(dto.isHadQuestionsAnswered());
		existingForm.setNoGuaranteesGiven(dto.isNoGuaranteesGiven());

		existingForm.setAnesthesiaConsent(dto.isAnesthesiaConsent());
		existingForm.setUnderstandsAnesthesiaRisks(dto.isUnderstandsAnesthesiaRisks());

		existingForm.setConsentForRecording(dto.isConsentForRecording());
		existingForm.setNoConsentForRecording(dto.isNoConsentForRecording());

		existingForm.setUnderstandsWithdrawalRight(dto.isUnderstandsWithdrawalRight());
		existingForm.setConsentGiven(dto.isConsentGiven());

		if (dto.getPatientSignature() != null)
			existingForm.setPatientSignature(dto.getPatientSignature());
		if (dto.getPatientSignedDate() != null)
			existingForm.setPatientSignedDate(dto.getPatientSignedDate());
		if (dto.getWitnessSignature() != null)
			existingForm.setWitnessSignature(dto.getWitnessSignature());
		if (dto.getWitnessSignedDate() != null)
			existingForm.setWitnessSignedDate(dto.getWitnessSignedDate());
		if (dto.getPhysicianSignature() != null)
			existingForm.setPhysicianSignature(dto.getPhysicianSignature());
		if (dto.getPhysicianSignedDate() != null)
			existingForm.setPhysicianSignedDate(dto.getPhysicianSignedDate());
		if (dto.getHospitalId() != null)
			existingForm.setHospitalId(dto.getHospitalId());
		if (dto.getHospitalName() != null)
			existingForm.setHospitalName(dto.getHospitalName());
		if (dto.getHospitalLogo() != null)
			existingForm.setHospitalLogo(Base64CompressionUtil.compressBase64(dto.getHospitalLogo()));
			

		// Save the updated form
		log.debug("Saving updated patient consent form id={}", id);
		PatientConsentForm updatedForm = patientConsentFormRepository.save(existingForm);
		log.info("Patient consent form updated successfully id={}", id);

		// Convert entity to DTO
		PatientConsentFormDTO updatedDTO = objectMapper.convertValue(updatedForm, PatientConsentFormDTO.class);
		response.setSuccess(true);
		response.setMessage("Consent form updated successfully.");
		response.setStatus(200);
		response.setData(updatedDTO);

		return response;
	}



    public Response getPatientDetailsForFormUsingBookingFallback(
            String bookingId,
            String patientId,
            String mobileNumber,
            Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse(ex);
    }

    public Response updatePatientConsentFormFallback(
            String id,
            PatientConsentFormDTO dto,
            Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse(ex);
    }

    public Response buildRateLimitResponse(Exception ex) {
        Response response = new Response();
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again after some time.");
        response.setStatus(429);
        return response;
    }

}