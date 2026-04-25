package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class TherapySession {	

	    private String serviceType;

	    // PACKAGE
	    private String packageId;
	    private String packageName;
	    private List<Program> programs;

	    // PROGRAM
	    private String programId;
	    private String programName;
	    private List<TherapyData> therapyData;

	    // THERAPY
	    private String therapyId;
	    private String therapyName;
	    private List<TherapyExercise> exercises;

	    private Double totalPrice;

		public String getSessionId() {
			// TODO Auto-generated method stub
			return null;
		}
	
}