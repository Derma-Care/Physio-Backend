package com.dermacare.bookingService.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TheraphyAnswersEntity{
	
	private long questionId;
	private String question;
	private String answer;

}
