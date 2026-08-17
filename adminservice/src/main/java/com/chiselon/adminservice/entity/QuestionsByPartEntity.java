package com.chiselon.adminservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "physiotherapy_questions")
public class QuestionsByPartEntity {

	 private Map<String, List<QuestionsEntity>> questionsByPart;
}