package com.dermaCare.customerService.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionsByPartDTO {
	
	private Map<String, List<QuestionsDTO>> questionsByPart;

}
