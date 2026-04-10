package com.dermacare.notification_service.dto;

import lombok.*;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrescriptionDetails {

    private List<Medicines> medicines;
    
  }

