package com.dermacare.category_services.service;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;

import com.dermacare.category_services.dto.ServicesDto;
import com.dermacare.category_services.util.ResponseStructure;

public interface ServicesService {
	
	 ResponseStructure<ServicesDto> addService(ServicesDto dto);

	    List<ServicesDto> getServicesByCategoryId(String categoryId);

	    ServicesDto getServiceById(String serviceId);

	    void deleteServiceById(String serviceId);

	    public ResponseEntity<ResponseStructure<ServicesDto>> updateService(String serviceId, ServicesDto dto);

	    void deleteService(String serviceId);

	    void deleteServicesByCategoryId(ObjectId objectId);

	    List<ServicesDto> getAllServices();

	    boolean checkServiceExistsAlready(String categoryId, String serviceName);
	    
	

}
