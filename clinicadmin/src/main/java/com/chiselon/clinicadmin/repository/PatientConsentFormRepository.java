package com.chiselon.clinicadmin.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.clinicadmin.entity.PatientConsentForm;

public interface PatientConsentFormRepository extends MongoRepository<PatientConsentForm, String> {

}
