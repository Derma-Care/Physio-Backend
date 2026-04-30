package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class Diagnosis {

    private String physioDiagnosis;
    private String affectedArea;
    private String severity;
    private String stage;
    private String notes;
}