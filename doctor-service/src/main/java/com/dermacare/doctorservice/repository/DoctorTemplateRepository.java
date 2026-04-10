package com.dermacare.doctorservice.repository;


import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dermacare.doctorservice.model.DoctorTemplate;

@Repository
public interface DoctorTemplateRepository extends MongoRepository<DoctorTemplate, String> {

	List<DoctorTemplate> findByClinicId(String clinicId);
}
