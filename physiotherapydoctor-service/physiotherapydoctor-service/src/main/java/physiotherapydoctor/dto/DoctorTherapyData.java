package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;
@Data
public class DoctorTherapyData {
	   private String therapyId;
	    private String therapyName;
	    private Double totalPrice;  // ✅ RENAME
//	    private String paymentStatus;      // ✅ ADD

	    private List<DoctorTherapyExercise> exercises;
}
