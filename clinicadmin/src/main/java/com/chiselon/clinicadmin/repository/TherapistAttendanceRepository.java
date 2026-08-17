package com.chiselon.clinicadmin.repository;

import java.util.List;
import com.chiselon.clinicadmin.entity.TherapistAttendance;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TherapistAttendanceRepository
extends MongoRepository<TherapistAttendance, String> {

TherapistAttendance findByTherapistIdAndDate(String therapistId, String date);

List<TherapistAttendance> findByTherapistIdAndDateStartingWith(String therapistId, String month);

List<TherapistAttendance> findByTherapistId(String therapistId);

List<TherapistAttendance> findByClinicIdAndBranchIdAndTherapistId(String clinicId, String branchId, String therapistId);

void deleteByTherapistId(String therapistId);

TherapistAttendance findByClinicIdAndBranchIdAndTherapistIdAndDate(String clinicId, String branchId, String therapistId,
		String date);


}