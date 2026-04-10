package com.dermaCare.customerService.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Only include non-null fields in JSON response

public class ResponseDTO {

	 private String message;
	    private Boolean registrationCompleted;
	    private int status; // HTTP status code or custom status
	    private boolean success; // New field to indicate success
	    private Object formData; // New field to hold form data

	    // Completion status fields
	    private Boolean caregiverDetailsCompleted;
	    private Boolean basicProfileCompleted;
	    private Boolean qualificationDetailsCompleted;
	    private Boolean courseCertificationsCompleted;
	    private Boolean experienceDetailsCompleted;
	    private Boolean bankAccountCompleted;
	    private Boolean preferredLocationsCompleted;
	

	    private Boolean providerRatingCompleted; // Field to indicate if provider rating is completed

	    private Boolean customerRatingCompleted; // Field to indicate if customer rating is completed
}