package com.chiselon.physiotherapydoctor.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chiselon.physiotherapydoctor.dto.NotificationDTO;
import com.chiselon.physiotherapydoctor.dto.ResBody;
import com.chiselon.physiotherapydoctor.service.NotificationService;

@RestController
@RequestMapping("/physiotherapy-doctor")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class NotificationController {

	@Autowired
	private NotificationService notificationService;

	@GetMapping("/notificationToDoctor/{hospitalId}/{doctorId}")
	public ResponseEntity<ResBody<List<NotificationDTO>>> notificationTodoctor(@PathVariable String hospitalId,
			@PathVariable String doctorId) {
		return notificationService.notificationToDoctor(hospitalId, doctorId);
	}

}
