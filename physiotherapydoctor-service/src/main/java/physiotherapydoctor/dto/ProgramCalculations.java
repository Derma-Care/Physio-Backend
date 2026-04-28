package physiotherapydoctor.dto;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

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
    private String therapistId;
    private String therapistName;
    private int totalPrice;
    private String programId;
	 private String programName;
    private List<TheraphyInfo> therapyData;
}