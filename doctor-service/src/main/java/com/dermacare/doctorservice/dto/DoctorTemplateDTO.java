package com.dermacare.doctorservice.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorTemplateDTO {
	
    private String title;
//    private String patientName;
    private LocalDateTime createdAt;
    private String clinicId;
    private String symptoms;
    private TestDetailsDTO tests;
    private TreatmentResponseDTO treatments;
    private FollowUpDetailsDTO followUp;
    private PrescriptionDetailsDTO prescription;
}


