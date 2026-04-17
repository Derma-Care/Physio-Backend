package physiotherapydoctor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import physiotherapydoctor.entity.PhysiotherapyRecord;

@Repository
public interface PhysiotherapydoctorRespository extends MongoRepository<PhysiotherapyRecord, String> {
//	List<PhysiotherapyRecord> findByTreatmentPlanTheraphyId(String theraphyId);
//	List<PhysiotherapyRecord> findByClinicIdAndBranchIdAndTreatmentPlanTheraphyId(
//	        String clinicId,
//	        String branchId,
//	        String therapistId
//	);


	Optional<PhysiotherapyRecord> findByTherapistRecordId(String therapistRecordId);
	
	   Optional<PhysiotherapyRecord> 
	    findByClinicIdAndBranchIdAndPatientInfoPatientIdAndBookingIdAndTherapistRecordId(
	        String clinicId,
	        String branchId,
	        String patientId,
	        String bookingId,
	        String therapistRecordId
	    );

	List<PhysiotherapyRecord> findByClinicIdAndBranchIdAndPatientInfoPatientIdAndBookingId(String clinicId,
			String branchId, String patientId, String bookingId);

	List<PhysiotherapyRecord> findByClinicIdAndBranchIdAndTreatmentPlan_TherapistId(String clinicId, String branchId,
			String therapistId);
	List<PhysiotherapyRecord> findByClinicIdAndBranchIdAndTreatmentPlanTherapistId(
	        String clinicId,
	        String branchId,
	        String therapistId
	);
}
