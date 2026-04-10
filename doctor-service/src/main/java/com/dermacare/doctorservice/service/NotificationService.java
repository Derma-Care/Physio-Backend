package com.dermacare.doctorservice.service;

import java.util.List;
import org.springframework.http.ResponseEntity;
import com.dermacare.doctorservice.dto.NotificationDTO;
import com.dermacare.doctorservice.dto.ResBody;

public interface NotificationService {
	
	public ResponseEntity<ResBody<List<NotificationDTO>>> notificationToDoctor(String hospitalId,
			 String doctorId);
	
}