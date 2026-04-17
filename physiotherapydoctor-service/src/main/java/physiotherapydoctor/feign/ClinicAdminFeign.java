package physiotherapydoctor.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.ResponseStructure;

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
}