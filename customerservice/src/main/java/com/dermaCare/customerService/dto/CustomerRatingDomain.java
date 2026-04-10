package com.dermaCare.customerService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerRatingDomain {
	    
	private double doctorRating;
    private double branchRating;
    private String feedback;
    private String hospitalId;
    private String branchId;
    private String doctorId; 
    private String customerMobileNumber;
    private String patientId;
    private String patientName;
    private String appointmentId;
    private boolean rated;
    private String dateAndTimeAtRating;
    
    
    public boolean getRated() {
    	return rated;
    }
	   
}
