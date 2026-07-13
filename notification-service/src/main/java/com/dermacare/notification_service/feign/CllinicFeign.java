package com.dermacare.notification_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.dermacare.notification_service.dto.CustomerOnbordingDTO;
import com.dermacare.notification_service.dto.ImageForNotificationDto;
import com.dermacare.notification_service.dto.Response;

@FeignClient(value = "clinicadmin")
public interface CllinicFeign {

	@GetMapping("/clinic-admin/doctor/{id}")
	public ResponseEntity<Response> getDoctorById(@RequestHeader("Authorization") String token,@PathVariable String id);

	@PutMapping("/clinic-admin/updateDoctorSlotWhileBooking/{doctorId}/{date}/{time}")
	public Boolean updateDoctorSlotWhileBooking(@RequestHeader("Authorization") String token,@PathVariable String doctorId, @PathVariable String date,
			@PathVariable String time);

	@PutMapping("/clinic-admin/makingFalseDoctorSlot/{doctorId}/{branchId}/{date}/{time}")

	public boolean makingFalseDoctorSlot(@RequestHeader("Authorization") String token,@PathVariable String doctorId,@PathVariable String branchId, @PathVariable String date,
			@PathVariable String time);
		
	 @PostMapping("/clinic-admin/uploadImageForNotification")
	 public ResponseEntity<?> uploadImageForNotification(@RequestHeader("Authorization") String token,@RequestBody ImageForNotificationDto imageForNotificationDto );
	   
	  @GetMapping("/clinic-admin/customers/getAllCustomers")
	  public ResponseEntity<Response> getAllCustomers(@RequestHeader("Authorization") String token);
	  
	  @GetMapping("/clinic-admin/gcmToken/{token}")
	    public CustomerOnbordingDTO getCustomerByToken(@RequestHeader("Authorization") String auth,
	 			 @PathVariable String token );
	 	
	  @GetMapping("/clinic-admin/deviceId/{clinicId}/{branchId}")
		public String getDeviceId(@RequestHeader("Authorization") String token,@PathVariable String clinicId,@PathVariable String branchId);
		
	  @GetMapping("/clinic-admin/deviceIdByCustomerId/{customerId}")
	    public String customerDeviceId(@RequestHeader("Authorization") String token,
	 			 @PathVariable String customerId );
	 	
	  @GetMapping("/clinic-admin/doctor/getDeviceId/{doctorId}")
		String getDoctorDeviceId(@RequestHeader("Authorization") String token,@PathVariable String doctorId);

	
	@GetMapping("/clinic-admin/therapist/deviceId/{deviceId}")
	public String retrivetherapistDeviceId(@RequestHeader("Authorization") String token,@PathVariable String deviceId);


}

