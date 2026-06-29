package com.AdminService.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionsEntity {

	private long questionId;
	private String question;
	private String type;
	private List<String> options;

}
