package com.chiselon.customerservice.util;

import java.util.List;

import com.chiselon.customerservice.dto.QuestionsByPartDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PysioQuestionsRes {
	
	    private String message;
	    private int status; // HTTP status code or custom status
	    private boolean success; // New field to indicate success
	    private List<QuestionsByPartDTO> questions;
	    

}
