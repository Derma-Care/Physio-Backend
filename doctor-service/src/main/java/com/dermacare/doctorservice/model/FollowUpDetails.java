package com.dermacare.doctorservice.model;



import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpDetails {
	
    private int durationValue;
    private String durationUnit;
    private String nextFollowUpDate;
    private String followUpNote;
}

