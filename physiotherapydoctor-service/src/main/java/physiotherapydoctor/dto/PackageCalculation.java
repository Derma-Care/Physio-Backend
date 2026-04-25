package physiotherapydoctor.dto;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PackageCalculation {

	
    private String serviceType;       
    private String bookingId;
    private String therapistRecordId;
    private String clinicId;
    private String branchId;
    private String patientId;
    private String doctorId;
    private String doctorName;
    private String therapistId;
    private String therapistName;
    private String packageId;
    private String packageName;
    private int total;
    private List<ProgramDataForPackage> therapySessions;
}
