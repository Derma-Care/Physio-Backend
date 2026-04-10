package com.dermacare.category_services.util;

import java.util.List;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dermacare.category_services.dto.CategoryDto;
import com.dermacare.category_services.dto.ServicesDto;
import com.dermacare.category_services.dto.SubServiceDTO;
import com.dermacare.category_services.dto.SubServicesInfoDto;
import com.dermacare.category_services.entity.SubServiceInfoEntity;
import com.dermacare.category_services.entity.SubServicesInfoEntity;
import com.dermacare.category_services.repository.SubServicesInfoRepository;
import com.dermacare.category_services.service.Impl.CategoryServiceImpl;
import com.dermacare.category_services.service.Impl.ServicesServiceImpl;

@Component
public class Converter {

    @Autowired
    private SubServicesInfoRepository subServicesRepository;

    @Autowired
    private CategoryServiceImpl categoryServiceImpl;

    @Autowired
    private ServicesServiceImpl servicesServiceImpl;

    // Converts DTO to Entity
    public SubServicesInfoEntity entityConverter(SubServicesInfoDto dto) {
        SubServicesInfoEntity entity = new SubServicesInfoEntity();
        entity.setCategoryId(dto.getCategoryId());

        CategoryDto category = categoryServiceImpl.getCategorById(dto.getCategoryId());
        if (category == null) {
            throw new IllegalArgumentException("Category not found for ID: " + dto.getCategoryId());
        }
        entity.setCategoryName(category.getCategoryName());

        List<SubServiceInfoEntity> subServiceEntities = dto.getSubServices().stream()
                .map(subServiceDTO -> {
                    String subServiceId = new ObjectId().toHexString();

                    // Fetch service details
                    ServicesDto service = servicesServiceImpl.getServiceById(subServiceDTO.getServiceId());
                    if (service == null) {
                        throw new IllegalArgumentException("Service not found for ID: " + subServiceDTO.getServiceId());
                    }

                    // ✅ Use setters (don't use the constructor)
                    SubServiceInfoEntity subServiceEntity = new SubServiceInfoEntity();
                    subServiceEntity.setSubServiceId(subServiceId);
                    subServiceEntity.setServiceId(service.getServiceId());
                    subServiceEntity.setServiceName(service.getServiceName());
                    subServiceEntity.setSubServiceName(subServiceDTO.getSubServiceName());

                    return subServiceEntity;
                })
                .collect(Collectors.toList());

        entity.setSubServices(subServiceEntities);
        return entity;
    }

    public SubServicesInfoDto dtoConverter(SubServicesInfoEntity entity) {
        SubServicesInfoDto dto = new SubServicesInfoDto();
        dto.setCategoryId(entity.getCategoryId());
        dto.setCategoryName(entity.getCategoryName());
        dto.setId(entity.getId());
        List<SubServiceDTO> subServiceDTOs = entity.getSubServices().stream()
            .map(subServiceEntity -> new SubServiceDTO(	
                subServiceEntity.getSubServiceId(),
                subServiceEntity.getSubServiceName(),
                subServiceEntity.getServiceName(),
                subServiceEntity.getServiceId()
            ))
            .collect(Collectors.toList());

        dto.setSubServices(subServiceDTOs);
        return dto;
    }


}