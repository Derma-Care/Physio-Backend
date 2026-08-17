package com.chiselon.customerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsultationTypeDTO {
	private int serviceAndTreatments;
	private int inClinic;
	private int videoOrOnline;

}
