package com.chiselon.customerservice.feignClient;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.chiselon.customerservice.dto.NotificationToCustomer;
import com.chiselon.customerservice.util.ResBody;


@FeignClient(value = "notification-service" )
public interface NotificationFeign {
	
	@GetMapping("/api/notificationservice/customerNotification/{customerMobileNumber}")
	public ResponseEntity<ResBody<List<NotificationToCustomer>>> customerNotification(
			@PathVariable String customerMobileNumber);
	
}
