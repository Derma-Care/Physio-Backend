package com.dermacare.category_services.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubServiceInfoEntity {
	
	private String subServiceId;
	private String subServiceName;
	private String serviceId;
	private String serviceName;
	
}
