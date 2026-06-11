package physiotherapydoctor.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import physiotherapydoctor.entity.PhysiotherapyRecordTemplate;

public interface PhysiotherapyRecordTemplateRepository
        extends MongoRepository<PhysiotherapyRecordTemplate, String> {

}