package com.pharmacyManagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.pharmacyManagement.entity.Order;

public interface OrderRepository extends MongoRepository<Order, String> {

  
    List<Order> findByClinic_ClinicIdAndBranch_BranchId(String clinicId, String branchId);

   
    Optional<Order> findByClinic_ClinicIdAndBranch_BranchIdAndOrderId(
            String clinicId, String branchId, String orderId);

   
    Optional<Order> findByOrderId(String orderId);

   
    void deleteByOrderId(String orderId);

List<Order> findByClinic_ClinicIdAndBranch_BranchIdAndSupplier_SupplierId(String clinicId, String branchId,
			String supplierId);
}