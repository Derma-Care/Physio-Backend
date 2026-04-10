package com.dermacare.category_services.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SubServicesInfoDto {
	private String id;
	private String categoryId;
	private String categoryName;
	private List<SubServiceDTO> subServices;

}

