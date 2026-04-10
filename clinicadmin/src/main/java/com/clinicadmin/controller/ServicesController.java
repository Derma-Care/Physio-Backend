package com.clinicadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicadmin.dto.Response;
import com.clinicadmin.service.ServicesService;

@RestController
@RequestMapping("/clinic-admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class ServicesController {
	@Autowired
	private ServicesService servicesService;

	@GetMapping("/getServiceByCategoryId/{categoryId}")
	public ResponseEntity<Response> getServiceByCategoryId(@PathVariable String categoryId) {
		Response response = servicesService.getServiceById(categoryId);
		return ResponseEntity.status(response.getStatus()).body(response);
	}
	@GetMapping("/getServiceByServiceId/{serviceId}")
	public ResponseEntity<Response> getServiceByServiceId(@PathVariable String serviceId)  {
		Response response = servicesService.getServiceByServiceId(serviceId);
		return ResponseEntity.status(response.getStatus()).body(response);
	}
	@GetMapping("/getAllServices")
	public ResponseEntity<Response> getServiceByCategoryId() {
		Response response = servicesService.getAllServices();
		return ResponseEntity.status(response.getStatus()).body(response);
	}
}
