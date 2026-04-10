package com.dermacare.notification_service.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerOnbordingDTO {
    
    private String mobileNumber;
    private String fullName;    
    private String dateOfBirth;  
    private String branchId;
    private String customerId;
    private String patientId;
    private String deviceId;
   
}
