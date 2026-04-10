package physiotherapydoctor.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class Complaints {
	private String complaintDetails;
	private String painAssessmentImage;
	private List<String> reportImages;
	private String selectedTherapy;
	private String selectedTherapyID;
	private Map<String, List<TherapyAnswer>> theraphyAnswers;
	private String duration;
}
