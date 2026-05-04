package com.clinicadmin.dto;

import lombok.Data;

@Data
public class ActivityDTO {

    private String activityId;
    private String activity;
    private String duration;
    private String location;
}