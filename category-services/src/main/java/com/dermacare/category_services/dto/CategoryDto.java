package com.dermacare.category_services.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {
	
	private String categoryId;
	private String categoryName;
	private String description;
	private String categoryImage;


}
