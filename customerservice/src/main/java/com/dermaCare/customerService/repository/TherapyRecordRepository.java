package com.dermaCare.customerService.repository;


import com.dermaCare.customerService.entity.TherapyRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TherapyRecordRepository extends MongoRepository<TherapyRecord, String> {

	 // Get By Clinic + Branch + Patient
    List<TherapyRecord> findByClincinidAndBrnchidAndPatientid(
            String clincinid,
            String brnchid,
            String patientid);

    // Get By Clinic + Branch + Patient + TherapyRecordId
    Optional<TherapyRecord> 
    findByClincinidAndBrnchidAndPatientidAndTherapyrecordid(
            String clincinid,
            String brnchid,
            String patientid,
            String therapyrecordid);
    
    Optional<TherapyRecord> findByTherapyrecordidAndExcerciseId(           
            String therapyrecordid,
            String excerciseId);
    
    List<TherapyRecord> findByClincinidAndBrnchidAndTherapyrecordidAndPatientidAndExcerciseId(
            String clincinid,
            String brnchid,
            String therapyid,
            String patientid,
            String excerciseId);

	Optional<TherapyRecord> findBy(String therapyrecordid);
}  
