package com.dermaCare.customerService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetailsBySubServiceIdAndConsultationTypeDTO {
	
	private String hospitlName;
	private String hospitalId;
	private String hospitalLogo;
	private String subServiceName;
	private String subServiceId;
	private String doctorId;
	private String serviceName;
	private DoctorFeeDTO doctorFee;
	private String consultationType;

}
