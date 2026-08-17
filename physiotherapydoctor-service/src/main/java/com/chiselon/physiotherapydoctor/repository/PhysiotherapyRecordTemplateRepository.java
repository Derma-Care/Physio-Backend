package com.chiselon.physiotherapydoctor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.chiselon.physiotherapydoctor.entity.PhysiotherapyRecordTemplate;

public interface PhysiotherapyRecordTemplateRepository extends MongoRepository<PhysiotherapyRecordTemplate, String> {

	Optional<PhysiotherapyRecordTemplate> findByClinicIdAndBranchIdAndBookingIdAndTemplateRecordId(String clinicId,
			String branchId, String bookingId, String templateRecordId);

	List<PhysiotherapyRecordTemplate> findByClinicIdAndBranchIdAndBookingId(String clinicId, String branchId,
			String bookingId);

	List<PhysiotherapyRecordTemplate> findByClinicId(String clinicId);

	Optional<PhysiotherapyRecordTemplate> findByClinicIdAndTemplateRecordId(String clinicId, String templateRecordId);

}