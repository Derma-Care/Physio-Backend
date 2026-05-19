package physiotherapydoctor.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
import physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
import physiotherapydoctor.dto.DoctorLoginDTO;
import physiotherapydoctor.dto.DoctorsDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.dto.TherapistRecordDTO;

@FeignClient(name = "clinicadmin")
public interface ClinicAdminFeign {
	
	@PostMapping("/clinic-admin/doctorLogin")
	public Response login(DoctorLoginDTO loginDTO);

	 @PutMapping("/clinic-admin/update-password/{username}")
	    Response changePassword(@PathVariable("username") String username, @RequestBody ChangeDoctorPasswordDTO updateDTO);
	 
	 @PostMapping("/clinic-admin/doctorId/{doctorId}/availability")
	 Response updateDoctorAvailability(@PathVariable("doctorId") String doctorId,
	                                   @RequestBody DoctorAvailabilityStatusDTO availabilityDTO);
	 

    // ✅ Get booking by bookingId
    @GetMapping("/clinic-admin/getBookingById/{bookingId}")
    ResponseStructure<BookingResponse> getBookingById(
            @PathVariable("bookingId") String bookingId);

    // ✅ Update booking status
    @PutMapping("/clinic-admin/updateAppointmentBasedOnBookingId")
    ResponseEntity<?> updateAppointment(
            @RequestBody BookingResponse bookingResponse);
    
    @GetMapping("/clinic-admin/getByPatientIdAndBookingId/{patientId}/{bookingId}")
    ResponseStructure<List<TherapistRecordDTO>> getByPatientIdAndBookingId(
            @PathVariable String patientId,
            @PathVariable String bookingId);
    
    @GetMapping("/clinic-admin/getRecordBySession/{clinicId}/{branchId}/{bookingId}/{patientId}/{sessionId}")
    ResponseEntity<ResponseStructure<TherapistRecordDTO>> getRecordBySession(
            @PathVariable String clinicId,
            @PathVariable String branchId,
            @PathVariable String bookingId,
            @PathVariable String patientId,
            @PathVariable String sessionId);
    
 // ================= GET Threapistdata by clinicId and Branch Id  with required field=================
 	@GetMapping("/clinic-admin/getTherapistWithRequiredFileds/{clinicId}/{branchId}")
 	public ResponseEntity<Response> getTherapistWithRequiredFileds(@PathVariable String clinicId,
 			@PathVariable String branchId);
 	
 	
 	@GetMapping("/clinic-admin/getCompletedTherapyRecord/{clinicId}/{branchId}/{therapistRecordId}/{sessionId}")
 	ResponseEntity<ResponseStructure<TherapistRecordDTO>> getCompletedTherapyRecord(
 	        @PathVariable String clinicId,
 	        @PathVariable String branchId,
 	        @PathVariable String therapistRecordId,
 	        @PathVariable String sessionId);
 	
	@PutMapping("/clinic-admin/updateDoctor/{doctorId}")
	public ResponseEntity<Response> updateDoctorById(@PathVariable String doctorId,
			 @RequestBody DoctorsDTO dto) ;
}
    
    
