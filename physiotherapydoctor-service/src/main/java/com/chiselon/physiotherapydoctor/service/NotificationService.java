package com.chiselon.physiotherapydoctor.service;


import java.util.List;

import org.springframework.http.ResponseEntity;

import com.chiselon.physiotherapydoctor.dto.NotificationDTO;
import com.chiselon.physiotherapydoctor.dto.ResBody;

public interface NotificationService {
	
	public ResponseEntity<ResBody<List<NotificationDTO>>> notificationToDoctor(String hospitalId,
			 String doctorId);
	
}
