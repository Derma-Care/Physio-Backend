package com.pharmacyManagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.pharmacyManagement.entity.Supplier;

public interface SupplierRepository extends MongoRepository<Supplier, String> {

	
	boolean existsBySupplierNameIgnoreCase(String supplierName);

	boolean existsByContactDetailsMobileNumber(String mobileNumber);

	boolean existsByContactDetailsEmail(String email);

	Optional<Supplier> findByUserName(String userName);

	List<Supplier> findByClinicIdAndBranchId(String clinicId, String branchId);

}
