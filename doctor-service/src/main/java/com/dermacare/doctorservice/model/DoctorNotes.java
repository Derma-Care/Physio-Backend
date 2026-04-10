package com.dermacare.doctorservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "doctor_notes")
public class DoctorNotes {
    @Id
    private String id;
    
    private String bookingId;
    private String doctorId;
    private String patientPhoneNumber;
    private String notes;
}
