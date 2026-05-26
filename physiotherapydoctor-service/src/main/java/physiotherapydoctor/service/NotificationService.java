package physiotherapydoctor.service;


import java.util.List;

import org.springframework.http.ResponseEntity;

import physiotherapydoctor.dto.NotificationDTO;
import physiotherapydoctor.dto.ResBody;

public interface NotificationService {
	
	public ResponseEntity<ResBody<List<NotificationDTO>>> notificationToDoctor(String hospitalId,
			 String doctorId);
	
}
