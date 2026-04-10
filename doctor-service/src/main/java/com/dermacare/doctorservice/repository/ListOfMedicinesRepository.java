package com.dermacare.doctorservice.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import com.dermacare.doctorservice.model.ListOfMedicines;
import java.util.List;

public interface ListOfMedicinesRepository extends MongoRepository<ListOfMedicines, String> {
    List<ListOfMedicines> findByClinicId(String clinicId);
}