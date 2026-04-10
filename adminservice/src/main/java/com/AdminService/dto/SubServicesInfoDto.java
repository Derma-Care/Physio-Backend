package com.AdminService.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SubServicesInfoDto {
	
	private String categoryId;
	private String categoryName;
	private List<SubServiceDTO> subServices;

}

