package com.AdminService.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorSlotDTO {
	private String doctorId;   
	private String hospitalId ; 
	private String branchId;
	private String date;
	private String branchName;
	private List<DoctorAvailableSlotDTO> availableSlots;
	
}