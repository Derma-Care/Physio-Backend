package com.chiselon.physiotherapydoctor.repository;



import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.chiselon.physiotherapydoctor.entity.DoctorTemplate;

@Repository
public interface DoctorTemplateRepository extends MongoRepository<DoctorTemplate, String> {

	List<DoctorTemplate> findByClinicId(String clinicId);
}

