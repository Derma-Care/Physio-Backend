package com.chiselon.customerservice.service;

import org.springframework.http.ResponseEntity;

import com.chiselon.customerservice.dto.MutiplePartsDto;
import com.chiselon.customerservice.dto.QuestionsByPartDTO;
import com.chiselon.customerservice.dto.QuestionsDTO;
import com.chiselon.customerservice.util.PysioQuestionsRes;
import com.chiselon.customerservice.util.Response;

public interface PhysiotherapyService {
	
	ResponseEntity<Response> create(QuestionsByPartDTO dto);

	public ResponseEntity<PysioQuestionsRes> getAll();
	    
    public ResponseEntity<Response> deleteQuestionByKeyAndId(String key, long questionId);
	            
    public ResponseEntity<Response> updateByKey(String key, QuestionsDTO dto);
	    
    public ResponseEntity<Response> getByKeys(MutiplePartsDto keys);
    
    public ResponseEntity<Response> getExerciseSessionsWithRecords(String clinicId,
			String branchId,  String bookingId,  String patientId,String therapistId,
			String therapistRecordId);
	             	   
}