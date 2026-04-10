package com.dermaCare.customerService.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicAndDoctorCombineData {
	
	private DoctorsDTO doctorsDTO;
	private ClinicDTO clinicDTO;

}
