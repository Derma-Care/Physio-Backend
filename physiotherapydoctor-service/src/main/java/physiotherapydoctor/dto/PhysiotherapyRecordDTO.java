package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class PhysiotherapyRecordDTO {
	 private String therapistRecordId;
	    private String bookingId;
	    private String clinicId;
	    private String branchId;
	    private String overallStatus;
	    private PatientInfo patientInfo;  
	    private Complaints complaints;     
	    private Assessment assessment;
	    private Diagnosis diagnosis;
	    private TreatmentPlan treatmentPlan;
	    private List<TherapySession> therapySessions;
	    private ExercisePlan exercisePlan;
//	    private ProgressNotes progressNotes;
	    private FollowUp followUp;
//	    private ProgressAnalytics progressAnalytics;
	    private List<TreatmentTemplate> treatmentTemplates;
	    private String createdAt;
	    private String updatedAt;
}
