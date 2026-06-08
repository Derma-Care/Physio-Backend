package physiotherapydoctor.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import physiotherapydoctor.dto.NotificationDTO;
import physiotherapydoctor.dto.ResBody;




@FeignClient(value = "notification-service")
public interface NotificationFeign {
	
//	@GetMapping("/api/notificationservice/getNotificationByBookingId/{id}")
//	public NotificationDTO getNotificationByBookingId(@RequestHeader("Authorization") String token,@PathVariable String id);
//
//	@PutMapping("/api/notificationservice/updateNotification")
//	public NotificationDTO updateNotification(@RequestHeader("Authorization") String token,@RequestBody NotificationDTO notificationDTO );
//	
	@GetMapping("/api/notificationservice/notificationtodoctor/{hospitalId}/{doctorId}")	
	public ResponseEntity<ResBody<List<NotificationDTO>>> notificationtodoctor(@PathVariable String hospitalId,
			@PathVariable String doctorId);
		
}
