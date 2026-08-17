package com.chiselon.clinicadmin.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.clinicadmin.dto.PrivacyPolicyDTO;
import com.chiselon.clinicadmin.entity.PrivacyPolicy;

public interface  PrivacyPolicyRepository extends MongoRepository<PrivacyPolicy,String>{

	List<PrivacyPolicyDTO> findByClinicId(String clinicId);

}