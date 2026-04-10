package com.dermacare.doctorservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class Dates {
	 private String date; 
	    private Integer sitting;
	    private String status; 
	    
}

