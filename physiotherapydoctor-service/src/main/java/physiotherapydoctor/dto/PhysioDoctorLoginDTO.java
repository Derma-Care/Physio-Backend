package physiotherapydoctor.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhysioDoctorLoginDTO {
	private String userName;
	private String password;
	private String role;
	private String deviceId;
	private String staffId;
	private String staffName;
	private String hospitalName;
	private String hospitalId;
	private String branchId;
	private String branchName;
	private Map<String, List<String>> permissions;
}
