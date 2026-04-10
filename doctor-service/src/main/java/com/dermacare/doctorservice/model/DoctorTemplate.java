package com.dermacare.doctorservice.model;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "doctor_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorTemplate {
    @Id
    private String id;
    private String title;
    private LocalDateTime createdAt;
    private String clinicId;
    private String symptoms;
    private TestDetails tests;
    private TreatmentResponse treatments;
    private FollowUpDetails followUp;
    private PrescriptionDetails prescription;
}

