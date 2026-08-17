package com.chiselon.notificationservice.dto;

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

