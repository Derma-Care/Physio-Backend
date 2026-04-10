package com.clinicadmin.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.clinicadmin.dto.PriceDropAlertDto;


@FeignClient(value = "notification-service")
public interface NotificationFeign {
	
	
	@GetMapping("/api/notificationservice/sendNotificationToClinic/{clinicId}")
	public ResponseEntity<?> sendNotificationToClinic(@PathVariable String clinicId );
	
	@PostMapping("/api/notificationservice/pricedrop/notification")
	public ResponseEntity<?> pricedrop(@RequestBody PriceDropAlertDto priceDropAlertDto);
	
	@GetMapping("/api/notificationservice/retrieve/priceDropNotification/{clinicId}/{branchId}")
	public ResponseEntity<?> priceDropNotification(@PathVariable String clinicId,@PathVariable String branchId );
	
	@PutMapping("/api/notificationservice/update/priceDropNotification/{clinicId}/{branchId}/{id}")
	public ResponseEntity<?> updatePriceDropNotification(@PathVariable String clinicId,@PathVariable String branchId,
			@PathVariable String id,@RequestBody PriceDropAlertDto dto );
	
	@DeleteMapping("/api/notificationservice/delete/priceDropNotification/{clinicId}/{branchId}/{id}")
	public ResponseEntity<?> deletePriceDropNotification(@PathVariable String clinicId,@PathVariable String branchId,@PathVariable String id);
		
}
