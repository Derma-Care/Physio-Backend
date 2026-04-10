package com.dermacare.category_services.service.Impl;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.dermacare.category_services.dto.CategoryDto;
import com.dermacare.category_services.entity.Category;
import com.dermacare.category_services.entity.Services;
import com.dermacare.category_services.entity.SubServices;
import com.dermacare.category_services.entity.SubServicesInfoEntity;
import com.dermacare.category_services.repository.CategoryRepository;
import com.dermacare.category_services.repository.ServicesRepository;
import com.dermacare.category_services.repository.SubServiceRepository;
import com.dermacare.category_services.repository.SubServicesInfoRepository;
import com.dermacare.category_services.service.CategoryService;
import com.dermacare.category_services.service.ServicesService;
import com.dermacare.category_services.util.HelperForConversion;
import com.dermacare.category_services.util.ResponseStructure;

@Service
public class CategoryServiceImpl implements CategoryService {

	@Autowired
	private CategoryRepository repository;

	@Autowired
	private ServicesRepository serviceManagmentRepository;

	@Autowired
	private ServicesService service;
	
	@Autowired
	public SubServiceRepository subServiceRepository;
	
	@Autowired
	private SubServicesServiceImpl subService;
	
	@Autowired
	private SubServicesInfoRepository subServicesInfoRepository;

	public CategoryDto addCategory(CategoryDto dto) {
		Category category = HelperForConversion.toEntity(dto);
		Category savedCategory = repository.save(category);
		return HelperForConversion.toDto(savedCategory);
	}

	public boolean existsByCategoryNameIgnoreCase(String categoryName) {
		 boolean exists = repository.existsByCategoryNameIgnoreCase(categoryName);;
		System.out.println(exists);
		return exists;
	}

	public List<CategoryDto> findAllCategories() {
		List<Category> listOfCategories = repository.findAll();
		if (listOfCategories.isEmpty()) {
			return null;
		}
		return HelperForConversion.converToDtos(listOfCategories);
	}

	public CategoryDto getCategorById(String categoryId) {
		Category category = repository.findById(new ObjectId(categoryId)).orElseThrow(
				()->new RuntimeException("Category Not found With : "+categoryId));
		return HelperForConversion.toDto(category);
	}
	
	

	public ResponseEntity<ResponseStructure<CategoryDto>> updateCategoryById(ObjectId categoryId, CategoryDto updateDto) {
	    ResponseStructure<CategoryDto> response = new ResponseStructure<>();

	    // Step 1: Fetch existing category by ID
	   Optional< Category> existing = repository.findById(categoryId);
	    if (existing.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseStructure.buildResponse(null,
                	"Category not found with ID: " + categoryId,
                    HttpStatus.NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()));}	
	    
	    Category c = existing.get();
	    String oldCategoryName = c.getCategoryName();
	    String newCategoryName = updateDto.getCategoryName();

	    // Step 2: Compare category names
	    if (!oldCategoryName.equalsIgnoreCase(newCategoryName)) {
	        // Step 3: If different, check if new name already exists
	        Optional<Category> existingWithName = repository. findByCategoryNameIgnoreCase(newCategoryName);
	        if (existingWithName.isPresent()) {
	            ResponseStructure<CategoryDto> errorResponse = ResponseStructure.buildResponse(
	                    null,
	                    "Category name already exists: " + newCategoryName,
	                    HttpStatus.BAD_REQUEST,
	                    HttpStatus.BAD_REQUEST.value()
	            );
	            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	        } else {
	            c.setCategoryName(newCategoryName);
	        }
	    }

	    // Step 4: Update remaining fields if they are not null
	    if (updateDto.getDescription() != null) {
	        c.setDescription(updateDto.getDescription());
	    }

	    if (updateDto.getCategoryImage() != null) {
	        byte[] categoryImageBytes = Base64.getDecoder().decode(updateDto.getCategoryImage());
	        c.setCategoryImage(categoryImageBytes);
	    }

	    // Save updated category
	    Category savedCategory = repository.save(c);

	    // Step 5: Update related entities if name changed
	    if (!oldCategoryName.equalsIgnoreCase(savedCategory.getCategoryName())) {
	        List<Services> services = serviceManagmentRepository.findByCategoryId(categoryId);
	        for (Services service : services) {
	            service.setCategoryName(savedCategory.getCategoryName());
	        }
	        serviceManagmentRepository.saveAll(services);

	        List<SubServices> subServices = subServiceRepository.findByCategoryId(categoryId);
	        for (SubServices subService : subServices) {
	            subService.setCategoryName(savedCategory.getCategoryName());
	        }
	        subServiceRepository.saveAll(subServices);

	        List<SubServicesInfoEntity> subServiceInfos = subServicesInfoRepository.findByCategoryId(categoryId.toString());
	        for (SubServicesInfoEntity info : subServiceInfos) {
	            info.setCategoryName(savedCategory.getCategoryName());
	        }
	        subServicesInfoRepository.saveAll(subServiceInfos);
	    }

	    // Step 6: Return successful response
	    CategoryDto resultDto = HelperForConversion.toDto(savedCategory);
	    ResponseStructure<CategoryDto> successResponse = ResponseStructure.buildResponse(
	            resultDto,
	            "Category updated successfully",
	            HttpStatus.OK,
	            HttpStatus.OK.value()
	    );
	    return new ResponseEntity<>(successResponse, HttpStatus.OK);
	}



	@Override
	public boolean findByCategoryId(String categoryId) {
		Optional<Category> optional = repository.findById(new ObjectId());
		return optional.isPresent();
	}

	

	public void deleteCategoryById(ObjectId categoryId) {
	    // Fetch the existing category
	    Category existingCategory = repository.findById(categoryId)
	        .orElseThrow(() -> new RuntimeException("Category not found with ID: " + categoryId));

	    String categoryName = existingCategory.getCategoryName();

	    // Delete related services
	    List<Services> services = serviceManagmentRepository.findByCategoryName(categoryName);
	    if (!services.isEmpty()) {
	        System.out.println("Found Services: " + services.size());
	        serviceManagmentRepository.deleteAll(services);
	    }

	    // Delete related subservices
	    List<SubServices> subServices = subServiceRepository.findByCategoryName(categoryName);
	    if (!subServices.isEmpty()) {
	        System.out.println("Found SubServices: " + subServices.size());
	        subServiceRepository.deleteAll(subServices);
	    }

	    // Delete related SubServiceInfo entities
	    List<SubServicesInfoEntity> subServiceInfos = subServicesInfoRepository.findByCategoryName(categoryName);
	    if (!subServiceInfos.isEmpty()) {
	        System.out.println("Found SubServiceInfos: " + subServiceInfos.size());
	        subServicesInfoRepository.deleteAll(subServiceInfos);
	    }

	    // Finally, delete the category
	    repository.deleteById(categoryId);
	}

}
