package com.chiselon.clinicadmin.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.clinicadmin.entity.ClinicAdminDeviceTokenEntity;


public interface ClinicAdminWebFcmTokenRepository extends MongoRepository<ClinicAdminDeviceTokenEntity, String>{

	Optional<ClinicAdminDeviceTokenEntity> findByUsername(String username);
}
