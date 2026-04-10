package com.dermacare.category_services.dto;
import java.util.List;
import lombok.Data;
@Data
public class CategoryServiceandSubserviceDto {

	    private String categoryId;

	  
	    private String categoryName;

	    
	    private String serviceId;

	 
	    private String serviceName;


	    private List< SubServiceDTO> subServices;
}
