package com.clinicadmin.dto;

import lombok.Data;

@Data
public class DoctorAnalyticsDTO {

    private String doctorId;
    private String doctorName;
    private String speciality;

    private long totalScheduled;
    private long completed;
    private long cancelled;
    private long missed;

    private double completionRate;
}