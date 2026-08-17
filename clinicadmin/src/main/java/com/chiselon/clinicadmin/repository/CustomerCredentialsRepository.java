package com.chiselon.clinicadmin.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.clinicadmin.entity.CustomerCredentials;

public interface CustomerCredentialsRepository extends MongoRepository<CustomerCredentials, String> {
    Optional<CustomerCredentials> findByUserName(String userName);

	void deleteByUserName(String customerId);
}
