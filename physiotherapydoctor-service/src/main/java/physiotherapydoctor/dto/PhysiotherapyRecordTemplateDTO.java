package physiotherapydoctor.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhysiotherapyRecordTemplateDTO {

	private String templateRecordId;
	private String bookingId;
	private String clinicId;
	private String branchId;

	private String createdAt;
	private String updatedAt;

	private Investigation investigation;

	private Diagnosis diagnosis;
	private TreatmentPlan treatmentPlan;

	private List<TherapySession> therapySessions;
	private List<RecoverySupportDTO> recoverySupport;

	private ExercisePlan exercisePlan;
	private FollowUp followUp;
	private String createdTime;

}
