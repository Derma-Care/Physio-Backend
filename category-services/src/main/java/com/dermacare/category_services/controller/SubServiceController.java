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

import com.dermacare.category_services.dto.CategoryServiceandSubserviceDto;
import com.dermacare.category_services.dto.SubServicesDto;
import com.dermacare.category_services.repository.SubServicesInfoRepository;
import com.dermacare.category_services.service.SubServicesService;
import com.dermacare.category_services.service.Impl.SubServicesServiceImpl;
import com.dermacare.category_services.util.ResponseStructure;

@RestController
@RequestMapping("/v1/subServices")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class SubServiceController {

	@Autowired
	private SubServicesService subServiceService;

	@PostMapping("/addSubService/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> addService(@PathVariable String subServiceId,
			@RequestBody SubServicesDto dto) {
		return subServiceService.addSubService(subServiceId, dto);
	}

	@GetMapping("/getSubServicesbycategoryId/{categoryId}")
	public ResponseEntity<ResponseStructure<List<SubServicesDto>>> getSubServiceByIdCategory(
			@PathVariable String categoryId) {
		try {
			new ObjectId(categoryId);
		} catch (Exception e) {
			return new ResponseEntity<>(
					ResponseStructure.buildResponse(new ArrayList<>(), "In Valid Category Id  ID must be HexaString",
							HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value()),
					HttpStatus.BAD_REQUEST);
		}
		List<SubServicesDto> servicesList = subServiceService.getSubServicesByCategoryId(categoryId);

		if (servicesList == null || servicesList.isEmpty()) {
			servicesList = new ArrayList<>();
			return new ResponseEntity<>(ResponseStructure.buildResponse(servicesList,
					"No Details Found With Category Id : " + categoryId + " .", HttpStatus.OK, HttpStatus.OK.value()),
					HttpStatus.OK);
		}

		return new ResponseEntity<>(ResponseStructure.buildResponse(servicesList, "Services found successfully",
				HttpStatus.OK, HttpStatus.OK.value()), HttpStatus.OK);
	}

	@GetMapping("/getSubServicesbyserviceId/{serviceId}")
	public ResponseEntity<ResponseStructure<List<SubServicesDto>>> getSubServicesByServiceId(
			@PathVariable String serviceId) {

		try {
			new ObjectId(serviceId);
		} catch (Exception e) {
			return new ResponseEntity<>(
					ResponseStructure.buildResponse(new ArrayList<>(), "In Valid Service Id  ID must be HexaString",
							HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value()),
					HttpStatus.BAD_REQUEST);
		}
		List<SubServicesDto> servicesList = subServiceService.getSubServicesByServiceId(serviceId);

		if (servicesList == null || servicesList.isEmpty()) {
			servicesList = new ArrayList<>();
			return new ResponseEntity<>(ResponseStructure.buildResponse(servicesList,
					"No Details Found With Service Id : " + serviceId + " .", HttpStatus.OK, HttpStatus.OK.value()),
					HttpStatus.OK);
		}

		return new ResponseEntity<>(ResponseStructure.buildResponse(servicesList, "SubServices found successfully",
				HttpStatus.OK, HttpStatus.OK.value()), HttpStatus.OK);
	}

	@GetMapping("/getSubService/{hospitalId}/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> getSubServiceByServiceId(@PathVariable String hospitalId,
			@PathVariable String subServiceId) {
		try {
			new ObjectId(subServiceId);
		} catch (Exception e) {
			return new ResponseEntity<>(
					ResponseStructure.buildResponse(null, "In Valid Service Id  ID must be HexaString",
							HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value()),
					HttpStatus.BAD_REQUEST);
		}

		SubServicesDto servicedomain = subServiceService.getSubServiceById(hospitalId, subServiceId);
		if (servicedomain != null) {
			return new ResponseEntity<>(ResponseStructure.buildResponse(servicedomain, "Services found successfully",
					HttpStatus.OK, HttpStatus.OK.value()), HttpStatus.OK);
		}
		return new ResponseEntity<>(ResponseStructure.buildResponse(null,
				"No Details Found With Service Id : " + subServiceId + " .", HttpStatus.OK, HttpStatus.OK.value()),
				HttpStatus.OK);
	}

	// -----------------------------------Get subservice by
	// hospitalId--------------------------------------------------------------

	@GetMapping("/getSubService/{hospitalId}")
	public ResponseEntity<ResponseStructure<List<SubServicesDto>>> getSubServiceByHospitalId(
			@PathVariable String hospitalId) {

		List<SubServicesDto> subServiceList = subServiceService.getSubServiceByHospitalId(hospitalId);

		if (subServiceList != null && !subServiceList.isEmpty()) {
			return new ResponseEntity<>(ResponseStructure.buildResponse(subServiceList, "Services found successfully",
					HttpStatus.OK, HttpStatus.OK.value()), HttpStatus.OK);
		}

		return new ResponseEntity<>(ResponseStructure.buildResponse(null,
				"No Details Found With Hospital Id: " + hospitalId + ".", HttpStatus.OK, HttpStatus.OK.value()),
				HttpStatus.OK);
	}

	@DeleteMapping("/deleteBySubServiceId/{hospitalId}/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> deleteSubService(@PathVariable String hospitalId,
			@PathVariable String subServiceId) {
		try {
			new ObjectId(subServiceId);
		} catch (Exception e) {
			return new ResponseEntity<>(
					ResponseStructure.buildResponse(null, "In Valid SubService Id  ID must be HexaString",
							HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value()),
					HttpStatus.BAD_REQUEST);
		}

		SubServicesDto servicedomain = subServiceService.getSubServiceById(hospitalId, subServiceId);
		if (servicedomain != null) {
			subServiceService.deleteSubServiceById(hospitalId, subServiceId);
			return new ResponseEntity<>(ResponseStructure.buildResponse(servicedomain,
					"SubService Deleted successfully", HttpStatus.OK, HttpStatus.OK.value()), HttpStatus.OK);

		}
		return new ResponseEntity<>(ResponseStructure.buildResponse(null,
				"Invalid SubService ID : " + subServiceId + " .", HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value()),
				HttpStatus.OK);
	}

	@PutMapping("/updateSubService/{hospitalId}/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> updateBySubServiceId(@PathVariable String hospitalId,
			@PathVariable String subServiceId, @RequestBody SubServicesDto domainServices) {

		try {
			new ObjectId(subServiceId);
		} catch (Exception e) {
			return new ResponseEntity<>(
					ResponseStructure.buildResponse(null, "In Valid SubService Id  ID must be HexaString",
							HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value()),
					HttpStatus.BAD_REQUEST);
		}

		SubServicesDto domain = subServiceService.updateSubService(hospitalId, subServiceId, domainServices);
		if (domain != null) {
			return new ResponseEntity<>(ResponseStructure.buildResponse(domain, "SubsService Updated Sucessfully",
					HttpStatus.OK, HttpStatus.OK.value()), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(ResponseStructure.buildResponse(null, "Subservice Not Found",
					HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping("/getAllSubServices")
	public ResponseEntity<ResponseStructure<List<SubServicesDto>>> getAllSubServices() {
		List<SubServicesDto> dtos = subServiceService.getAllSubService();

		if (dtos == null || dtos.isEmpty()) {
			return ResponseEntity.ok(ResponseStructure.buildResponse(new ArrayList<>(), "No services available",
					HttpStatus.OK, HttpStatus.OK.value()));
		}
		return ResponseEntity.ok(ResponseStructure.buildResponse(dtos, "SubServices fetched Successfully",
				HttpStatus.OK, HttpStatus.OK.value()));
	}

	@GetMapping("/groupedSubServices")
	public ResponseEntity<ResponseStructure<List<CategoryServiceandSubserviceDto>>> getGroupedSubServices() {
		List<CategoryServiceandSubserviceDto> groupedList = subServiceService.getGroupedSubServices();

		if (groupedList == null || groupedList.isEmpty()) {
			return new ResponseEntity<>(ResponseStructure.buildResponse(new ArrayList<>(), "No SubServices Found",
					HttpStatus.OK, HttpStatus.OK.value()), HttpStatus.OK);
		}

		return new ResponseEntity<>(ResponseStructure.buildResponse(groupedList, "SubServices Grouped Successfully",
				HttpStatus.OK, HttpStatus.OK.value()), HttpStatus.OK);
	}

	@GetMapping("/retrieveSubServicesBySubServiceId/{subServiceId}")
	public ResponseEntity<ResponseStructure<List<SubServicesDto>>> retrieveSubServicesBySubServiceId(
			@PathVariable String subServiceId) {
		// System.out.println("invke");
		return subServiceService.retrieveSubServicesBySubServiceId(subServiceId);
	}
 
	// ------------------------------------Amount calculation using cosultation type
	// (1= FOC,2=paid)-----------------------
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
