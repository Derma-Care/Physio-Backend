package physiotherapydoctor.feign;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import physiotherapydoctor.dto.ExerciseInfo;
import physiotherapydoctor.dto.NotificationDTO;
import physiotherapydoctor.dto.ResBody;


@FeignClient(value = "notification-service")
public interface NotificationFeign {
	
	@GetMapping("/api/notificationservice/getNotificationByBookingId/{id}")
	public NotificationDTO getNotificationByBookingId(@PathVariable String id);

	@PutMapping("/api/notificationservice/updateNotification")
	public NotificationDTO updateNotification(@RequestBody NotificationDTO notificationDTO );
	
	@GetMapping("/api/notificationservice/notificationtodoctor/{hospitalId}/{doctorId}")	
	public ResponseEntity<ResBody<List<NotificationDTO>>> notificationtodoctor(@PathVariable String hospitalId,
			@PathVariable String doctorId);
	
	@PostMapping("/api/notificationservice/notificationToTherapist")
	public void notificationToTherapist(@RequestBody Map<String, String> data);
	
	@PostMapping("/api/notificationservice/exercise-reminders")
	public ResponseEntity<String> sendBulkExerciseReminders(
	        @RequestBody List<ExerciseInfo> reminders);

		
}
