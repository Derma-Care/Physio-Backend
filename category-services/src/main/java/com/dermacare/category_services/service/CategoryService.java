package com.dermacare.category_services.service;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;

import com.dermacare.category_services.dto.CategoryDto;
import com.dermacare.category_services.util.ResponseStructure;

public interface CategoryService {

	public CategoryDto addCategory(CategoryDto categoryDomain);

	public boolean existsByCategoryNameIgnoreCase(String categoryName);

	public List<CategoryDto> findAllCategories();

	public CategoryDto getCategorById(String categoryId);

	public ResponseEntity<ResponseStructure<CategoryDto>> updateCategoryById(ObjectId categoryId, CategoryDto updateDto);
	
	public boolean findByCategoryId(String categoryId);

	public void deleteCategoryById(ObjectId categoryId);

}
