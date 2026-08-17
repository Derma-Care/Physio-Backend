package com.chiselon.clinicadmin.dto;

import lombok.Data;

@Data
public class TopReferringDoctorDTO {

    private String fullName;

    private Long patientsReferred;
}