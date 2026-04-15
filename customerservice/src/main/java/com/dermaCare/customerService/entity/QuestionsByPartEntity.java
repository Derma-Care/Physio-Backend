package com.dermaCare.customerService.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "physiotherapy_questions")
public class QuestionsByPartEntity {

	 @Id
	 private String id;

	 private Map<String, List<QuestionsEntity>> questionsByPart;
}
