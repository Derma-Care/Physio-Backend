package com.chiselon.adminservice.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.adminservice.entity.BranchCredentials;

public interface BranchCredentialsRepository extends MongoRepository<BranchCredentials, String> {
    BranchCredentials findByUserName(String userName);

	BranchCredentials findByUserNameAndPassword(String userName, String password);

	List<BranchCredentials> findByBranchId(String branchId);

	void deleteByBranchId(String branchId);

}
