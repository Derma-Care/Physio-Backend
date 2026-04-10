package com.clinicadmin.controller;

import java.util.List;

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

import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.SubServicesDto;
import com.clinicadmin.service.SubServiceService;

@RestController
@RequestMapping("/clinic-admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class SubServiceController {

	@Autowired
	SubServiceService subServiceService;

	@PostMapping("/addSubService/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> addSubService(@PathVariable String subServiceId,
			@RequestBody SubServicesDto dto) {
		return subServiceService.addService(subServiceId, dto);
	}

	@GetMapping("/category/{categoryId}")
	public ResponseEntity<ResponseStructure<List<SubServicesDto>>> getSubServicesByCategoryId(
			@PathVariable String categoryId) {
		return subServiceService.getSubServiceByIdCategory(categoryId);
	}

	@GetMapping("/serviceId/{serviceId}")
	public ResponseEntity<ResponseStructure<List<SubServicesDto>>> getSubServiceByServiceId(
			@PathVariable String serviceId) {
		return subServiceService.getSubServicesByServiceId(serviceId);
	}

	@GetMapping("/getSubService/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> getSubServiceBySubServiceId(
			@PathVariable String subServiceId) {
		return subServiceService.getSubServiceByServiceId(subServiceId);
	}

	@DeleteMapping("/deleteSubService/{hospitalId}/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> deleteSubService(@PathVariable String hospitalId,
			@PathVariable String subServiceId) {
		return subServiceService.deleteSubService(hospitalId, subServiceId);
	}

	@PutMapping("/updateSubService/{hospitalId}/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> updateSubService(@PathVariable String hospitalId,
			@PathVariable String subServiceId, @RequestBody SubServicesDto dto) {
		return subServiceService.updateBySubServiceId(hospitalId, subServiceId, dto);

	}

	@GetMapping("/getSubService/{hospitalId}/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> getSubServiceByServiceId(@PathVariable String hospitalId,
			@PathVariable String subServiceId) {
		return subServiceService.getSubServiceByServiceId(hospitalId, subServiceId);
	}

	@GetMapping("/getSubServiceByHospitalId/{hospitalId}")
	public ResponseEntity<ResponseStructure<List<SubServicesDto>>> getSubServiceByHospitalId(
			@PathVariable String hospitalId) {
		return subServiceService.getSubServiceByHospitalId(hospitalId);
	}

	@GetMapping("/subService/getAllSubServies")
	public ResponseEntity<ResponseStructure<List<SubServicesDto>>> getAllSubServices() {
		return subServiceService.getAllSubServices();
	}

	  @GetMapping("/calculateAmountByConsultationType/{hospitalId}/{subServiceId}/{subServiceName}/{consultationType}")
	    public ResponseEntity<ResponseStructure<SubServicesDto>> getSubServiceCostByConsultationType(
	            @PathVariable String hospitalId,
	            @PathVariable String subServiceId,
	            @PathVariable String subServiceName,
	            @PathVariable int consultationType) {

	        return subServiceService.getSubServiceCostByConsultationType(
	                hospitalId, subServiceId, subServiceName, consultationType);
	    }

}