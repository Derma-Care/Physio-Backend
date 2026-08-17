package com.chiselon.physiotherapydoctor.repository;




import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.physiotherapydoctor.entity.MedicineType;

public interface MedicineTypeRepository extends MongoRepository<MedicineType, String> {

}

