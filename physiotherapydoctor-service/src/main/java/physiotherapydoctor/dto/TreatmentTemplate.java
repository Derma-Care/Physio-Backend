package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class TreatmentTemplate {

    private String condition;
    private List<String> modalities;
    private String manualTherapy;
    private List<String> exercises;
    private String duration;
    private String frequency;
}