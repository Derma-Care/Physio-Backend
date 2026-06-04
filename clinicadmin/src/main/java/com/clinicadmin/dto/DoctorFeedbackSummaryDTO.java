package com.clinicadmin.dto;

import java.util.List;

import lombok.Data;

@Data
public class DoctorFeedbackSummaryDTO {

    private String doctorId;
    private String doctorName;
    private String clinicId;

    private long totalPatientsRated;
    private double averageRating;

    private List<PatientRatingDTO> patients;



}