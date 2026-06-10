package com.dermaCare.customerService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSessionsWithRecords {
	
	 private String bookingId;
	    private String branchId;
	    private String clinicId;
	    private String patientId;
	    private String therapistId;
	    private String therapistRecordId;

}
