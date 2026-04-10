package com.pharmacyManagement.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.pharmacyManagement.entity.City;

@Repository
public interface CityRepository extends MongoRepository<City, String> {
	boolean existsByCityNameIgnoreCase(String cityName);
}
