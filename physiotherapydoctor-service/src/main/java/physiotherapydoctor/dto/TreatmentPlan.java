package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class TreatmentPlan {

	private String doctorId;
	private String doctorName;

	private String therapistId;
	private String therapistName;

	private String manualTherapy;
	private String precautions;
//	private String frequency;
//	private List<String> modalities;
//	private String sessionDuration;
//	private String totalSessions;
}
