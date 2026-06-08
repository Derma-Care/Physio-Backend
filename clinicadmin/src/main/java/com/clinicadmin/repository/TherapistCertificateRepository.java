package com.clinicadmin.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.clinicadmin.entity.TherapistCertificate;

@Repository
public interface TherapistCertificateRepository
        extends MongoRepository<TherapistCertificate, String> {

    List<TherapistCertificate>findByClinicIdAndBranchId(String clinicId,String branchId);
}