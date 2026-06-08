package physiotherapydoctor.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import physiotherapydoctor.dto.BookingRequset;
import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.ResponseStructure;

@FeignClient(name = "bookingservice")
public interface  BookingFeignClient {
	
	@GetMapping("/api/v1/getBookedServiceById/{id}")
	public ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(@PathVariable String id);

	@GetMapping("/api/v1/patient/{clinicId}/{patientId}/{page}/{size}")
	public ResponseEntity<Page<BookingResponse>> bookingByPatientId(
			@PathVariable String clinicId,
			@PathVariable String patientId,
			@PathVariable int page,
			@PathVariable int size);

	@GetMapping("/api/v1/getAppointsByInput/{input}")
	public ResponseEntity<?> getAppointsByInput(@PathVariable String input);

	@GetMapping("/api/v1/todayAppointments/{clinicId}/{doctorId}/{page}/{size}")
	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(
			@PathVariable String clinicId,
			@PathVariable String doctorId,
			@PathVariable int page,
			@PathVariable int size);

	@GetMapping("/api/v1/filterDoctorAppointmentsByDoctorId/{clinicId}/{doctorId}/{number}")
	public ResponseEntity<?> filterDoctorAppointmentsByDoctorId(@PathVariable String clinicId,@PathVariable String doctorId,@PathVariable String number);
	
	@GetMapping("/api/v1/getCompletedApntsByDoctorId/{clinicId}/{doctorId}")
	public ResponseEntity<?> filterDoctorAppointmentsByDoctorId(@PathVariable String clinicId,@PathVariable String doctorId);
	
	@GetMapping("/api/v1/getSizeOfConsultationTypesByDoctorId/{clinicId}/{doctorId}")
	public ResponseEntity<?> getSizeOfConsultationTypesByDoctorId(@PathVariable String clinicId,@PathVariable String doctorId);
	
	@GetMapping("/api/v1/getInProgressAppointments/{mobilenumber}")
	public ResponseEntity<?> inProgressAppointments(@PathVariable String mobilenumber);

	@GetMapping("/api/v1/futureAppointments/{doctorId}/{page}/{size}")
	public ResponseEntity<?> getDoctorFutureAppointments(
			@PathVariable String doctorId,
			@PathVariable int page,
			@PathVariable int size);

	@GetMapping("/api/v1/doctor/{doctorId}/{page}/{size}")
	public ResponseEntity<?> bookingByDoctorId(
			@PathVariable String doctorId,
			@PathVariable int page,
			@PathVariable int size);

	@PostMapping("/api/v1/bookService")
	ResponseEntity<?> bookService(@RequestBody BookingRequset bookingRequest);
	
	@GetMapping("/api/v1/in-progress/appointments/{patientId}/{bookingId}")
	public ResponseEntity<?> getInProgressAppointmentByPatientIdAndBookingId(@PathVariable String patientId,@PathVariable String bookingId);
	
	@PutMapping("/api/v1/update/bookingId")
	public ResponseEntity<?> updateAppointmentBasedOnBookingId(@RequestBody BookingResponse bookingResponse );

	@GetMapping("/api/v1/appointments/{clinicId}/{branchId}/{doctorId}/{status}/{page}/{size}")
	public ResponseEntity<?> getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatus(
			@PathVariable String clinicId,
			@PathVariable String branchId,
			@PathVariable String doctorId,
			@PathVariable String status,
			@PathVariable int page,
			@PathVariable int size);
}
