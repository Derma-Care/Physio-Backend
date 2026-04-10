package com.dermacare.doctorservice.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SymptomDetails {
    private String symptomDetails;
    private String doctorObs;
    private String diagnosis;
    private String duration;
    private List<String> attachments;
}

