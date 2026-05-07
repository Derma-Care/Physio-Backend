package com.clinicadmin.repository;

import com.clinicadmin.entity.PatientFeedback;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface PatientFeedbackRepository extends MongoRepository<PatientFeedback, String> {

}