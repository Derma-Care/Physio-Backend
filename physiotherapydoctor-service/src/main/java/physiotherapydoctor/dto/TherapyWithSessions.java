package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class TherapyWithSessions {
	private String doctorId;
	private String doctorName;
	private Integer noOfSessionCount;
	private Integer noTherapyCount;
	private String programId;
	private String programName;
	private String therapistId;
	private String therapistName;
	private String therapistRecordId;
    private List<TheraphyData> therophyData;

}
