package com.chiselon.clinicadmin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.clinicadmin.entity.LoginEntity;

public interface LoginRepository extends MongoRepository<LoginEntity, String> {
    Optional<LoginEntity> findByUsername(String username);
    boolean existsByUsername(String username);  // fixed
    Optional<LoginEntity> findByStaffId(String staffId);
    List<LoginEntity> findByHospitalIdAndBranchId(
            String hospitalId,
            String branchId
    );
	void deleteByStaffId(String therapistId);
    
}
