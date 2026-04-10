package com.dermacare.category_services.service;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;

import com.dermacare.category_services.dto.CategoryServiceandSubserviceDto;
import com.dermacare.category_services.dto.SubServicesDto;
import com.dermacare.category_services.util.ResponseStructure;

public interface SubServicesService {

	public  ResponseEntity<ResponseStructure<SubServicesDto>> addSubService(String subServiceId,SubServicesDto dto) ;

	    List<SubServicesDto> getSubServicesByServiceId(String serviceId);

	    List<SubServicesDto> getSubServicesByCategoryId(String categoryId);

	    SubServicesDto getSubServiceById(String hospitalId, String subServiceId);

	    void deleteSubServiceById(String hospitalId, String subServiceId);

	    SubServicesDto updateSubService(String hospitalId, String subServiceId, SubServicesDto domainService);

	    void deleteSubServicesByCategoryId(ObjectId objectId);

	    List<SubServicesDto> getAllSubService();

	    boolean checkSubServiceExistsAlready(String categoryId, String subServiceName);

	    List<SubServicesDto> getAllSubServices();

	    boolean checkServiceExistsOrNot(String serviceId);

	    List<CategoryServiceandSubserviceDto> getGroupedSubServices();

	    CategoryServiceandSubserviceDto saveGroupedSubServices(CategoryServiceandSubserviceDto requestDto);
	    
	    List<SubServicesDto> getSubServiceByHospitalId(String hospitalId);
	    
	    public ResponseEntity<ResponseStructure<List<SubServicesDto>>> retrieveSubServicesBySubServiceId(String subServiceId);

	

		ResponseEntity<ResponseStructure<SubServicesDto>> getSubServiceCostByConsultationType(String hospitalId,
				String subServiceId, String subServiceName, int consultationType);
}
