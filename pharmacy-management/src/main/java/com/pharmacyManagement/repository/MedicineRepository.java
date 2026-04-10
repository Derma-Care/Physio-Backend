package com.pharmacyManagement.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.pharmacyManagement.entity.Medicine;

@Repository
public interface MedicineRepository extends MongoRepository<Medicine, String> {
	
	Optional<Medicine> findByBarcode(String barcode);

}