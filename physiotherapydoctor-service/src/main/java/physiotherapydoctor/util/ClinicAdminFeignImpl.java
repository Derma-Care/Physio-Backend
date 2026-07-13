package physiotherapydoctor.util;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
import physiotherapydoctor.dto.ClinicInfoDTO;
import physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
import physiotherapydoctor.dto.DoctorsDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.dto.TherapistRecordDTO;
import physiotherapydoctor.dto.TreatmentDTO;
import physiotherapydoctor.dto.VitalsDTO;
import physiotherapydoctor.feign.ClinicAdminFeign;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicAdminFeignImpl {

	private final ClinicAdminFeign clinicAdminFeign;
	private final KeyCloakTokenStore keyCloakTokenStore;

	private String token() {
		return keyCloakTokenStore.getAccess_token();
	}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "doctorLoginFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "doctorLoginFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "doctorLoginFallback")
	public ResponseEntity<Response> doctorLogin(Map<String, String> dto) {

		return clinicAdminFeign.doctorLogin(dto);
	}

	public ResponseEntity<Response> doctorLoginFallback(Map<String, String> dto, Throwable ex) {

		log.error("Doctor Login Failed", ex);

		throw getFallbackException(ex);	}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "changePasswordFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "changePasswordFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "changePasswordFallback")
	public Response changePassword(String username, ChangeDoctorPasswordDTO dto) {

		return clinicAdminFeign.changePassword(token(), username, dto);
	}

	public Response changePasswordFallback(String username, ChangeDoctorPasswordDTO dto, Throwable ex) {

		log.error("Change Password Failed {}", username, ex);
		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updateDoctorAvailabilityFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "updateDoctorAvailabilityFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "updateDoctorAvailabilityFallback")
	public Response updateDoctorAvailability(String doctorId, DoctorAvailabilityStatusDTO dto) {

		return clinicAdminFeign.updateDoctorAvailability(token(), doctorId, dto);
	}

	public Response updateDoctorAvailabilityFallback(String doctorId, DoctorAvailabilityStatusDTO dto, Throwable ex) {

		log.error("Availability Update Failed {}", doctorId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getBookingByIdFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getBookingByIdFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getBookingByIdFallback")
	public ResponseStructure<BookingResponse> getBookingById(String bookingId) {

		return clinicAdminFeign.getBookingById(token(), bookingId);
	}

	public ResponseStructure<BookingResponse> getBookingByIdFallback(String bookingId, Throwable ex) {

		log.error("Get Booking Failed {}", bookingId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updateAppointmentFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "updateAppointmentFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "updateAppointmentFallback")
	public ResponseEntity<?> updateAppointment(BookingResponse bookingResponse) {

		return clinicAdminFeign.updateAppointment(token(), bookingResponse);
	}

	public ResponseEntity<?> updateAppointmentFallback(BookingResponse bookingResponse, Throwable ex) {

		log.error("Update Appointment Failed", ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getByPatientIdAndBookingIdFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getByPatientIdAndBookingIdFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getByPatientIdAndBookingIdFallback")
	public ResponseStructure<List<TherapistRecordDTO>> getByPatientIdAndBookingId(String patientId, String bookingId) {

		return clinicAdminFeign.getByPatientIdAndBookingId(token(), patientId, bookingId);
	}

	public ResponseStructure<List<TherapistRecordDTO>> getByPatientIdAndBookingIdFallback(String patientId,
			String bookingId, Throwable ex) {

		log.error("Therapist Records Failed {}", bookingId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getRecordBySessionFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getRecordBySessionFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getRecordBySessionFallback")
	public ResponseEntity<ResponseStructure<TherapistRecordDTO>> getRecordBySession(String clinicId, String branchId,
			String bookingId, String patientId, String sessionId) {

		return clinicAdminFeign.getRecordBySession(token(), clinicId, branchId, bookingId, patientId, sessionId);
	}

	public ResponseEntity<ResponseStructure<TherapistRecordDTO>> getRecordBySessionFallback(String clinicId,
			String branchId, String bookingId, String patientId, String sessionId, Throwable ex) {

		log.error("Get Record By Session Failed bookingId={}, sessionId={}", bookingId, sessionId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getTherapistWithRequiredFiledsFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getTherapistWithRequiredFiledsFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getTherapistWithRequiredFiledsFallback")
	public ResponseEntity<Response> getTherapistWithRequiredFileds(String clinicId, String branchId) {

		return clinicAdminFeign.getTherapistWithRequiredFileds(token(), clinicId, branchId);
	}

	public ResponseEntity<Response> getTherapistWithRequiredFiledsFallback(String clinicId, String branchId,
			Throwable ex) {

		log.error("Get Therapist Failed clinicId={}, branchId={}", clinicId, branchId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getCompletedTherapyRecordFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getCompletedTherapyRecordFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getCompletedTherapyRecordFallback")
	public ResponseEntity<ResponseStructure<TherapistRecordDTO>> getCompletedTherapyRecord(String clinicId,
			String branchId, String therapistRecordId, String sessionId) {

		return clinicAdminFeign.getCompletedTherapyRecord(token(), clinicId, branchId, therapistRecordId, sessionId);
	}

	public ResponseEntity<ResponseStructure<TherapistRecordDTO>> getCompletedTherapyRecordFallback(String clinicId,
			String branchId, String therapistRecordId, String sessionId, Throwable ex) {

		log.error("Completed Therapy Record Failed therapistRecordId={}", therapistRecordId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updateDoctorByIdFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "updateDoctorByIdFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "updateDoctorByIdFallback")
	public ResponseEntity<Response> updateDoctorById(String doctorId, DoctorsDTO dto) {

		return clinicAdminFeign.updateDoctorById(token(), doctorId, dto);
	}

	public ResponseEntity<Response> updateDoctorByIdFallback(String doctorId, DoctorsDTO dto, Throwable ex) {

		log.error("Update Doctor Failed doctorId={}", doctorId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "addTreatmentFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "addTreatmentFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "addTreatmentFallback")
	public ResponseEntity<Response> addTreatment(TreatmentDTO dto) {

		return clinicAdminFeign.addTreatment(dto);
	}

	public ResponseEntity<Response> addTreatmentFallback(TreatmentDTO dto, Throwable ex) {

		log.error("Add Treatment Failed", ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getAllDoctorsFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getAllDoctorsFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllDoctorsFallback")
	public ResponseEntity<Response> getAllDoctors() {

		return clinicAdminFeign.getAllDoctors(token());
	}

	public ResponseEntity<Response> getAllDoctorsFallback(Throwable ex) {

		log.error("Get All Doctors Failed", ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getDoctorByIdFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getDoctorByIdFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getDoctorByIdFallback")
	public ResponseEntity<Response> getDoctorById(String id) {

		return clinicAdminFeign.getDoctorById(token(), id);
	}

	public ResponseEntity<Response> getDoctorByIdFallback(String id, Throwable ex) {

		log.error("Get Doctor By Id Failed doctorId={}", id, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getDoctorByClinicAndDoctorIdFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getDoctorByClinicAndDoctorIdFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getDoctorByClinicAndDoctorIdFallback")
	public ResponseEntity<Response> getDoctorByClinicAndDoctorId(String clinicId, String doctorId) {

		return clinicAdminFeign.getDoctorByClinicAndDoctorId(token(), clinicId, doctorId);
	}

	public ResponseEntity<Response> getDoctorByClinicAndDoctorIdFallback(String clinicId, String doctorId,
			Throwable ex) {

		log.error("Get Doctor By Clinic Failed clinicId={}, doctorId={}", clinicId, doctorId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getDoctorsByHospitalByIdFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getDoctorsByHospitalByIdFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getDoctorsByHospitalByIdFallback")
	public ResponseEntity<Response> getDoctorsByHospitalById(String hospitalId) {

		return clinicAdminFeign.getDoctorsByHospitalById(token(), hospitalId);
	}

	public ResponseEntity<Response> getDoctorsByHospitalByIdFallback(String hospitalId, Throwable ex) {

		log.error("Get Doctors By Hospital Failed hospitalId={}", hospitalId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getClinicInfoByDoctorIdFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getClinicInfoByDoctorIdFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getClinicInfoByDoctorIdFallback")
	public ClinicInfoDTO getClinicInfoByDoctorId(String doctorId) {

		return clinicAdminFeign.getClinicInfoByDoctorId(token(), doctorId);
	}

	public ClinicInfoDTO getClinicInfoByDoctorIdFallback(String doctorId, Throwable ex) {

		log.error("Get Clinic Info Failed doctorId={}", doctorId, ex);

		throw getFallbackException(ex);
	}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "addVitalsFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "addVitalsFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "addVitalsFallback")
	public ResponseEntity<Response> addVitals(String bookingId, VitalsDTO dto) {

		return clinicAdminFeign.addVitals(token(), bookingId, dto);
	}

	public ResponseEntity<Response> addVitalsFallback(String bookingId, VitalsDTO dto, Throwable ex) {

		log.error("Add Vitals Failed bookingId={}", bookingId, ex);
		throw getFallbackException(ex);	}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getVitalsFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getVitalsFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getVitalsFallback")
	public ResponseEntity<Response> getVitals(String bookingId, String patientId) {

		return clinicAdminFeign.getVitals(token(), bookingId, patientId);
	}

	public ResponseEntity<Response> getVitalsFallback(String bookingId, String patientId, Throwable ex) {

		log.error("Get Vitals Failed bookingId={}, patientId={}", bookingId, patientId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "delVitalsFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "delVitalsFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "delVitalsFallback")
	public ResponseEntity<Response> delVitals(String bookingId, String patientId) {

		return clinicAdminFeign.delVitals(token(), bookingId, patientId);
	}

	public ResponseEntity<Response> delVitalsFallback(String bookingId, String patientId, Throwable ex) {

		log.error("Delete Vitals Failed bookingId={}, patientId={}", bookingId, patientId, ex);
		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updateVitalsFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "updateVitalsFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "updateVitalsFallback")
	public ResponseEntity<Response> updateVitals(String bookingId, String patientId, VitalsDTO dto) {

		return clinicAdminFeign.updateVitals(token(), bookingId, patientId, dto);
	}

	public ResponseEntity<Response> updateVitalsFallback(String bookingId, String patientId, VitalsDTO dto,
			Throwable ex) {

		log.error("Update Vitals Failed bookingId={}, patientId={}", bookingId, patientId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getDiseasesByHospitalIdFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getDiseasesByHospitalIdFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getDiseasesByHospitalIdFallback")
	public ResponseEntity<Response> getDiseasesByHospitalId(String hospitalId) {

		return clinicAdminFeign.getDiseasesByHospitalId(token(), hospitalId);
	}

	public ResponseEntity<Response> getDiseasesByHospitalIdFallback(String hospitalId, Throwable ex) {

		log.error("Get Diseases Failed hospitalId={}", hospitalId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getSignedUrlFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getSignedUrlFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getSignedUrlFallback")
	public ResponseEntity<String> getSignedUrl(String fileKey) {

		return clinicAdminFeign.getSignedUrl(token(), fileKey);
	}

	public ResponseEntity<String> getSignedUrlFallback(String fileKey, Throwable ex) {

		log.error("Get Signed URL Failed fileKey={}", fileKey, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getAllTreatmentsFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getAllTreatmentsFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllTreatmentsFallback")
	public ResponseEntity<Response> getAllTreatments() {

		return clinicAdminFeign.getAllTreatments(token());
	}

	public ResponseEntity<Response> getAllTreatmentsFallback(Throwable ex) {

		log.error("Get All Treatments Failed", ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getTreatmentByIdFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getTreatmentByIdFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getTreatmentByIdFallback")
	public ResponseEntity<Response> getTreatmentById(String id, String hospitalId) {

		return clinicAdminFeign.getTreatmentById(token(), id, hospitalId);
	}

	public ResponseEntity<Response> getTreatmentByIdFallback(String id, String hospitalId, Throwable ex) {

		log.error("Get Treatment Failed id={}", id, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "deleteTreatmentByIdFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "deleteTreatmentByIdFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteTreatmentByIdFallback")
	public ResponseEntity<Response> deleteTreatmentById(String id, String hospitalId) {

		return clinicAdminFeign.deleteTreatmentById(token(), id, hospitalId);
	}

	public ResponseEntity<Response> deleteTreatmentByIdFallback(String id, String hospitalId, Throwable ex) {

		log.error("Delete Treatment Failed id={}", id, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updateTreatmentByIdFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "updateTreatmentByIdFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "updateTreatmentByIdFallback")
	public ResponseEntity<Response> updateTreatmentById(String id, String hospitalId, TreatmentDTO dto) {

		return clinicAdminFeign.updateTreatmentById(token(), id, hospitalId, dto);
	}

	public ResponseEntity<Response> updateTreatmentByIdFallback(String id, String hospitalId, TreatmentDTO dto,
			Throwable ex) {

		log.error("Update Treatment Failed id={}", id, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getAllRecoverySupportsByClinicIdFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getAllRecoverySupportsByClinicIdFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllRecoverySupportsByClinicIdFallback")
	public Response getAllRecoverySupportsByClinicId(String clinicId) {

		return clinicAdminFeign.getAllRecoverySupportsByClinicId(token(), clinicId);
	}

	public Response getAllRecoverySupportsByClinicIdFallback(String clinicId, Throwable ex) {

		log.error("Recovery Supports Failed clinicId={}", clinicId, ex);

		throw getFallbackException(ex);}

	@CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getAssignedTherapistDetailsFallback")
	@Retry(name = "clinicAdminService", fallbackMethod = "getAssignedTherapistDetailsFallback")
	@RateLimiter(name = "clinicAdminService", fallbackMethod = "getAssignedTherapistDetailsFallback")
	public ResponseEntity<Response> getAssignedTherapistDetails(String therapistRecordId) {

		return clinicAdminFeign.getAssignedTherapistDetails(token(), therapistRecordId);
	}

	public ResponseEntity<Response> getAssignedTherapistDetailsFallback(String therapistRecordId, Throwable ex) {

		log.error("Assigned Therapist Details Failed therapistRecordId={}", therapistRecordId, ex);

		throw getFallbackException(ex);}
	
	
	@CircuitBreaker(
	        name = "clinicAdminService",
	        fallbackMethod = "getCustomernameFallback")
	@Retry(
	        name = "clinicAdminService",
	        fallbackMethod = "getCustomernameFallback")
	public String getCustomername(String id) {

	    return clinicAdminFeign.getCustomername(token(),id);
	}

	public String getCustomernameFallback(
	        String id,
	        Exception ex) {

	    throw getFallbackException(ex);
	}
	
	
	
	
	
	@CircuitBreaker(
	        name = "clinicAdminService",
	        fallbackMethod = "getPatientnameFallback")
	@Retry(
	        name = "clinicAdminService",
	        fallbackMethod = "getPatientnameFallback")
	public String getPatientname(String id) {

	    return clinicAdminFeign.getPatientname( token(), id);
	}

	public String getPatientnameFallback(
	        String token,
	        String id,
	        Exception ex) {

	    throw getFallbackException(ex);
	}
	
	
	
	 private RuntimeException getFallbackException(Throwable ex) {

	        if (ex instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
	            return new ResponseStatusException(
	                    HttpStatus.SERVICE_UNAVAILABLE,
	                    "Booking service is currently unavailable after multiple retry attempts."
	                    );      
	        }else if (ex instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
	            return new ResponseStatusException(
	                    HttpStatus.SERVICE_UNAVAILABLE,
	                    "Booking Service is temporarily unavailable"); 
	    }else{ return new ResponseStatusException(
	                HttpStatus.TOO_MANY_REQUESTS,
	                "Too many requests. Please try again after some time.");}
	    }
}