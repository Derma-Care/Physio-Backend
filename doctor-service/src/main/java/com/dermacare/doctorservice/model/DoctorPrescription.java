package com.dermacare.doctorservice.model;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "DoctorPrescription")
public class DoctorPrescription {
    @Id
    private String id; 
    private LocalDateTime visitDateTime;

    private String clinicId;
    
    private List<Medicine> medicines;  
}
