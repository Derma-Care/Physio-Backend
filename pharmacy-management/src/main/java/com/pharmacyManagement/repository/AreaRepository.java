package com.pharmacyManagement.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.pharmacyManagement.entity.Area;

@Repository
public interface AreaRepository extends MongoRepository<Area, String> {
	boolean existsByCityId(String cityId);

	List<Area> findByCityId(String cityId);

	boolean existsByAreaNamesAndCityId(String areaName, String cityId);

	

}
