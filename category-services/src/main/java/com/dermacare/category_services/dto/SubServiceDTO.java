package com.dermacare.category_services.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubServiceDTO {
    private String subServiceId;
    private String subServiceName;
    private String serviceName;
    private String serviceId;
}

