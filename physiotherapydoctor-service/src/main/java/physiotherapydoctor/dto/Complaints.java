package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class Complaints {

    private String complaintDetails;
    private String painAssessmentImage;
    private List<String> reportImages;

    private String selectedTherapy;
    private String selectedTherapyId;

    private String duration;

    private String previousInjuries;
    private String currentMedications;
    private String allergies;
    private String occupation;
    private List<String> activityLevels;
    private String patientPain;

    private List<TherapyAnswer> therapyAnswers;
}
