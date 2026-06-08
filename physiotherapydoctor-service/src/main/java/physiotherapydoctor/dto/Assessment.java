package physiotherapydoctor.dto;

import java.util.Map;

import lombok.Data;

@Data
public class Assessment {

  

    // Existing sections
    private SubjectiveAssessment subjectiveAssessment;

    private FunctionalAssessment functionalAssessment;

    private PhysicalExamination physicalExamination;
    // Main discriminator field
    private String patientPain;
    // Pain/Rehab specific sections
    private Map<String, Object> acutePainPatients;

    private Map<String, Object> chronicPainPatients;

    private Map<String, Object> mechanicalPainPatients;

    private Map<String, Object> neuropathicPainPatients;

    private Map<String, Object> inflammatoryPainPatients;

    private Map<String, Object> myofascialPainPatients;

    private Map<String, Object> posturalPainPatients;

    private Map<String, Object> sportsRehabPatients;

    private Map<String, Object> neuroRehabPatients;

    private Map<String, Object> orthopedicRehabPatients;

    private Map<String, Object> pediatricRehabPatients;

    private Map<String, Object> geriatricRehabPatients;

    private Map<String, Object> cardiacRehabPatients;

    // Other assessment sections
    private RedFlags redFlags;

    private RadiationNeuro radiationNeuro;

    private Psychosocial psychosocial;

    private SpecialSymptoms specialSymptoms;
}