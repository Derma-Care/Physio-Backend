package com.dermaCare.customerService.feignClient;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.dermaCare.customerService.dto.NotificationToCustomer;
import com.dermaCare.customerService.util.ResBody;


@FeignClient(value = "notification-service" )
public interface NotificationFeign {
	
	@GetMapping("/api/notificationservice/customerNotification/{customerMobileNumber}")
	public ResponseEntity<ResBody<List<NotificationToCustomer>>> customerNotification(
			@PathVariable String customerMobileNumber);
	
}
