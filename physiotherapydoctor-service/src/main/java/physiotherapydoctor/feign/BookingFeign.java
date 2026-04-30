package physiotherapydoctor.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
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
public interface BookingFeign {

    @GetMapping("/api/v1/getBookedServiceById/{id}")
    ResponseStructure<BookingResponse> getBookingById(@PathVariable("id") String id);

    @PutMapping("/api/v1/updateAppointment")
	public ResponseEntity<?> updateAppointment(@RequestBody BookingResponse bookingResponse );
    
    
    @GetMapping("/api/v1/getDoctorFutureAppointments/{doctorId}")
	public ResponseEntity<?> getDoctorFutureAppointments(@PathVariable String doctorId);
	
	@GetMapping("/api/v1/getAllBookedServices/{doctorId}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByDoctorId(@PathVariable String doctorId);
	
	@PostMapping("/api/v1/bookService")
	ResponseEntity<?> bookService(@RequestBody BookingRequset bookingRequest);
	
//	@GetMapping("/api/v1/getTodayDoctorAppointmentsByDoctorId/{clinicId}/{doctorId}")
//	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(@PathVariable String clinicId,@PathVariable String doctorId);
//	
	@GetMapping("/api/v1/getInProgressAppointments/{mobilenumber}")
	public ResponseEntity<?> inProgressAppointments(@PathVariable String mobilenumber);
	
	@GetMapping("/api/v1/in-progress/appointments/{patientId}/{bookingId}")
	public ResponseEntity<?> getInProgressAppointmentByPatientIdAndBookingId(@PathVariable String patientId,@PathVariable String bookingId);
	
	@GetMapping("/api/v1/getTodayDoctorAppointmentsByDoctorId/{clinicId}/{doctorId}")
	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(@PathVariable String clinicId,@PathVariable String doctorId);
	
}