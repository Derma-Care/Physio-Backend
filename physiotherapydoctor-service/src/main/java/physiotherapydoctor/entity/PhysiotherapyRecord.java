package physiotherapydoctor.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import physiotherapydoctor.dto.Assessment;
import physiotherapydoctor.dto.Complaints;
import physiotherapydoctor.dto.Diagnosis;
import physiotherapydoctor.dto.ExercisePlan;
import physiotherapydoctor.dto.FollowUp;
import physiotherapydoctor.dto.PatientInfo;
import physiotherapydoctor.dto.ProgressAnalytics;
import physiotherapydoctor.dto.ProgressNotes;
import physiotherapydoctor.dto.TherapySession;
import physiotherapydoctor.dto.TreatmentPlan;
import physiotherapydoctor.dto.TreatmentTemplate;

@Document(collection = "physiotherapy_records")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhysiotherapyRecord {

    @Id
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
//    private ProgressNotes progressNotes;
    private FollowUp followUp;
//    private ProgressAnalytics progressAnalytics;
    private List<TreatmentTemplate> treatmentTemplates;
    private String createdAt;
    private String updatedAt;
    
}