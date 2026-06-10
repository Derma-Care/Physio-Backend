package com.clinicadmin.dto;

import java.util.List;

import lombok.Data;

@Data
public class AttendanceDTO {

    private String userId;
    private String role;

    private String clinicId;
    private String branchId;
    private String date;

    // 🔥 Now using object
    private TimeLocationDTO login;
    private TimeLocationDTO logout;

    private List<ActivityDTO> activities;

    private String loginTime;
    private String logoutTime;
    private String loginLocation;
    private String logoutLocation;
    private String loginLatitude;
    private String loginLongitude;

    private String logoutLatitude;
    private String logoutLongitude;
	
	
	


}