package com.AdminService.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.AdminService.dto.VedioCallDTO;
import com.AdminService.service.VedioCallService;
import com.AdminService.util.Response;

@RestController
@RequestMapping("/admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class VedioCallController {
	@Autowired
	private VedioCallService vedioCallService;

	@PostMapping("/addVedioCallIds")
	ResponseEntity<Response> addingVedioCallIds(@RequestBody VedioCallDTO dto) {
		Response response = vedioCallService.addVedioCallCredential(dto);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/getVedioCallIds")
	ResponseEntity<Response> getVedioCallIds() {
		Response response = vedioCallService.getCredentials();
		return ResponseEntity.status(response.getStatus()).body(response);
	}

}