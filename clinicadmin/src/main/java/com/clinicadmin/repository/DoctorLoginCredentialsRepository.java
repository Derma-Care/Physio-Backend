package com.clinicadmin.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.clinicadmin.entity.DoctorLoginCredentials;

public interface DoctorLoginCredentialsRepository extends MongoRepository<DoctorLoginCredentials, String> {
    Optional<DoctorLoginCredentials> findByUsername(String username);
    boolean existsByUsername(String username);  // fixed
    Optional<DoctorLoginCredentials> findByStaffId(String staffId);
}

