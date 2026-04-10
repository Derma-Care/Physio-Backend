package com.dermaCare.customerService.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportsAndDoctorSaveDetailsDto {
	
	private List<DoctorSaveDetailsDTO> doctorSaveDetailsDTO;
	private List<ReportsDtoList> reportsDtoList;

}
