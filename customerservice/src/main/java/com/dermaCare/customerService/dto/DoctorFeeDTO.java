package com.dermaCare.customerService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorFeeDTO {
	
	//private int serviceAndTreatmentFee;
	private int inClinicFee;
	private int vedioConsultationFee;;
}
