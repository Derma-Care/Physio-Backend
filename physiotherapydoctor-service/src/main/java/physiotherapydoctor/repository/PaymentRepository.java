package physiotherapydoctor.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import physiotherapydoctor.entity.PaymentRecord;

@Repository
public interface PaymentRepository extends MongoRepository<PaymentRecord, String>{

}
