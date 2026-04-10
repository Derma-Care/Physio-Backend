package com.pharmacyManagement.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.pharmacyManagement.entity.SalesReturn;
import com.pharmacyManagement.enums.ReturnStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesReturnRepository extends MongoRepository<SalesReturn, String> {

    Optional<SalesReturn> findByReturnNoAndStatus(String returnNo, ReturnStatus status);

    Optional<SalesReturn> findByReturnNo(String returnNo);

    boolean existsByReturnNo(String returnNo);

    boolean existsByOriginalBillNo(String originalBillNo);
    
    List<SalesReturn> findByClinicIdAndBranchId(String cid,String bid);
    
}

