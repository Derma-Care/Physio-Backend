package physiotherapydoctor.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PackageCalculationForTemplate {

	  private String serviceType;       
	    private String bookingId;
	    private String templateRecordId;
	    private String clinicId;
	    private String branchId;
	    private String doctorId;
	    private String doctorName;
	    private String therapistId;
	    private String therapistName;
	    private String packageId;
	    private String packageName;
	    private int total;
	    private List<ProgramDataForPackage> therapySessions;
}
