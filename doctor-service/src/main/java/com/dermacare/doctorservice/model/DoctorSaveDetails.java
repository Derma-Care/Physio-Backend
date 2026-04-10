package com.dermacare.doctorservice.model;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.dermacare.doctorservice.dto.TreatmentResponseDTO;
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
@Document(collection = "doctor_save_details")
public class DoctorSaveDetails {

    @Id
    private String id;

    private String patientId;
    private String doctorId;
    private String doctorName;
    private String clinicId;
    private String clinicName;
    private String bookingId;
    private String subServiceId;
    private String customerId;
    private LocalDateTime visitDateTime;
    private String visitType; 
    private int visitCount; 
    private SymptomDetails symptoms;
    private TestDetails tests;
    private TreatmentResponse treatments;
    private FollowUpDetails followUp;
    private PrescriptionDetails prescription;
    private List<String> prescriptionPdf;
	private LocalDateTime consultationStartDate;
    private LocalDateTime consultationExpiryDate;
    
}
