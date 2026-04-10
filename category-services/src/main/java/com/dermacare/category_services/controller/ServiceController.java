package com.dermacare.category_services.controller;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

import com.dermacare.category_services.dto.ServicesDto;
import com.dermacare.category_services.repository.ServicesRepository;
import com.dermacare.category_services.service.ServicesService;
import com.dermacare.category_services.util.ResponseStructure;

@RestController
@RequestMapping("/v1/services")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class ServiceController {

	@Autowired
	private ServicesService service;

	@Autowired
	private ServicesRepository serviceRepository;

	@PostMapping("/addService")
	public ResponseEntity<ResponseStructure<ServicesDto>> addService(@RequestBody ServicesDto dto) {

	    boolean present = service.checkServiceExistsAlready(dto.getCategoryId(), dto.getServiceName());

	    if (present) {
	        return ResponseEntity.status(HttpStatus.CONFLICT)
	                .body(ResponseStructure.buildResponse(null, "Service already exists in the same category",
	                        HttpStatus.CONFLICT, HttpStatus.CONFLICT.value()));
	    }

	    ResponseStructure<ServicesDto> response = service.addService(dto);
	    
	    return ResponseEntity.status(response.getHttpStatus())
	            .body(response);
	}


	@GetMapping("/getServices/{categoryId}")
	public ResponseEntity<ResponseStructure<List<ServicesDto>>> getServiceById(@PathVariable String categoryId) {

		try {
			new ObjectId(categoryId);
		} catch (Exception e) {
			return new ResponseEntity<>(
					ResponseStructure.buildResponse(new ArrayList<>(), "In Valid Category Id  ID must be HexaString",
							HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value()),
					HttpStatus.BAD_REQUEST);
		}
		List<ServicesDto> servicesList = service.getServicesByCategoryId(categoryId);

		if (servicesList == null || servicesList.isEmpty()) {
			servicesList = new ArrayList<>();
			return new ResponseEntity<>(ResponseStructure.buildResponse(servicesList,
					"No Details Found With Category Id : " + categoryId + " .", HttpStatus.OK,
					HttpStatus.OK.value()), HttpStatus.OK);}
return new ResponseEntity<>(ResponseStructure.buildResponse(servicesList, "Services found successfully",
				HttpStatus.OK, HttpStatus.OK.value()), HttpStatus.OK);
}
	

	@GetMapping("/getService/{serviceId}")
	public ResponseEntity<ResponseStructure<ServicesDto>> getServiceByServiceId(@PathVariable String serviceId) {
		try {
			new ObjectId(serviceId);
		} catch (Exception e) {
			return new ResponseEntity<>(
					ResponseStructure.buildResponse(null, "In Valid Service Id  ID must be HexaString",
							HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value()),
					HttpStatus.BAD_REQUEST);
		}

		ServicesDto servicedomain = service.getServiceById(serviceId);
		if (servicedomain != null) {
			return new ResponseEntity<>(ResponseStructure.buildResponse(servicedomain, "Services found successfully",
					HttpStatus.OK, HttpStatus.OK.value()), HttpStatus.OK);
		}
		return new ResponseEntity<>(
				ResponseStructure.buildResponse(null, "No Details Found With Service Id : " + serviceId + " .",
						HttpStatus.OK, HttpStatus.OK.value()),
				HttpStatus.OK);
	}

	// Endpoint to delete service and related subservices
	@DeleteMapping("/deleteService/{serviceId}")
	public ResponseEntity<ResponseStructure<String>> deleteService(@PathVariable String serviceId) {
		try {
			// Call the service layer to delete the service and its subservices
			service.deleteService(serviceId);

			// Return success response
			return new ResponseEntity<>(
					ResponseStructure.buildResponse("Service and its related subservices deleted successfully.",
							"Service Deleted", HttpStatus.OK, HttpStatus.OK.value()),
					HttpStatus.OK);
		} catch (RuntimeException e) {
			// Handle errors such as service not found
			return new ResponseEntity<>(ResponseStructure.buildResponse(null, e.getMessage(), HttpStatus.NOT_FOUND,
					HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
		}
	}

	@PutMapping("/updateService/{serviceId}")
	public ResponseEntity<ResponseStructure<ServicesDto>> updateByServiceId(
	        @PathVariable String serviceId,
	        @RequestBody ServicesDto domainServices) {

	    try {
	        new ObjectId(serviceId);
	    } catch (IllegalArgumentException e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(ResponseStructure.buildResponse(null,
	                        "Invalid Service ID: must be a valid hexadecimal string.",
	                        HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value()));
	    }	   
	    // Call service layer to handle update logic and response
	    ResponseStructure<ServicesDto> response = service.updateService(serviceId, domainServices).getBody();

	    // Return appropriate HTTP status from response structure
	    return ResponseEntity.status(HttpStatus.valueOf(response.getStatusCode())).body(response);
	}

	
	@GetMapping("/getAllServices")
	public ResponseEntity<ResponseStructure<List<ServicesDto>>> getAllServices() {
		List<ServicesDto> dtos = service.getAllServices();
		if (dtos == null || dtos.isEmpty()) {
			return ResponseEntity.ok(ResponseStructure.buildResponse(new ArrayList<>(), "No services available",
					HttpStatus.OK, HttpStatus.OK.value()));
		}

		// If services exist, return them
		return ResponseEntity.ok(ResponseStructure.buildResponse(dtos, "Services fetched Successfully", HttpStatus.OK,
				HttpStatus.OK.value()));
	}

	// -------------------------------exists ServicesId--------------------------------------------------------------

	@GetMapping("/exists/{id}")
	public boolean isServiceExists(@PathVariable("id") String id) {
		return serviceRepository.existsById(new ObjectId(id));
	}
}