package physiotherapydoctor.dto;


import lombok.Data;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProgramCalculations {
	private String serviceType;       // "package" | "program" | "therapy" | "exercise"
    private String bookingId;
    private String therapistRecordId;
    private String clinicId;
    private String branchId;
    private String patientId;
    private String doctorId;
    private String doctorName;
    private int totalPrice;
    private String programId;
	 private String programName;
    private List<TheraphyInfo> therapyData;
}