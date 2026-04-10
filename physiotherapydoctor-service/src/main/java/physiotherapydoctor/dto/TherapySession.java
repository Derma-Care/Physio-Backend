package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class TherapySession {

	private String programId;
	private String programName;
	private String clinicId;
	private String branchId;
	private int totalTherapyIds;
	private String serviceType; // Program or package;
	private List<TherapyData> therapyData;


//	private String sessionId;
//	private String sessionDate;
//	private String status;
//	private List<String> modalitiesUsed;
//	private List<String> exercisesDone;
//	private String patientResponse;
//	private String frequency;
//	private String duration;
////	private String therapistNotes;
////	private String overallStatus; // ✅ NEW
}