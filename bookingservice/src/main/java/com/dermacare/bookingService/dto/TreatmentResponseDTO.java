package com.dermacare.bookingService.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TreatmentResponseDTO {
	
    private List<String> selectedTestTreatment;
    private Map<String, TreatmentDetailsDTO> generatedData; // dynamic treatment names 
    private int totalSittings;
    private int pendingSittings;
    private int takenSittings;
    private int currentSitting;

   
}
