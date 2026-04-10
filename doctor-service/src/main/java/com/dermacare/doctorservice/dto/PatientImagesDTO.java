package com.dermacare.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientImagesDTO {

	private String beforeImage;
	private String afterImage;
	
}
