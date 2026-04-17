package com.clinicadmin.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.clinicadmin.entity.TherapistRecord;

public interface TherapistRecordRepository extends MongoRepository<TherapistRecord, String> {

    Optional<TherapistRecord> findByClinicIdAndBranchIdAndTherapistRecordId(
            String clinicId, String branchId, String therapistRecordId);
    Optional<TherapistRecord> findByClinicIdAndBranchIdAndTherapistRecordIdAndBookingId(
            String clinicId,
            String branchId,
            String therapistRecordId,
            String bookingId
    );

	
}