package com.chiselon.adminservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.chiselon.adminservice.entity.Branch;

@Repository
public interface BranchRepository extends MongoRepository<Branch, String>
{
	int countByClinicId(String clinicId);

	List<Branch> findByClinicId(String clinicId);

	Optional<Branch> findByBranchId(String branchId);

	void deleteByBranchId(String branchId);

	Branch findFirstByClinicId(String userName);

	Optional<Branch> findByClinicIdAndBranchId(String clinicId, String branchId);
	
}
