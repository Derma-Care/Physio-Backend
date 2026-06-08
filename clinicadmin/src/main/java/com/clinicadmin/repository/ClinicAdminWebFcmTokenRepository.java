package com.clinicadmin.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.clinicadmin.entity.ClinicAdminDeviceTokenEntity;


public interface ClinicAdminWebFcmTokenRepository extends MongoRepository<ClinicAdminDeviceTokenEntity, String>{

	ClinicAdminDeviceTokenEntity findByUsername(String username);
}
