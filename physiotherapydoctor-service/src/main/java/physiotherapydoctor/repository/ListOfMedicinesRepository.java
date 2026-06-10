package physiotherapydoctor.repository;



import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import physiotherapydoctor.entity.ListOfMedicines;

public interface ListOfMedicinesRepository extends MongoRepository<ListOfMedicines, String> {
    List<ListOfMedicines> findByClinicId(String clinicId);
}
