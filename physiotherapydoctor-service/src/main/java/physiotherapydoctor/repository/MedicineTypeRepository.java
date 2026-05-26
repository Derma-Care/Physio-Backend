package physiotherapydoctor.repository;




import org.springframework.data.mongodb.repository.MongoRepository;

import physiotherapydoctor.entity.MedicineType;

public interface MedicineTypeRepository extends MongoRepository<MedicineType, String> {

}

