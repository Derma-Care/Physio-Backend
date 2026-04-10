package com.dermaCare.customerService.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties
public class TheraphyAnswersDTO {
	
	private long questionId;
	private String question;
	private String answer;
	private List<String> options;

}
