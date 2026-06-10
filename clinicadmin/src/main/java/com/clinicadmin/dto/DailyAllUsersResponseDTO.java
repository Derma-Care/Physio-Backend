// ============================================================================
// 2. NEW DTO: DailyAllUsersResponseDTO.java
// ============================================================================

package com.clinicadmin.dto;

import java.util.List;

import lombok.Data;

@Data
public class DailyAllUsersResponseDTO {

    // User details from DoctorLoginCredentials
    private String userId;      // staffId
    private String name;        // staffName
    private String role;        // Doctor/Admin/etc

    // Attendance details
    private String clinicId;
    private String branchId;
    private String date;
    private String status;
    private String logTime;
    private String workingHours;
    private String idleTime;

    private TimeLocationDTO login;
    private TimeLocationDTO logout;

//    private List<ActivityDTO> activities;
//
//	private String description;
//	
		
	
}