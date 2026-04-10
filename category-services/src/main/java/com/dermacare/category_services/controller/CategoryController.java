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

import com.dermacare.category_services.dto.CategoryDto;
import com.dermacare.category_services.repository.CategoryRepository;
import com.dermacare.category_services.service.CategoryService;
import com.dermacare.category_services.util.ResponseStructure;

@RestController
@RequestMapping("/v1/category")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class CategoryController {

	@Autowired
	private CategoryService categoryService;

	@Autowired
	CategoryRepository categoryRepository;

	@PostMapping("/addCategory")
	public ResponseEntity<ResponseStructure<CategoryDto>> addNewCategory(@RequestBody CategoryDto dto) {
		
		if (categoryService.existsByCategoryNameIgnoreCase(dto.getCategoryName())) {
			return new ResponseEntity<>(ResponseStructure.buildResponse(null,
					"Category Already Exists With Same Name " + dto.getCategoryName(), HttpStatus.CONFLICT,
					HttpStatus.CONFLICT.value()), HttpStatus.CONFLICT);
		}
		CategoryDto savedEntiy = categoryService.addCategory(dto);
		return new ResponseEntity<>(ResponseStructure.buildResponse(savedEntiy, "Category Saved Sucessfully",
				HttpStatus.CREATED, HttpStatus.CREATED.value()), HttpStatus.OK);
	}


	@GetMapping("/getCategories")
	public ResponseEntity<ResponseStructure<List<CategoryDto>>> getAllCategory() {
		List<CategoryDto> listOfCategories = categoryService.findAllCategories();

		// If no categories exist, return 200 OK with an empty list
		if (listOfCategories == null || listOfCategories.isEmpty()) {
			return ResponseEntity.ok(ResponseStructure.buildResponse(new ArrayList<>(), "No categories available",
					HttpStatus.OK, HttpStatus.OK.value()));
		}

		// If categories exist, return 200 OK with category data
		return ResponseEntity.ok(ResponseStructure.buildResponse(listOfCategories, "Categories fetched successfully",
				HttpStatus.OK, HttpStatus.OK.value()));
	}

	
	@DeleteMapping("/deleteCategory/{categoryId}")
	public ResponseEntity<ResponseStructure<String>> deleteCategory(@PathVariable ObjectId categoryId) {
		try {
			// Call the service to delete category and related services/subservices
			categoryService.deleteCategoryById(categoryId);
			// Build the response
			return new ResponseEntity<>(ResponseStructure.buildResponse(
					"Category and its related services/subservices deleted successfully.", "Category Deleted",
					HttpStatus.OK, HttpStatus.OK.value()), HttpStatus.OK);
		} catch (RuntimeException e) {
			// Handle errors such as category not found
			return new ResponseEntity<>(ResponseStructure.buildResponse(null, e.getMessage(), HttpStatus.OK,
					HttpStatus.OK.value()), HttpStatus.OK);
		}
	}

	@PutMapping("updateCategory/{categoryId}")
	public ResponseEntity<ResponseStructure<CategoryDto>> updateCategory(@PathVariable ObjectId categoryId,
			@RequestBody CategoryDto updatedCategory) {

		return categoryService.updateCategoryById(categoryId, updatedCategory);
		
	}

	@GetMapping("/getCategory/{categoryId}")
	public ResponseEntity<ResponseStructure<CategoryDto>> getCategoryById(@PathVariable String categoryId) {
		CategoryDto dto = categoryService.getCategorById(categoryId);
		try {
			new ObjectId(categoryId);
		} catch (Exception e) {
			return new ResponseEntity<>(ResponseStructure.buildResponse(null, "Invalid Category Id",
					HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
		}
		if(dto != null) {
		return new ResponseEntity<>(ResponseStructure.buildResponse(dto, "Category fetched  SucessFully", HttpStatus.OK,
				HttpStatus.OK.value()), HttpStatus.OK);}
		else {
			return new ResponseEntity<>(ResponseStructure.buildResponse(null, "Category Not Found", HttpStatus.OK,
					HttpStatus.OK.value()), HttpStatus.OK);}
		}
	
//-------------------------------exists categoryId--------------------------------------------------------------
	@GetMapping("/exists/{id}")
	public boolean isCategoryExists(@PathVariable("id") String id) {
		return categoryRepository.existsById(new ObjectId(id));
	}
}
