package com.dermaCare.customerService.repository;




import org.springframework.data.mongodb.repository.MongoRepository;

import com.dermaCare.customerService.entity.Payment;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    boolean existsByTransactionId(String transactionId);
}



