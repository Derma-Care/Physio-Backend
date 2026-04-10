package com.AdminService.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SubServicesInfoEntity {
	
	private String id;
	private String categoryId;
	private String categoryName;
	private String serviceId;
	private String serviceName;
	private List<SubServiceInfoEntity> subServices;

}
