package physiotherapydoctor.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramAndTherophyAndExcercisesInfoForTemplate {
	
	//private String id;
	private String bookingId;
	private String doctorName;
	private String doctorId;
	private String therapistName;
	private String therapistId;
	private String templateRecordId;
	private String programId;
	private String programName;
	private Integer programCost;
	private Integer noOfSessionCount;
	private Integer noTherapyCount;
	private List<TherophyDataDto> therophyData;
	private String clinicId;
	private String branchId;
	
}

