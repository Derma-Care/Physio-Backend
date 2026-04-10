package com.clinicadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicadmin.dto.Response;
import com.clinicadmin.service.CategoryService;

@RestController
@RequestMapping("/clinic-admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class CategoryController {
	
	@Autowired
	private CategoryService categoryService;
	
	@GetMapping("/getCategoryByCategoryId/{categoryId}")
	public ResponseEntity<Response> getCategoryByCategoryId(@PathVariable String categoryId) {
	    Response response = categoryService.getCategoryById(categoryId);
	    return ResponseEntity.status(response.getStatus()).body(response);
	}
	@GetMapping("/getAllCategories")
	public ResponseEntity<Response> getAllCategories() {
	    Response response = categoryService.getAllCategory();
	    return ResponseEntity.status(response.getStatus()).body(response);
	}


}
