package com.dermaCare.customerService.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsultationDTO {
	
	private String consultationId;
	private String consultationType;

}
