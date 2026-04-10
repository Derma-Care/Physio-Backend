package com.dermacare.category_services.service.Impl;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.dermacare.category_services.dto.ServicesDto;
import com.dermacare.category_services.entity.Category;
import com.dermacare.category_services.entity.Services;
import com.dermacare.category_services.entity.SubServiceInfoEntity;
import com.dermacare.category_services.entity.SubServices;
import com.dermacare.category_services.entity.SubServicesInfoEntity;
import com.dermacare.category_services.repository.CategoryRepository;
import com.dermacare.category_services.repository.ServicesRepository;
import com.dermacare.category_services.repository.SubServiceRepository;
import com.dermacare.category_services.repository.SubServicesInfoRepository;
import com.dermacare.category_services.service.ServicesService;
import com.dermacare.category_services.util.HelperForConversion;
import com.dermacare.category_services.util.ResponseStructure;

@Service
public class ServicesServiceImpl implements ServicesService {

	@Autowired
	private ServicesRepository servicesRepository;

	@Autowired
	private CategoryRepository categoryRepository;
	
	@Autowired
	public SubServiceRepository subServiceRepository;
	
	@Autowired
	private SubServicesInfoRepository subServicesInfoRepository;
	
	public ResponseStructure<ServicesDto> addService(ServicesDto dto) {
	    try {
	        // Validate serviceName
	        if (dto.getServiceName() == null || dto.getServiceName().trim().isEmpty()) {
	            return ResponseStructure.buildResponse(null,
	                "Service name must not be null or empty.",
	                HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value());
	        }

	        // Validate categoryId
	        if (dto.getCategoryId() == null || dto.getCategoryId().trim().isEmpty()) {
	            return ResponseStructure.buildResponse(null,
	                "Category ID must not be null or empty.",
	                HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value());
	        }

	        // Optional validation: you can also check if image and description are empty strings
	        if (dto.getServiceImage() != null && dto.getServiceImage().trim().isEmpty()) {
	            return ResponseStructure.buildResponse(null,
	                "Service image is provided but empty.",
	                HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value());
	        }

	        Optional<Category> optionalCategory = categoryRepository.findById(new ObjectId(dto.getCategoryId()));
	        if (optionalCategory.isEmpty()) {
	            return ResponseStructure.buildResponse(null,
	                "Invalid categoryId: " + dto.getCategoryId(),
	                HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value());
	        }

	        Category category = optionalCategory.get();
	        dto.setCategoryName(category.getCategoryName());

	        Services service = HelperForConversion.toEntity(dto);
	        Services savedService = servicesRepository.save(service);

	        return ResponseStructure.buildResponse(HelperForConversion.toDto(savedService),
	            "Service Created Successfully", HttpStatus.CREATED, HttpStatus.CREATED.value());

	    } catch (Exception e) {
	        return ResponseStructure.buildResponse(null,
	            "An unexpected error occurred: " + e.getMessage(),
	            HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
	    }
	}



	public List<ServicesDto> getServicesByCategoryId(String categoryId) {

		List<Services> listOfService = servicesRepository.findByCategoryId(new ObjectId(categoryId));

		if (listOfService.isEmpty()) {
			return null;
		}
		return HelperForConversion.toServiceDtos(listOfService);
	}

	public ServicesDto getServiceById(String serviceId) {
		Services service = servicesRepository.findById(new ObjectId(serviceId)).
				orElseThrow(()-> new RuntimeException("Service Not Found With ServiceId: "+serviceId));
		if (service == null) {
			return null;
		}
		return HelperForConversion.toDto(service);
	}

	
	public void deleteServiceById(String serviceId) {
		servicesRepository.deleteById(new ObjectId(serviceId));
	}

	
	public ResponseEntity<ResponseStructure<ServicesDto>> updateService(String serviceId, ServicesDto dto) {
		 ResponseStructure<ServicesDto> structure = new ResponseStructure<>();
		try {
	        ObjectId serviceObjectId = new ObjectId(serviceId);
	        // Step 1: Find existing service
	        Optional<Services> optionalService = servicesRepository.findById(serviceObjectId);
	        if (optionalService.isEmpty()) {
	  	                ResponseStructure.buildResponse(null,
	                    "Service not found with ID: " + serviceId,
	                    HttpStatus.NOT_FOUND,
	                    HttpStatus.NOT_FOUND.value()
	            );
	        }
	        // Step 2: Validate new service name
	        if (dto.getServiceName() == null || dto.getServiceName().trim().isEmpty()) {
	           
	                ResponseStructure.buildResponse(null,
	                    "Service name must not be null or empty.",
	                    HttpStatus.BAD_REQUEST,
	                    HttpStatus.BAD_REQUEST.value()
	            );
	        }	    
	        // Step 4: Validate category
	        Optional<Category> optionalCategory = categoryRepository.findById(new ObjectId(dto.getCategoryId()));
	        if (optionalCategory.isEmpty()) {
	          
	                ResponseStructure.buildResponse(null,
	                    "Invalid categoryId: " + dto.getCategoryId(),
	                    HttpStatus.BAD_REQUEST,
	                    HttpStatus.BAD_REQUEST.value()
	            );
	        }	        
	        Services services = optionalService.get();
	        if(services != null) {
	        if(services.getServiceName().equalsIgnoreCase(dto.getServiceName())) {
	        	structure = updatingServiceByCheckingServiceName(optionalCategory,optionalService,
	        dto,serviceId);
	        }else{
	        List<Services> servicesObject = servicesRepository.findByServiceNameIgnoreCase(dto.getServiceName());
	        if(servicesObject != null && !servicesObject.isEmpty()) {
	        	structure = 
	 	            ResponseStructure.buildResponse(null,
	 	                "Service Name Already Exist",
	 	                HttpStatus.CONFLICT,
	 	                HttpStatus.CONFLICT.value()
	 	        );}else {
	 	        	structure = updatingServiceByCheckingServiceName(optionalCategory,optionalService,
	 	        	        dto,serviceId);
	 	        }}
	    }}catch (Exception e) {
	       
	    	structure = ResponseStructure.buildResponse(null,
	                "An unexpected error occurred: " + e.getMessage(),
	                HttpStatus.INTERNAL_SERVER_ERROR,
	                HttpStatus.INTERNAL_SERVER_ERROR.value()
	        );
	    }
		
		return ResponseEntity.status(structure.getStatusCode()).body(structure);
	}

		
	 private ResponseStructure<ServicesDto> updatingServiceByCheckingServiceName( Optional<Category> optionalCategory,Optional<Services> optionalService,
			    ServicesDto dto,String serviceId ){
			    	try {
			    	ObjectId serviceObjectId = new ObjectId(serviceId);
			        Category category = optionalCategory.get();
			        Services existingService = optionalService.get();

			        existingService.setServiceName(dto.getServiceName().trim());
			        existingService.setCategoryId(new ObjectId(dto.getCategoryId()));
			        existingService.setCategoryName(category.getCategoryName());

			        if (dto.getDescription() != null) {
			            existingService.setDescription(dto.getDescription());
			        }

			        if (dto.getServiceImage() != null) {
			            existingService.setServiceImage(Base64.getDecoder().decode(dto.getServiceImage()));
			        }

			        Services updatedService = servicesRepository.save(existingService);

			        // Step 6: Update related SubServices
			        List<SubServices> relatedSubServices = subServiceRepository.findByServiceId(serviceObjectId);
			        for (SubServices sub : relatedSubServices) {
			            sub.setServiceName(dto.getServiceName().trim());
			        }
			        subServiceRepository.saveAll(relatedSubServices);

			        // Step 7: Update nested serviceName in SubServicesInfoEntity
			        List<SubServicesInfoEntity> allInfoEntities = subServicesInfoRepository.findAll();
			        List<SubServicesInfoEntity> toUpdateInfoEntities = new ArrayList<>();

			        for (SubServicesInfoEntity infoEntity : allInfoEntities) {
			            boolean updated = false;
			            List<SubServiceInfoEntity> updatedSubList = new ArrayList<>();

			            for (SubServiceInfoEntity sub : infoEntity.getSubServices()) {
			                if (serviceId.equals(sub.getServiceId())) {
			                    sub.setServiceName(dto.getServiceName().trim());
			                    updated = true;
			                }
			                updatedSubList.add(sub);
			            }
			            if (updated) {
			                infoEntity.setSubServices(updatedSubList);
			                toUpdateInfoEntities.add(infoEntity);
			            }
			        }
			        if (!toUpdateInfoEntities.isEmpty()) {
			            subServicesInfoRepository.saveAll(toUpdateInfoEntities);
			        }			       
			           return ResponseStructure.buildResponse(
			                HelperForConversion.toDto(updatedService),
			                "Service updated successfully",
			                HttpStatus.OK,
			                HttpStatus.OK.value()
			            );
			         }catch(Exception e) {
			        	 return ResponseStructure.buildResponse(
					               null,
					                e.getMessage(),
					                HttpStatus.INTERNAL_SERVER_ERROR,
					                HttpStatus.INTERNAL_SERVER_ERROR.value()
					            );
			         }
	 }
	 

	public void deleteService(String serviceId) {
	    ObjectId serviceObjectId;

	    // Step 1: Validate ObjectId
	    try {
	        serviceObjectId = new ObjectId(serviceId);
	    } catch (IllegalArgumentException e) {
	        throw new RuntimeException("Invalid Service ID format: " + serviceId);
	    }

	    // Step 2: Fetch the service
	    Services existingService = servicesRepository.findById(serviceObjectId)
	        .orElseThrow(() -> new RuntimeException("Service not found with ID: " + serviceId));

	    String serviceName = existingService.getServiceName();

	    // Step 3: Delete related SubServices
	    List<SubServices> relatedSubServices = subServiceRepository.findByServiceId(serviceObjectId);
	    if (!relatedSubServices.isEmpty()) {
	        subServiceRepository.deleteAll(relatedSubServices);
	        System.out.println("Deleted SubServices: " + relatedSubServices.size());
	    } else {
	        System.out.println("No related SubServices found.");
	    }

	    // Step 4: Delete related SubServicesInfoEntity entries (manual filter)
	    List<SubServicesInfoEntity> allInfoEntities = subServicesInfoRepository.findAll();
	    List<SubServicesInfoEntity> relatedInfoEntities = allInfoEntities.stream()
	        .filter(info -> info.getSubServices().stream()
	            .anyMatch(sub -> serviceId.equals(sub.getServiceId())))
	        .collect(Collectors.toList());

	    if (!relatedInfoEntities.isEmpty()) {
	        subServicesInfoRepository.deleteAll(relatedInfoEntities);
	        System.out.println("Deleted SubServicesInfoEntities: " + relatedInfoEntities.size());
	    } else {
	        System.out.println("No related SubServicesInfoEntities found.");
	    }

	    // Step 5: Delete the service itself
	    servicesRepository.deleteById(serviceObjectId);
	    System.out.println("Service deleted successfully: " + serviceId);
	}



	public void deleteServicesByCategoryId(ObjectId objectId) {
		List<Services> listOfSubServices = servicesRepository.findByCategoryId(objectId);
		if (!listOfSubServices.isEmpty()) {
			servicesRepository.deleteAllByCategoryId(objectId);
		}
	}

	public List<ServicesDto> getAllServices() {

		List<Services> listOfServices = servicesRepository.findAll();
		if (listOfServices.isEmpty()) {
			return new ArrayList<>();
		}
		return HelperForConversion.toServiceDtos(listOfServices);
	}

	public boolean checkServiceExistsAlready(String categoryId, String serviceName) {
		try {
		Services res = servicesRepository
				.findByCategoryIdAndServiceNameIgnoreCase(new ObjectId(categoryId), serviceName);
		if(res != null) {
		return true;}
		else {
			return false;
		}
		}catch(Exception e) {
			return false;
		}
	}
	
}