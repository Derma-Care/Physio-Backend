package com.clinicadmin.dto;

import java.util.List;

import com.clinicadmin.entity.TherapyExercises;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheraphyProgramWithTheraphyNamesDto {
	
	private String id;
	private String programName;
	private double totalProgramAmount;
	private List<TheraphyNamesDTO> therophy;
	private String clinicId;
	private String branchId;
	private long theraphyCount;
	

}
