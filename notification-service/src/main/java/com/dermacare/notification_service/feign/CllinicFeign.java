package com.dermacare.notification_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.dermacare.notification_service.dto.CustomerOnbordingDTO;
import com.dermacare.notification_service.dto.ImageForNotificationDto;
import com.dermacare.notification_service.dto.Response;



@FeignClient(value = "clinicadmin")
public interface CllinicFeign {

	@GetMapping("/clinic-admin/doctor/{id}")
	public ResponseEntity<Response> getDoctorById(@PathVariable String id);
	
	@PutMapping("/clinic-admin/updateDoctorSlotWhileBooking/{doctorId}/{date}/{time}")
	public Boolean updateDoctorSlotWhileBooking(@PathVariable String doctorId, @PathVariable String date,
			@PathVariable String time);
	
	@PutMapping("/clinic-admin/makingFalseDoctorSlot/{doctorId}/{branchId}/{date}/{time}")
	public boolean makingFalseDoctorSlot(@PathVariable String doctorId,@PathVariable String branchId, @PathVariable String date,
			@PathVariable String time);
		
	 @PostMapping("/clinic-admin/uploadImageForNotification")
	 public ResponseEntity<?> uploadImageForNotification(@RequestBody ImageForNotificationDto imageForNotificationDto );
	   
	  @GetMapping("/clinic-admin/customers/getAllCustomers")
	  public ResponseEntity<Response> getAllCustomers();
	  
	  @GetMapping("/clinic-admin/gcmToken/{token}")
	    public CustomerOnbordingDTO getCustomerByToken(
	 			 @PathVariable String token );
	 	
	     
	
	
}
