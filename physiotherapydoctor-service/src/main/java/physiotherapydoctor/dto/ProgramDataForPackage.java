package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class ProgramDataForPackage {
	
	private String programId;
    private String programName;
    private Double totalPrice;
    private List<TherapyinfoForPackage> therapyData;

}
