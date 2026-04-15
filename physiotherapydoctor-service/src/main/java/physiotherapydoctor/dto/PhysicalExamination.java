package physiotherapydoctor.dto;

import lombok.Data;
import java.util.List;

@Data
public class PhysicalExamination {

    private List<String> postureAssessment;
    private List<String> rangeOfMotion;
    private List<String> muscleStrength;
    private List<String> neurologicalSigns;
}
