package com.pharmacyManagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.pharmacyManagement.entity.PurchaseBill;

@Repository
public interface PurchaseBillRepository extends MongoRepository<PurchaseBill, String> {

	boolean existsByPurchaseBillNo(String purchaseBillNo);

	Optional<PurchaseBill> findByPurchaseBillNo(String purchaseBillNo);

	@Query("{ 'date' : { $gte: ?0, $lte: ?1 } }")
	List<PurchaseBill> findByDateRange(String fromDate, String toDate);

	List<PurchaseBill> findByClinicIdAndBranchId(String clinicId, String branchId);

	Optional<PurchaseBill> findByClinicIdAndBranchIdAndPurchaseBillNo(String clinicId, String branchId,
			String purchaseBillNo);

}
