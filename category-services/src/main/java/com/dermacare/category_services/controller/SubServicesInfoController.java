package com.dermacare.category_services.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dermacare.category_services.dto.SubServicesInfoDto;
import com.dermacare.category_services.repository.SubServicesInfoRepository;
import com.dermacare.category_services.service.SubServiceInfo;
import com.dermacare.category_services.util.Response;

@RestController
@RequestMapping("/v1/SubServicesInfo")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class SubServicesInfoController {

	@Autowired
	private SubServiceInfo serviceImpl;
	
	@Autowired
	private SubServicesInfoRepository subServicesInfoRepository;

	@PostMapping("/addSubService")
	public ResponseEntity<?> addSubService(@RequestBody SubServicesInfoDto dto) {
		Response response = serviceImpl.addSubService(dto);
		if (response != null) {
			return ResponseEntity.status(response.getStatus()).body(response);
		} else {
			return null;
		}
	}

	@GetMapping("/getSubServiceByIdCategory/{categoryId}")
	public ResponseEntity<?> getSubServiceByIdCategory(@PathVariable String categoryId) {
		Response response = serviceImpl.getSubServiceByIdCategory(categoryId);
		if (response != null) {
			return ResponseEntity.status(response.getStatus()).body(response);
		} else {
			return null;
		}
	}

	@GetMapping("/getSubServicesByServiceId/{serviceId}")
	public ResponseEntity<?> getSubServicesByServiceId(@PathVariable String serviceId) {
		Response response = serviceImpl.getSubServicesByServiceId(serviceId);
		if (response != null) {
			return ResponseEntity.status(response.getStatus()).body(response);
		} else {
			return null;
		}
	}

	@GetMapping("/getSubServiceBySubServiceId/{subServiceId}")
	public ResponseEntity<?> getSubServiceBySubServiceId(@PathVariable String subServiceId) {
		Response response = serviceImpl.getSubServiceBySubServiceId(subServiceId);
		if (response != null) {
			return ResponseEntity.status(response.getStatus()).body(response);
		} else {
			return null;
		}
	}

	@GetMapping("/getAllSubServices")
	public ResponseEntity<?> getAllSubServices() {
		Response response = serviceImpl.getAllSubServices();
		if (response != null) {
			return ResponseEntity.status(response.getStatus()).body(response);
		} else {
			return null;
		}
	}

	@PutMapping("/updateBySubServiceId/{subServiceId}")
	public ResponseEntity<?> updateBySubServiceId(@PathVariable String subServiceId,
			@RequestBody SubServicesInfoDto domainServices) {
		Response response = serviceImpl.updateBySubServiceId(subServiceId, domainServices);
		if (response != null) {
			return ResponseEntity.status(response.getStatus()).body(response);
		} else {
			return null;
		}
	}

	@DeleteMapping("/deleteSubService/{subServiceId}")
	public ResponseEntity<?> deleteSubService(@PathVariable String subServiceId) {
		Response response = serviceImpl.deleteSubService(subServiceId);
		if (response != null) {
			return ResponseEntity.status(response.getStatus()).body(response);
		} else {
			return null;
		}
	}

	@GetMapping("/exists/{id}")
	public boolean isSubServiceExists(@PathVariable("id") String id) {
	    return subServicesInfoRepository.existsBySubServices_SubServiceId(id);
	}

}
