package com.dermacare.notification_service.dto;


import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DoctorsDTO {

    private String id;
    
    private String doctorId;

   
    private String hospitalId;


    private String doctorPicture;

  
    private String doctorLicence;

  
    private String doctorMobileNumber;

    private String doctorName;

   
    private String specialization;

   
    private String gender;

   
    private String experience;

  
    private String qualification;

    
    private String availableDays;

  
    private String availableTimes;

    
    private String profileDescription;

    
    private List< String> focusAreas;

    
    private List< String> languages;

  
    private List<String> careerPath;

   
    private List<String> highlights;
   
	private boolean doctorAvailabilityStatus;
	
	private boolean recommendation;
	
	
    public void trimAllDoctorFields() {
        id = trim(id);
        doctorId = trim(doctorId);
        hospitalId = trim(hospitalId);
        doctorPicture = trim(doctorPicture);
        doctorLicence = trim(doctorLicence);
        doctorMobileNumber = trim(doctorMobileNumber);
        doctorName = trim(doctorName);
        specialization = trim(specialization);
        gender = trim(gender);
        experience = trim(experience);
        qualification = trim(qualification);
        profileDescription = trim(profileDescription);
        availableDays = trim(availableDays);
        availableTimes = trim(availableTimes);

        focusAreas = trimList(focusAreas);
        languages = trimList(languages);
        careerPath = trimList(careerPath);
        highlights = trimList(highlights);
    }

    private String trim(String value) {
        return (value != null) ? value.trim() : null;
    }

    private List<String> trimList(List<String> list) {
        return (list != null)
            ? list.stream().map(this::trim).collect(Collectors.toList())
            : null;
    }
}