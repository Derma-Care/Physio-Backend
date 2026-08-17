package com.chiselon.clinicadmin.feignclient;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.chiselon.clinicadmin.dto.DoctorRatingNotificationDTO;
import com.chiselon.clinicadmin.dto.PriceDropAlertDto;

@FeignClient(value = "notification-service")
public interface NotificationFeign {

	@GetMapping("/api/notificationservice/sendNotificationToClinic/{clinicId}")
	public ResponseEntity<?> sendNotificationToClinic(@RequestHeader("Authorization") String token,@PathVariable String clinicId);

	@PostMapping("/api/notificationservice/pricedrop/notification")
	public ResponseEntity<?> pricedrop(@RequestHeader("Authorization") String token,@RequestBody PriceDropAlertDto priceDropAlertDto);

	@GetMapping("/api/notificationservice/retrieve/priceDropNotification/{clinicId}/{branchId}")
	public ResponseEntity<?> priceDropNotification(@RequestHeader("Authorization") String token,@PathVariable String clinicId, @PathVariable String branchId);

	@PutMapping("/api/notificationservice/update/priceDropNotification/{clinicId}/{branchId}/{id}")
	public ResponseEntity<?> updatePriceDropNotification(@RequestHeader("Authorization") String token,@PathVariable String clinicId, @PathVariable String branchId,
			@PathVariable String id, @RequestBody PriceDropAlertDto dto);

	@DeleteMapping("/api/notificationservice/delete/priceDropNotification/{clinicId}/{branchId}/{id}")

	public ResponseEntity<?> deletePriceDropNotification(@RequestHeader("Authorization") String token,@PathVariable String clinicId, @PathVariable String branchId,
			@PathVariable String id);

	@PostMapping("/api/notificationservice/doctor-rating/send")
	ResponseEntity<?> sendDoctorRatingNotification(@RequestHeader("Authorization") String token,@RequestBody DoctorRatingNotificationDTO dto);

	@PostMapping("/api/notificationservice/therapistOverallFeedback") 
	public void therapistOverallFeedback(@RequestHeader("Authorization") String token,@RequestBody Map<String, String> data);
	
	@PostMapping("/api/notificationservice/therapistSessionFeedback") 
	public void therapistSessionFeedback(@RequestHeader("Authorization") String token,@RequestBody Map<String, String> data);

//	public ResponseEntity<?> deletePriceDropNotification(@PathVariable String clinicId, @PathVariable String branchId,
//			@PathVariable String id);

	@PostMapping("/api/notificationservice/doctor-rating/send")
	public ResponseEntity<?> sendDoctorRatingNotification(@RequestBody DoctorRatingNotificationDTO dto);

	
	@PostMapping("/api/notificationservice/therapistSessionReassign")
	public void sendSessionReassignNotificationToTherapist(@RequestBody Map<String, String> data);
		
	
	@PostMapping("/api/notificationservice/therapistSessionWithdraw")
	public void sendSessionWithdrawNotificationToTherapist(@RequestBody Map<String, String> data);
		

}
