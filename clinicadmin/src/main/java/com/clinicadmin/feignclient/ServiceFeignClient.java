package com.clinicadmin.feignclient;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.clinicadmin.dto.CategoryDto;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.ServicesDto;
import com.clinicadmin.dto.SubServicesDto;



@FeignClient(name = "category-services")
public interface ServiceFeignClient {


    @PostMapping("/api/v1/subServices/addSubService/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> addService(@PathVariable String subServiceId, @RequestBody SubServicesDto dto);


    @GetMapping("/api/v1/subServices/getSubServicesbycategoryId/{categoryId}")
    ResponseEntity<ResponseStructure<List<SubServicesDto>>> getSubServiceByIdCategory(@PathVariable String categoryId);

    @GetMapping("/api/v1/subServices/getSubServicesbyserviceId/{serviceId}")
    ResponseEntity<ResponseStructure<List<SubServicesDto>>> getSubServicesByServiceId(@PathVariable String serviceId);

    @GetMapping("/api/v1/subServices/getSubService/{subServiceId}")
    ResponseEntity<ResponseStructure<SubServicesDto>> getSubServiceByServiceId(@PathVariable String subServiceId);


	@DeleteMapping("/api/v1/subServices/deleteBySubServiceId/{hospitalId}/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> deleteSubService(@PathVariable String hospitalId,@PathVariable String subServiceId);

	@PutMapping("/api/v1/subServices/updateSubService/{hospitalId}/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> updateBySubServiceId(@PathVariable String hospitalId,@PathVariable String subServiceId,
			@RequestBody SubServicesDto domainServices);

    @GetMapping("/api/v1/subServices/getAllSubServices")  
    ResponseEntity<ResponseStructure<List<SubServicesDto>>> getAllSubServices();
    
	@GetMapping("/api/v1/subServices/getSubService/{hospitalId}/{subServiceId}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> getSubServiceByServiceId(@PathVariable String hospitalId, @PathVariable String subServiceId);
	
	@GetMapping("/api/v1/subServices/getSubService/{hospitalId}")
	public ResponseEntity<ResponseStructure<List<SubServicesDto>>> getSubServiceByHospitalId(@PathVariable String hospitalId);
	// ------------------------------------Amount calculation using cosultation type (1= FOC,2=paid)-----------------------

	
	@GetMapping("/api/v1/subServices/calculateAmountByConsultationType/{hospitalId}/{subServiceId}/{subServiceName}/{consultationType}")
	public ResponseEntity<ResponseStructure<SubServicesDto>> getSubServiceCostByConsultationType(
	        @PathVariable String hospitalId,
	        @PathVariable String subServiceId,
	        @PathVariable String subServiceName,
	        @PathVariable int consultationType);
    
	@GetMapping("/api/v1/SubServicesInfo/exists/{id}")
	public boolean isSubServiceExists(@PathVariable("id") String id);
    /*  ---------------------------------------------------------------------------------------------------------------------------------------------
	-----------------------------------------------------   Category       ----------------------------------------------------------------------
	--------------------------------------------------------------------------------------------------------------------------------------------- */
	
	@GetMapping("/api/v1/category/getCategory/{categoryId}")
	public ResponseEntity<ResponseStructure<CategoryDto>> getCategoryById(@PathVariable  String  categoryId);
    
    @GetMapping("/api/v1/category/getCategories")
	public ResponseEntity<ResponseStructure<List<CategoryDto>>> getAllCategory();
    
    @GetMapping("/api/v1/category/exists/{id}")
    boolean isCategoryExists(@PathVariable("id") String id);


    /*  -----------------------------------------------------------------------------------------------------------------------------------------
 	-----------------------------------------------------   Services       ----------------------------------------------------------------------
	--------------------------------------------------------------------------------------------------------------------------------------------- */
	@GetMapping("/api/v1/services/getServices/{categoryId}")
	public ResponseEntity<ResponseStructure<List<ServicesDto>>> getServiceById(@PathVariable String categoryId);
     
	@GetMapping("/api/v1/services/getService/{serviceId}")
	public ResponseEntity<ResponseStructure<ServicesDto>> getServiceByServiceId(@PathVariable String serviceId);
    
	@GetMapping("/api/v1/services/getAllServices")
	public ResponseEntity<ResponseStructure<List<ServicesDto>>> getAllServices();
	
	@GetMapping("/api/v1/services/exists/{id}")
	boolean isServiceExists(@PathVariable("id") String id);
    
    
}