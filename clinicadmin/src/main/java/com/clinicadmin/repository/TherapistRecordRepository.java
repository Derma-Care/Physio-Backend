package com.clinicadmin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.clinicadmin.entity.TherapistRecord;

public interface TherapistRecordRepository extends MongoRepository<TherapistRecord, String> {

    Optional<TherapistRecord> findByClinicIdAndBranchIdAndTherapistRecordId(
            String clinicId, String branchId, String therapistRecordId);
    Optional<TherapistRecord> findByClinicIdAndBranchIdAndTherapistRecordIdAndSessionId(
            String clinicId,
            String branchId,
            String therapistRecordId,
            String sessionId
    );
  List<TherapistRecord> findAllByPatientIdAndBookingId(String patientId, String bookingId);
	
}