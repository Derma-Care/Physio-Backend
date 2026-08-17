package com.chiselon.clinicadmin.repository;



import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.clinicadmin.dto.ReferredDoctorDTO;
import com.chiselon.clinicadmin.entity.ReferredDoctor;

import java.util.List;
import java.util.Optional;

public interface ReferredDoctorRepository extends MongoRepository<ReferredDoctor, String> {
    Optional<ReferredDoctor> findByReferralId(String referralId);
    boolean existsByReferralId(String referralId);
	boolean existsByMobileNumber(String mobileNumber);
	List<ReferredDoctorDTO> findByClinicId(String clinicId);
}