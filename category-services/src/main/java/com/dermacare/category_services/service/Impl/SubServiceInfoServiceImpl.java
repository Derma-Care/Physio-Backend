package com.dermacare.category_services.service.Impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dermacare.category_services.dto.SubServiceDTO;
import com.dermacare.category_services.dto.SubServicesInfoDto;
import com.dermacare.category_services.entity.SubServiceInfoEntity;
import com.dermacare.category_services.entity.SubServices;
import com.dermacare.category_services.entity.SubServicesInfoEntity;
import com.dermacare.category_services.repository.SubServiceRepository;
import com.dermacare.category_services.repository.SubServicesInfoRepository;
import com.dermacare.category_services.service.SubServiceInfo;
import com.dermacare.category_services.util.Converter;
import com.dermacare.category_services.util.Response;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SubServiceInfoServiceImpl implements SubServiceInfo {
	
	@Autowired
	private Converter converter;
	
	@Autowired
	private SubServicesInfoRepository subServicesInfoRepository;
	
	@Autowired
	private SubServiceRepository subServiceRepository;
	
		
	public  Response addSubService( SubServicesInfoDto requestDto){
		 Response response = new  Response();
	    	try {
	    	SubServicesInfoEntity entity = converter.entityConverter(requestDto);
	    	if(entity.getCategoryName() == null) {
	    	response.setStatus(404);
  			response.setSuccess(false);
  			response.setMessage("Incorrect CategoryId");
	    	}
	    	for(SubServiceInfoEntity e :entity.getSubServices()) {
	    	if(e.getServiceName() == null) {
	        response.setStatus(404);
  			response.setSuccess(false);
  			response.setMessage("Incorrect ServiceId");
	    	}}
	    	Set<String> uniqueNames = new HashSet<>();
	    	for (SubServiceDTO dto : requestDto.getSubServices()) {
	    	    String name = dto.getSubServiceName().trim();
	    	    if (!uniqueNames.add(name)) {
	    	        response.setStatus(400);
	    	        response.setSuccess(false);
	    	        response.setMessage("Only unique SubService names are allowed. Duplicate found: " + dto.getSubServiceName());
	    	        return response;}
	    	    if (subServicesInfoRepository.existsBySubServicesSubServiceNameIgnoreCase(name)) {
	    	        response.setStatus(400);
	    	        response.setSuccess(false);
	    	        response.setMessage("SubService '" + dto.getSubServiceName() + "' already exists.");
	    	        return response;}} 
	    	    	
	    	SubServicesInfoEntity savedInfo = subServicesInfoRepository.save(entity);
	    	
	    	if(savedInfo != null) {
		    			response.setData(converter.dtoConverter(entity));
		    			response.setStatus(200);
		    			response.setSuccess(true);
		    			response.setMessage("saved successfully");
	    	}else {
  			response.setStatus(404);
  			response.setSuccess(false);
  			response.setMessage(" Failed To AddSubService");}
	    	}catch(Exception ex) {
	    	            response.setStatus(500);
		    			response.setMessage(ex.getMessage());
		    			response.setSuccess(false);
	    	        }
	                    return response;
	    	    } 	 
	
	
	
	public Response getSubServiceByIdCategory(String categoryId){
		 Response response = new  Response();
	    	try {
	   List<SubServicesInfoEntity> subServicesEntity = subServicesInfoRepository.
	    		findByCategoryId(categoryId);
	    if(subServicesEntity != null) {
	    	 List<SubServicesInfoDto> dto = new ObjectMapper().convertValue(subServicesEntity, new TypeReference<List<SubServicesInfoDto>>() {});
		    			response.setData(dto);
		    			response.setStatus(200);
		    			response.setSuccess(true);
		    			response.setMessage("subservices fetched successfully");
	    }else {
			response.setStatus(200);
			response.setSuccess(true);
			response.setMessage("Subservice Not Found With given Id");
	    }
	    }catch(Exception e) {
	    	            response.setStatus(500);
		    			response.setMessage(e.getMessage());
		    			response.setSuccess(false);
	    	        }
	                    return response;
	    	    } 	 
	
	
	public Response getSubServicesByServiceId(String serviceId){
		 Response response = new  Response();
	    	try {
	    		  List<SubServicesInfoEntity> subServicesEntity = subServicesInfoRepository.
	    				  findByServiceId(serviceId);
	    		  if(subServicesEntity != null) {
	    			 
	    			  List<SubServicesInfoDto> dto = new ObjectMapper().convertValue(subServicesEntity, new TypeReference<List<SubServicesInfoDto>>() {});
	    			    			response.setData(dto);
	    			    			response.setStatus(200);
	    			    			response.setSuccess(true);
	    			    			response.setMessage("subservices fetched successfully");
	    		  }else {
	    			  response.setStatus(200);
		    			response.setSuccess(true);
		    			response.setMessage("SubService Not Found With Given Id");
	    		  }
	    		  }catch(Exception e) {
	    		    	            response.setStatus(500);
	    			    			response.setMessage(e.getMessage());
	    			    			response.setSuccess(false);
	    		    	        }
	    		                    return response;
	    	    } 	
	
	
	
	public Response getSubServiceBySubServiceId(String subServiceId){
		 Response response = new  Response();
	    	try {
	    		SubServicesInfoEntity subServicesEntity = 
	    				subServicesInfoRepository.findBySubServicesSubServiceId(subServiceId);
	    				if(subServicesEntity != null) {	  	
	    		    SubServicesInfoDto dto = converter.dtoConverter(subServicesEntity);
	    			    			response.setData(dto);
	    			    			response.setStatus(200);
	    			    			response.setSuccess(true);
	    			    			response.setMessage("subservices fetched successfully");}
	    				else {
			    			response.setStatus(200);
			    			response.setSuccess(true);
			    			response.setMessage("Subservices Not Found With Given Id");
	    				}
	    			    		}catch(Exception e) {
	    		    	            response.setStatus(500);
	    			    			response.setMessage(e.getMessage());
	    			    			response.setSuccess(false);
	    		    	        }
	    		                    return response;
	    	    } 	
	
	
	public Response deleteSubService(String subServiceId) {
	    Response response = new Response();
	    try {
	        // Step 1: Find the parent SubServicesInfoEntity containing the subServiceId
	        SubServicesInfoEntity subServicesEntity = subServicesInfoRepository.findBySubServicesSubServiceId(subServiceId);

	        if (subServicesEntity == null) {
	            response.setStatus(404);
	            response.setSuccess(false);
	            response.setMessage("SubService Not Found With Given Id");
	            return response;
	        }

	        List<SubServiceInfoEntity> subServicesList = subServicesEntity.getSubServices();
	        if (subServicesList == null || subServicesList.isEmpty()) {
	            // If no subservices left in the list, delete the whole parent entity
	            subServicesInfoRepository.delete(subServicesEntity);
	            response.setStatus(200);
	            response.setSuccess(true);
	            response.setMessage("No subservices found, parent entity deleted");
	            return response;
	        }

	        // Step 2: Remove the target subservice from the list
	        boolean removed = subServicesList.removeIf(sub -> subServiceId.equals(sub.getSubServiceId()));

	        if (!removed) {
	            response.setStatus(404);
	            response.setSuccess(false);
	            response.setMessage("SubService Not Found in subservices list");
	            return response;
	        }

	        // Step 3: After removal, if no subservices left in the parent entity, delete the entire entity
	        if (subServicesList.isEmpty()) {
	            subServicesInfoRepository.delete(subServicesEntity);
	            response.setStatus(200);
	            response.setSuccess(true);
	            response.setMessage("SubService deleted and parent entity removed as no subservices left");
	        } else {
	            // Otherwise, update the subservices list in the parent entity
	            subServicesEntity.setSubServices(subServicesList);
	            subServicesInfoRepository.save(subServicesEntity);
	            response.setStatus(200);
	            response.setSuccess(true);
	            response.setMessage("SubService deleted successfully");
	        }

	        // Step 4: Delete all SubServices where this subServiceId exists
	        List<SubServices> relatedSubServices = subServiceRepository.findBySubServiceId(new ObjectId(subServiceId));
	        if (!relatedSubServices.isEmpty()) {
	            subServiceRepository.deleteAll(relatedSubServices);
	            System.out.println("Deleted related SubServices: " + relatedSubServices.size());
	        }

	        // Step 5: Optionally, you can clean up any other references to this subServiceId in other entities here
	        // Example: Delete any references in "SubServicesInfoEntity" or other collections
	        // (You may need to add custom code for this based on your application's needs)

	        response.setStatus(200);
	        response.setSuccess(true);
	        response.setMessage("SubService and related SubServices deleted successfully");

	    } catch (Exception e) {
	        response.setStatus(500);
	        response.setSuccess(false);
	        response.setMessage("Internal server error: " + e.getMessage());
	    }
	    return response;
	}
	
	
	
	public Response updateBySubServiceId(String subServiceId, SubServicesInfoDto domainServices) {
	    Response response = new Response();
	    try {	    		    	
	        SubServicesInfoEntity subServicesEntity = 
	        subServicesInfoRepository.findBySubServicesSubServiceId(subServiceId);
	        		        
	      List<SubServices> subsrvice = subServiceRepository.findBySubServiceId(new ObjectId(subServiceId));
	        if (subServicesEntity == null) {
	            response.setStatus(404);
	            response.setMessage("SubServiceInfo with given ID not found");
	            response.setSuccess(false);
	            return response;
	          }
	            
	        if(subServicesEntity != null) {
	        for(SubServiceDTO s: domainServices.getSubServices()){
	     	String subServiceName = s.getSubServiceName();	     
	        List<SubServiceInfoEntity> listEntity = subServicesEntity.getSubServices();
	        Optional<SubServiceInfoEntity> optional = listEntity.stream().filter(n->n.getSubServiceName().
		            equalsIgnoreCase(subServiceName)).findFirst();	     
		            if(optional.isPresent()) {
		            response = updateSubServiceByCheckingName(subServicesEntity,
		            subsrvice,domainServices,subServiceId);}
		            else {
		    boolean subServicesInfoEntity = subServicesInfoRepository.
		    existsBySubServicesSubServiceNameIgnoreCase(subServiceName);
		    System.out.println(subServicesInfoEntity);
		    if(subServicesInfoEntity) {
		    	response.setStatus(409);
	            response.setMessage("SubService Name Already Exist ");
	            response.setSuccess(false);
		    }else{
		    	 response =	updateSubServiceByCheckingName(subServicesEntity,
		         subsrvice,domainServices,subServiceId);	
		    }}}}}catch(Exception e) {
		    	response.setStatus(500);
	            response.setMessage(e.getMessage());
	            response.setSuccess(false);
		        }
	        return response;
	        }
	        
	             
	        private Response updateSubServiceByCheckingName(SubServicesInfoEntity subServicesEntity,
	        		List<SubServices> subsrvice,SubServicesInfoDto domainServices,String subServiceId) {
	        Response response = new Response();
	        try {
	        List<SubServiceInfoEntity> listEntity = subServicesEntity.getSubServices();
	        if (domainServices.getSubServices() != null) {
	        	List<SubServiceDTO> domain = domainServices.getSubServices(); 
	        	for(SubServiceDTO s: domain ) {
	            Optional<SubServiceInfoEntity> optional = listEntity.stream().filter(n->n.getSubServiceId().
	            equals(subServiceId)).findFirst();
	            if(optional.isPresent()) {
	            optional.get().setSubServiceName(s.getSubServiceName());
	            if(!subsrvice.isEmpty()) {
		        	for(SubServices sub :  subsrvice ) {
		        		sub.setSubServiceName(s.getSubServiceName());
		        		subServiceRepository.save(sub);}}}}
	        subServicesEntity.setSubServices(listEntity);	 
	        subServicesInfoRepository.save(subServicesEntity);
	        response.setStatus(200);
	        response.setSuccess(true);
	        response.setMessage("SubService updated successfully");
	        }} catch (Exception e) {
	        response.setStatus(500);
	        response.setMessage("Error: " + e.getMessage());
	        response.setSuccess(false);
	    }
	    return response;
	}
	
		 	    
	public Response getAllSubServices(){
		 Response response = new  Response();
	    	try {
	    		List<SubServicesInfoEntity> subServicesEntity = subServicesInfoRepository.findAll();
	    		List<SubServicesInfoDto> list = new ArrayList<>();
	    		if(subServicesEntity != null) {
	    		for(SubServicesInfoEntity s :  subServicesEntity ) {
	    		    SubServicesInfoDto dto = converter.dtoConverter(s);
	    		                   list.add(dto); }
	    		                    response.setData(list);
	    			    			response.setStatus(200);
	    			    			response.setSuccess(true);
	    			    			response.setMessage("subservices fetched successfully");
	    		                   }else {
	    		                      	response.setStatus(200);
	    			                    response.setSuccess(true);
	    		                    	response.setMessage("Subservices Not Found with given Id");
	    		                   }
	    		                   }catch(Exception e) {
	    		    	            response.setStatus(500);
	    			    			response.setMessage(e.getMessage());
	    			    			response.setSuccess(false);
	    		    	        }
	    		                    return response;  }
}
