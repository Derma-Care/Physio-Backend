package com.AdminService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class SubServiceDTO {
    private String subServiceId;
    private String subServiceName;
    private String serviceName;
    private String serviceId;
	
}

