package com.dermacare.doctorservice.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DoctorResponseDTO {
    private String doctorId;
    private String doctorMobileNumber;
    private String hospitalId; 
}


