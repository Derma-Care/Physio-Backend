package physiotherapydoctor.dto;

import java.util.Map;

import lombok.Data;

@Data
public class Assessment {

	private SubjectiveAssessment subjectiveAssessment;

	private FunctionalAssessment functionalAssessment;

	private PhysicalExamination physicalExamination;

	// optional sections
	private Map<String, Object> chronicPainPatients;
	private Map<String, Object> sportsRehabPatients;
	private Map<String, Object> neuroRehabPatients;

	private RedFlags redFlags;
	private RadiationNeuro radiationNeuro;
	private Psychosocial psychosocial;
	private SpecialSymptoms specialSymptoms;
}