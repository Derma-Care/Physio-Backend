package com.chiselon.notificationservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.chiselon.notificationservice.dto.BookingResponse;
import com.chiselon.notificationservice.dto.ExerciseInfo;
import com.chiselon.notificationservice.dto.NotificationDTO;
import com.chiselon.notificationservice.dto.NotificationResponse;
import com.chiselon.notificationservice.dto.NotificationToCustomer;
import com.chiselon.notificationservice.dto.PriceDropAlertDto;
import com.chiselon.notificationservice.dto.ResBody;
import com.chiselon.notificationservice.dto.Response;
import com.chiselon.notificationservice.service.ServiceInterface;


@RestController
@RequestMapping("/notificationservice")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class NotificationController {
	
	@Autowired
	private ServiceInterface notificationService;
	
	
	@PostMapping("/notifications")
	public ResponseEntity<Response> createNotification(@RequestBody BookingResponse booking) {
		return notificationService.createNotification(booking);
	     
	}
	
	
	@PostMapping("/response")
	public ResponseEntity<?> response(@RequestBody NotificationResponse notificationResponse){
		ResBody<NotificationDTO> res = notificationService.notificationResponse(notificationResponse);
		 if(res != null) {
		    	return ResponseEntity.status(res.getStatus()).body(res);}
		    return null;
		 
	}	
	
	@GetMapping("/sendNotificationToClinic/{clinicId}")
	public ResponseEntity<?> sendNotificationToClinic(@PathVariable String clinicId ){
		ResBody<List<NotificationDTO>> res = notificationService.sendNotificationToClinic(clinicId);
		 if(res != null) {
		    	return ResponseEntity.status(res.getStatus()).body(res);}
		    return null;
}	
		

	@GetMapping("/customerNotification/{customerMobileNumber}")
	public ResponseEntity<ResBody<List<NotificationToCustomer>>> customerNotification(
			@PathVariable String customerMobileNumber){
		return notificationService.notificationToCustomer(customerMobileNumber);
		}
	
	
	@GetMapping("/getNotificationByBookingId/{id}")
	public NotificationDTO getNotificationByBookingId(@PathVariable String id){
	NotificationDTO res = notificationService.getNotificationByBookingId(id);
		 if(res != null) {
		    return res;
		 }
		 return null;
	}	
	
	
	@PutMapping("/updateNotification")
	public NotificationDTO updateNotification(@RequestBody NotificationDTO notificationDTO ){
	NotificationDTO res = notificationService.updateNotification(notificationDTO);
		 if(res != null) {
		    return res;
		 }
		 return null;
	}	
	
	@PostMapping("/pricedrop/notification")
	public ResponseEntity<?> pricedrop(@RequestBody PriceDropAlertDto priceDropAlertDto){
		return notificationService.sendImageNotifications(priceDropAlertDto);
		 
	}	
	
	
	@GetMapping("/retrieve/priceDropNotification/{clinicId}/{branchId}")
	public ResponseEntity<?> priceDropNotification(@PathVariable String clinicId,@PathVariable String branchId ){
		return notificationService.priceDropNotifications(clinicId, branchId);
}	
	
	@PutMapping("/update/priceDropNotification/{clinicId}/{branchId}/{id}")
	public ResponseEntity<?> updatePriceDropNotification(@PathVariable String clinicId,@PathVariable String branchId,
		@PathVariable String id,@RequestBody PriceDropAlertDto dto ){
		return notificationService.updatePriceDropAlert(clinicId, branchId, id,dto);
}	
		
	@DeleteMapping("/delete/priceDropNotification/{clinicId}/{branchId}/{id}")
	public ResponseEntity<?> deletePriceDropNotification(@PathVariable String clinicId,@PathVariable String branchId,@PathVariable String id ){
		return notificationService.deletePriceDropAlerts(clinicId, branchId,id);
}	
	
	@PostMapping("/notificationToTherapist")
	public void notificationToTherapist(@RequestBody Map<String, String> data) {
		notificationService.sendNotificationToTherapist(data);
	     
	}
	
	@PostMapping("/therapistOverallFeedback")
	public void therapistOverallFeedback(@RequestBody Map<String, String> data) {
		notificationService.sendOverallFeedbackNotificationToTherapist(data);
	     
	}
	
	@PostMapping("/therapistSessionFeedback")
	public void therapistSessionFeedback(@RequestBody Map<String, String> data) {
		notificationService.sendSessionFeedbackNotificationToTherapist(data);
	     
	}
	
	@PostMapping("/therapistSessionReassign")
	public void sendSessionReassignNotificationToTherapist(@RequestBody Map<String, String> data) {
		notificationService.sendSessionReassignNotificationToTherapist(data);
	     
	}
	
	@PostMapping("/therapistSessionWithdraw")
	public void sendSessionWithdrawNotificationToTherapist(@RequestBody Map<String, String> data) {
		notificationService.sendSessionWithdrawNotificationToTherapist(data);
	     
	}
	
	@PostMapping("/exercise-reminders")
	public ResponseEntity<String> sendBulkExerciseReminders(
	        @RequestBody List<ExerciseInfo> reminders) {

	    notificationService.sendBulkExerciseReminders(reminders);

	    return ResponseEntity.ok("Exercise reminders sent successfully");
	}
	
	
}
