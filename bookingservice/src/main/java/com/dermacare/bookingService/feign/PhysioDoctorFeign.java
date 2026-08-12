package com.dermacare.bookingService.feign;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.dermacare.bookingService.dto.Session;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(value = "physiotherapydoctor-service")
public interface PhysioDoctorFeign {

	@GetMapping("/api/physiotherapy-doctor/getPhysioByBookingId/{bookingId}/{startdate}/{endDate}")
	public ResponseEntity<List<Session>> getPhysioByBookingId(@PathVariable String bookingId,@PathVariable String startdate,@PathVariable String endDate);

	 @GetMapping("/api/physiotherapy-doctor/followups/today/booking-ids")
	 public List<String> getTodayFollowUpBookingIds();

	 @GetMapping("/api/physiotherapy-doctor/prescription/{BookingId}")
		public String getByBookingId(@PathVariable String BookingId);

	@PostMapping("/api/physiotherapy-doctor/sessionInfo/clinicId/{clinicId}/branchId/{branchId}/dates/{startDate}/{endDate}")
	public Map<String, List<Session>> getSessionInfo(
			@PathVariable	String clinicId,
			@PathVariable String branchId,
			@PathVariable String startDate,
			@PathVariable String endDate,
			@RequestBody List<String> list);
}
