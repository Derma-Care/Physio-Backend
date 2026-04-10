package com.dermacare.doctorservice.repository;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dermacare.doctorservice.model.Doctor;

public interface DoctorRepository extends MongoRepository<Doctor, String> {
    Optional<Doctor> findByDoctorMobileNumber(String mobileNumber);
    
}
