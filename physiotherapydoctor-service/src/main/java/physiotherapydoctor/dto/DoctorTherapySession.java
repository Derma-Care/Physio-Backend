package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class DoctorTherapySession {
	 private String serviceType;

	    // PACKAGE
	    private String packageId;
	    private String packageName;
	    private List<DoctorProgram> programs;

	    // PROGRAM
	    private String programId;
	    private String programName;
	    private List<DoctorTherapyData> therapyData;

	    // THERAPY
	    private String therapyId;
	    private String therapyName;
	    private List<DoctorTherapyExercise> exercises;

	    private Double totalPrice;
}
