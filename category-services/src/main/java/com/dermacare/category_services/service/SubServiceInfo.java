package com.dermacare.category_services.service;

import com.dermacare.category_services.dto.SubServicesInfoDto;
import com.dermacare.category_services.util.Response;

public interface SubServiceInfo {
	
	 Response addSubService(SubServicesInfoDto dto);

	    Response getSubServiceByIdCategory(String categoryId);

	    Response getSubServicesByServiceId(String serviceId);

	    Response getSubServiceBySubServiceId(String subServiceId);

	    Response deleteSubService(String subServiceId);

	    Response updateBySubServiceId(String subServiceId, SubServicesInfoDto domainServices);

	    Response getAllSubServices();

}
