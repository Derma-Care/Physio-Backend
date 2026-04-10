package com.dermacare.bookingService.feign;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.dermacare.bookingService.dto.NotificationDTO;


@FeignClient(value = "notification-service")
public interface NotificationFeign {
	
	@GetMapping("/api/notificationservice/getNotificationByBookingId/{id}")
	public NotificationDTO getNotificationByBookingId(@PathVariable String id);
		

	@PutMapping("/api/notificationservice/updateNotification")
	public NotificationDTO updateNotification(@RequestBody NotificationDTO notificationDTO );
	
	
		
}