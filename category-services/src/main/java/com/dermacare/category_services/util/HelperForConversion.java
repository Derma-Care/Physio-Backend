package com.dermacare.category_services.util;

import java.util.Base64;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;

import com.dermacare.category_services.dto.CategoryDto;
import com.dermacare.category_services.dto.ServicesDto;
import com.dermacare.category_services.dto.SubServicesDto;
import com.dermacare.category_services.entity.Category;
import com.dermacare.category_services.entity.Services;
import com.dermacare.category_services.entity.SubServices;

public class HelperForConversion {
	
	
	public static Services toEntity(ServicesDto dto) {
		Services entity = new Services();
		BeanUtils.copyProperties(dto, entity);
		entity.setServiceImage(base64ToByteArray(dto.getServiceImage()));
		entity.setCategoryId(new ObjectId(dto.getCategoryId()));
		return entity;
	}
	
	public static ServicesDto toDto(Services entity) {
		ServicesDto dto = new ServicesDto();
		BeanUtils.copyProperties(entity, dto);
		dto.setServiceImage(byteArrayToBase64(entity.getServiceImage()));
		dto.setCategoryId(entity.getCategoryId().toString());
		dto.setServiceId(entity.getServiceId().toString());
		return dto;
	}
	
	
	public static SubServices toEntity(SubServicesDto dto) {
		SubServices entity = new SubServices();
		BeanUtils.copyProperties(dto, entity);
		
		entity.setSubServiceId(new ObjectId(dto.getSubServiceId()));
		entity.setCategoryId(new ObjectId(dto.getCategoryId()));
		entity.setCategoryName(dto.getCategoryName());
		entity.setServiceId(new ObjectId(dto.getServiceId()));
		entity.setServiceName(dto.getServiceName());
		entity.setServiceId(new ObjectId(dto.getServiceId()));
		entity.setSubServiceImage(base64ToByteArray(dto.getSubServiceImage()));
		return entity;
	}
	
	public static SubServicesDto toDto(SubServices entity) {
        SubServicesDto dto = new SubServicesDto();
        BeanUtils.copyProperties(entity, dto);

        dto.setSubServiceId(entity.getSubServiceId() != null ? entity.getSubServiceId().toString() : null);
        dto.setCategoryId(entity.getCategoryId() != null ? entity.getCategoryId().toString() : null);
        dto.setServiceId(entity.getServiceId() != null ? entity.getServiceId().toString() : null);
        dto.setSubServiceImage(byteArrayToBase64(entity.getSubServiceImage()));

        return dto;
    }
	
	
	public static byte[] base64ToByteArray(String base64String) {
		return Base64.getDecoder().decode(base64String);
	}

	public static String byteArrayToBase64(byte[] byteArray) {
		return Base64.getEncoder().encodeToString(byteArray);
	}
	
	
	public static Category toEntity(CategoryDto dto) {
		Category category = new Category();
		BeanUtils.copyProperties(dto, category);
		category.setCategoryImage(base64ToByteArray(dto.getCategoryImage()));
		return category;
	}

	public static CategoryDto toDto(Category category) {
		CategoryDto dto = new CategoryDto();
		dto.setCategoryImage(byteArrayToBase64(category.getCategoryImage()));
		dto.setCategoryId(category.getCategoryId().toString());
		dto.setCategoryName(category.getCategoryName());
		dto.setDescription(category.getDescription());
		return dto;
	}
	
	public static List<CategoryDto> converToDtos(List<Category> listOfCategories) {
		return listOfCategories.stream().map(HelperForConversion::toDto).toList();
	}

	public static List<SubServicesDto> toDtos(List<SubServices> subservicesList) {
		return subservicesList.stream().map(HelperForConversion::toDto).toList();
	}
	
	public static List<ServicesDto> toServiceDtos(List<Services> servicesList) {
		return servicesList.stream().map(HelperForConversion::toDto).toList();
	}

}