package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class PhysicalExamination {

    private List<String> postureAssessment;
    private List<String> rangeOfMotion;
    private List<String> muscleStrength;
    private List<String> neurologicalSigns;
}
