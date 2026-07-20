package physiotherapydoctor.service;

import org.springframework.http.ResponseEntity;

import physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
import physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
import physiotherapydoctor.dto.DoctorLoginDTO;
import physiotherapydoctor.dto.ResetPasswordDTO;
import physiotherapydoctor.dto.Response;

public interface PhysiotherapyDoctorDetails {

	Response getPhysioDoctorDetails(String clinicId, String branchId);

//	============From Doctor service ccms======================================= 

	Response login(DoctorLoginDTO loginDTO);

//		Response registerDoctor(DoctorDTO doctorDTO);
//		Response changePassword(ChangeDoctorPasswordDTO updateDTO);
	Response changePassword(String username, ChangeDoctorPasswordDTO updateDTO);

	Response updateDoctorAvailability(String doctorId, DoctorAvailabilityStatusDTO availabilityDTO);

	public ResponseEntity<?> getAllDoctors();

	public ResponseEntity<?> getDoctorById(String id);

	public ResponseEntity<?> getDoctorByClinicAndDoctorId(String clinicId, String doctorId);

	public ResponseEntity<?> getDoctorsByHospitalById(String clinicId);

	public ResponseEntity<?> getDoctorsBySubServiceId(String hsptlId, String subServiceId);

	// public ResponseEntity<?> getAllDoctorsBySubServiceId(String subServiceId);
	public ResponseEntity<?> getDoctorFutureAppointments(String doctorId);

	public ResponseEntity<Response> getDiseasesFromClinicAdmin(String hospitalId);

	public ResponseEntity<Response> getLabTestsFromClinicAdmin(String hospitalId);

//	-------Forgot password-----------------------	
	ResponseEntity<Response> forgotPassword(String mobileNumber, String role);

	ResponseEntity<Response> verifyOtp(String mobileNumber, String role, String otp);

	ResponseEntity<Response> resetPassword(String role, String mobileNumber, ResetPasswordDTO dto);

}
