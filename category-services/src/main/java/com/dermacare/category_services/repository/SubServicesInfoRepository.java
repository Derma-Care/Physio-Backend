package com.dermacare.category_services.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.dermacare.category_services.entity.SubServicesInfoEntity;

public interface SubServicesInfoRepository extends MongoRepository<SubServicesInfoEntity, String> {

	public List<SubServicesInfoEntity> findByCategoryId(String categoryId);
	@Query("{ 'subServices.serviceId': ?0 }")
	public List<SubServicesInfoEntity> findByServiceId(String serviceId);

	public SubServicesInfoEntity findBySubServicesSubServiceId(String subServiceId);

	public SubServicesInfoEntity findByCategoryNameAndSubServicesServiceName(String CategoryName, String ServiceName);

	 boolean existsBySubServices_SubServiceId(String subServiceId);
	public List<SubServicesInfoEntity> findByCategoryName(String categoryName);
	public  SubServicesInfoEntity findBySubServicesSubServiceName(String subServiceName);
	public boolean existsBySubServicesSubServiceNameIgnoreCase(String name);
//	public List<SubServicesInfoEntity> findByServiceName(String serviceName);

}
