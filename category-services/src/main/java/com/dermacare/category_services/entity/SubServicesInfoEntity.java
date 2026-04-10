package com.dermacare.category_services.entity;

import java.util.List;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SubServicesInfoEntity {
	@Id
	private String id;
	private String categoryId;
	private String categoryName;
	private List<SubServiceInfoEntity> subServices;

}
