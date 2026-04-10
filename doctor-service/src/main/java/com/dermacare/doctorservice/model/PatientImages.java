package com.dermacare.doctorservice.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "PatientImages")
public class PatientImages {
	
	private String id;
	private String beforeImage;
	private String afterImage;
	
}
