package com.dermaCare.customerService.entity;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TherophyRecordList {
	
	 private Integer setsdone;  
	    private String repitationdone;	   
	    private Integer sessioncount;///current session
	    private Integer session;///all sessions
	    private Boolean sessioncompleted;
	    private LocalDate date;
	    private String excerciseId;
	    private String notes;   	   
	    private byte[] beforeImage;
	    private byte[]  afterImage;
	    private byte[]  beforeVideo;
	    private byte[] afterVideo;

}
