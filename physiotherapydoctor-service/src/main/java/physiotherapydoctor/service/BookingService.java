package physiotherapydoctor.service;

import org.springframework.http.ResponseEntity;

public interface BookingService {


	public ResponseEntity<?> getAppointmentsByPatientId(String clinicId,
														String patientId,
														int page
														);
	ResponseEntity<?> searchAppointmentsByInput(String input);

	public  ResponseEntity<?> getTodaysAppointments(String clinicId,
													String doctorId,
													int page);

    ResponseEntity<?> getFilteredAppointments(String clinicId, String doctorId, String number);

    ResponseEntity<?> getCompletedAppointments(String clinicId, String doctorId);

    ResponseEntity<?> getConsultationTypeCounts(String clinicId, String doctorId);

	ResponseEntity<?> getInProgressAppointments(String mobileNumber);


	public ResponseEntity<?> getAllBookedServicesByDoctorId(String doctorId,
															int page);
	ResponseEntity<?> getDoctorFutureAppointments(String doctorId, int page);
	 public ResponseEntity<?> getInProgressBookingsByIds(String patientId,
	    		String bookingId);

	public  ResponseEntity<?> getDoctorAppointmentsonStatus(String clinicId,
															String branchId,
															String doctorId,
															String status,
															int page) ;

	ResponseEntity<?> searchPatient(String clinicId, String input);

	        
}

