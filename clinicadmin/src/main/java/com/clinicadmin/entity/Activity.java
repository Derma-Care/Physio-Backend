package com.clinicadmin.entity;

import lombok.Data;

@Data
public class Activity {

    private String activityId;
    private String activity;
    private String duration;
    private String latitude;
    private String longtitude;
    private String location;
    private String description;

}
