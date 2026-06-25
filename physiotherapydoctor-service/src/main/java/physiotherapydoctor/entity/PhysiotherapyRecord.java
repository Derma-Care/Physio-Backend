package physiotherapydoctor.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import physiotherapydoctor.dto.Assessment;
import physiotherapydoctor.dto.Complaints;
import physiotherapydoctor.dto.Diagnosis;
import physiotherapydoctor.dto.ExercisePlan;
import physiotherapydoctor.dto.FollowUp;
import physiotherapydoctor.dto.Investigation;
import physiotherapydoctor.dto.PatientInfo;
import physiotherapydoctor.dto.RecoverySupportDTO;
import physiotherapydoctor.dto.TherapySession;
import physiotherapydoctor.dto.TreatmentPlan;

@Document(collection = "physiotherapy_records")
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhysiotherapyRecord {

    @Id
    private String therapistRecordId;
    private String bookingId;
    private String clinicId;
    private String branchId;

//    private String overallStatus;
    private String createdAt;
    private String updatedAt;

    private PatientInfo patientInfo;
    private Complaints complaints;
    private Investigation investigation;
    private Assessment assessment;
    private Diagnosis diagnosis;
    private TreatmentPlan treatmentPlan;

    private List<TherapySession> therapySessions;
    private List<RecoverySupportDTO> recoverySupport;

    private ExercisePlan exercisePlan;
    private FollowUp followUp;
    private String prescriptionPdf;
	private String createdTime;
	
	private boolean uptoInvestigation = false;	
		
	}
    
