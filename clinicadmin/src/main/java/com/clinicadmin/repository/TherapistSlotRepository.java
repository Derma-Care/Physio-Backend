package com.clinicadmin.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.clinicadmin.entity.TherapistSlot;

public interface TherapistSlotRepository extends MongoRepository<TherapistSlot, String> {

    TherapistSlot findByTherapistIdAndBranchIdAndDate(String therapistId, String branchId, String date);

    List<TherapistSlot> findAllByTherapistIdAndDate(String therapistId, String date);

    List<TherapistSlot> findByClinicIdAndBranchIdAndTherapistId(String clinicId, String branchId, String therapistId);

    List<TherapistSlot> findByClinicIdAndTherapistId(String clinicId, String therapistId);
}