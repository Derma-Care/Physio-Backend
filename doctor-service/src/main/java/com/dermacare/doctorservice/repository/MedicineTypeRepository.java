package com.dermacare.doctorservice.repository;



import org.springframework.data.mongodb.repository.MongoRepository;

import com.dermacare.doctorservice.model.MedicineType;

public interface MedicineTypeRepository extends MongoRepository<MedicineType, String> {

}
