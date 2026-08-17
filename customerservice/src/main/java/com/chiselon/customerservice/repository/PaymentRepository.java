package com.chiselon.customerservice.repository;




import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.customerservice.entity.Payment;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    boolean existsByTransactionId(String transactionId);
}



