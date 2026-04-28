package physiotherapydoctor.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.dto.TherapistRecordDTO;

@FeignClient(name = "clinicadmin")
public interface ClinicAdminFeign {

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
    
}
    
    
