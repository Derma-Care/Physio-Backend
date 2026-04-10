package com.dermacare.doctorservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ListOfMedicinesDTO {
    private String id;
    
    private String clinicId;
    
    private List<String> listOfMedicines;
}