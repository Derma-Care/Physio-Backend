package physiotherapydoctor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import physiotherapydoctor.service.BookingService;

@RestController
@RequestMapping("/physiotherapy-doctor")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class BookingController {

	@Autowired
	private BookingService bookingService;

	// 1. Get appointments by patient ID
	@GetMapping("/appointments/patient/{patientId}")
	public ResponseEntity<?> getAppointmentsByPatientId(@PathVariable String patientId) {
		return bookingService.getAppointmentsByPatientId(patientId);
	}

	// 2. Search appointments by input (e.g. patient name, booking ID)
	@GetMapping("/appointments/search/{input}")
	public ResponseEntity<?> searchAppointmentsByInput(@PathVariable String input) {
		return bookingService.searchAppointmentsByInput(input);
	}

	// 3. Get today's appointments for a doctor in a clinic
	@GetMapping("/appointments/today/{clinicId}/{doctorId}")
	public ResponseEntity<?> getTodaysAppointments(@PathVariable String clinicId, @PathVariable String doctorId) {

		return bookingService.getTodaysAppointments(clinicId, doctorId);
	}

	// 4. Filter doctor appointments by status
	@GetMapping("/appointments/filter/{clinicId}/{doctorId}/{number}")
	public ResponseEntity<?> getFilteredAppointments(@PathVariable String clinicId, @PathVariable String doctorId,
			@PathVariable String number) {

		return bookingService.getFilteredAppointments(clinicId, doctorId, number);
	}

	// 5. Get completed appointments for a doctor
	@GetMapping("/appointments/completed/{clinicId}/{doctorId}")
	public ResponseEntity<?> getCompletedAppointments(@PathVariable String clinicId, @PathVariable String doctorId) {

		return bookingService.getCompletedAppointments(clinicId, doctorId);
	}

	// 6. Get consultation type statistics for a doctor
	@GetMapping("/appointments/consultation-types/{clinicId}/{doctorId}")
	public ResponseEntity<?> getConsultationTypeCounts(@PathVariable String clinicId, @PathVariable String doctorId) {

		return bookingService.getConsultationTypeCounts(clinicId, doctorId);
	}

	// 7. Get in-progress appointments by patient mobile number
	@GetMapping("/appointments/in-progress/{mobileNumber}")
	public ResponseEntity<?> getInProgressAppointments(@PathVariable String mobileNumber) {

		return bookingService.getInProgressAppointments(mobileNumber);
	}

	// 8. Get all appointments by doctor ID
	@GetMapping("/getAllAppointmentsByDoctorId/{doctorId}")
	public ResponseEntity<?> getAllBookedServicesByDoctorId(@PathVariable String doctorId) {

		return bookingService.getAllBookedServicesByDoctorId(doctorId);
	}

	// 9. Get future appointments within next 15 days
	@GetMapping("/getFutureDoctorappointmentsByDoctorId/{doctorId}")
	public ResponseEntity<?> getDoctorFutureAppointments(@PathVariable String doctorId) {

		return bookingService.getDoctorFutureAppointments(doctorId);
	}

	// 10. Get in-progress bookings by patientId and bookingId
	@GetMapping("/in-progress/PatientId/bookingId/{patientId}/{bookingId}")
	public ResponseEntity<?> getInprogressBookingsByPatientId(@PathVariable String patientId,
			@PathVariable String bookingId) {

		return bookingService.getInProgressBookingsByIds(patientId, bookingId);
	}

	// 11. Get doctor appointments by status
	@GetMapping("/getDoctorAppointmentsonStatuses/{clinicId}/{branchId}/{doctorId}/{status}")
	public ResponseEntity<?> getDoctorAppointmentsonStatus(@PathVariable String clinicId, @PathVariable String branchId,
			@PathVariable String doctorId, @PathVariable String status) {

		return bookingService.getDoctorAppointmentsonStatus(clinicId, branchId, doctorId, status);
	}
}