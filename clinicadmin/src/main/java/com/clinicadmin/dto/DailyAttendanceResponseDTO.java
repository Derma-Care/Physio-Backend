package com.clinicadmin.dto;

import lombok.Data;
import java.util.List;

@Data
public class DailyAttendanceResponseDTO {

    private String date;

    private TimeLocationDTO login;
    private TimeLocationDTO logout;

    private String logTime;
    private String status;

    private List<ActivityDTO> activities;

//	private String  description;
		
		
	
}