package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class ExerciseCalculations {
	
	 private String serviceType;       // "package" | "program" | "therapy" | "exercise"
	    private String bookingId;
	    private String therapistRecordId;
	    private String clinicId;
	    private String branchId;
	    private String patientId;
	    private String doctorId;
	    private String doctorName;
	    private int totalPrice;
	    private List<Exercise> exercises;
	
   
}
