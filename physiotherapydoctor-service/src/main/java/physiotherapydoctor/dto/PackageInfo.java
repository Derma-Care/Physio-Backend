package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class PackageInfo {
	    private String packageId;
	    private String packageName;
	    private String serviceType;
	    private Double totalPrice;
	    private List<ProgramDataForPackage> programs;
}
