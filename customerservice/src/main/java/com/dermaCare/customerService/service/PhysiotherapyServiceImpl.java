package com.dermaCare.customerService.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.dermaCare.customerService.dto.MutiplePartsDto;
import com.dermaCare.customerService.dto.QuestionsByPartDTO;
import com.dermaCare.customerService.dto.QuestionsDTO;
import com.dermaCare.customerService.entity.QuestionsByPartEntity;
import com.dermaCare.customerService.entity.QuestionsEntity;
import com.dermaCare.customerService.repository.PhysiotherapyRepo;
import com.dermaCare.customerService.util.GetByKey;
import com.dermaCare.customerService.util.PysioQuestionsRes;
import com.dermaCare.customerService.util.Response;
import com.dermaCare.customerService.util.SequenceGeneratorService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PhysiotherapyServiceImpl implements PhysiotherapyService {
	
	    @Autowired
	    private PhysiotherapyRepo repository;
	    
	    @Autowired
	    private GetByKey getByKey;

	    @Autowired
	    private SequenceGeneratorService sequenceGenerator;
	    
	    
	    private QuestionsEntity mapToEntity(QuestionsDTO d) {
	        return new QuestionsEntity(
	                d.getQuestionId(),
	                d.getQuestion(),
	                d.getType(),
	                d.getOptions()
	        );
	    }

	    private QuestionsDTO mapToDTO(QuestionsEntity e) {
	        return new QuestionsDTO(
	                e.getQuestionId(),
	                e.getQuestion(),
	                e.getType(),
	                e.getOptions()
	        );
	    }

	    // ✅ CREATE
	    @Override
	    public ResponseEntity<Response> create(QuestionsByPartDTO dto) {
	        try {

	            Map<String, List<QuestionsEntity>> entityMap = new HashMap<>();

	            dto.getQuestionsByPart().forEach((key, value) -> {

	                List<QuestionsEntity> list = value.stream().map(q -> {
	                    long seq = sequenceGenerator.generateSequence("question_sequence");
	                    q.setQuestionId(seq);
	                    return mapToEntity(q);
	                }).collect(Collectors.toList());

	                entityMap.put(key, list);
	            });

	            QuestionsByPartEntity entity = new QuestionsByPartEntity(entityMap);

	            return new ResponseEntity<>(
	                    new Response("Created successfully", 201, true, repository.save(entity)),
	                    HttpStatus.CREATED
	            );

	        } catch (Exception e) {
	            return new ResponseEntity<>(
	                    new Response("Error: " + e.getMessage(), 500, false, null),
	                    HttpStatus.INTERNAL_SERVER_ERROR
	            );
	        }
	    }

	    // ✅ GET ALL
	    @Override
	    public ResponseEntity<PysioQuestionsRes> getAll() {
	        try {
	            return ResponseEntity.ok(
	                    new PysioQuestionsRes("Fetched", 200, true,new ObjectMapper().convertValue(repository.findAll(), new TypeReference<List<QuestionsByPartDTO>>() {
						}))
	            );
	        } catch (Exception e) {
	            return new ResponseEntity<>(
	                    new PysioQuestionsRes(e.getMessage(), 500, false, null),
	                    HttpStatus.INTERNAL_SERVER_ERROR
	            );
	        }
	    }

	    
	    @Override
	    public ResponseEntity<Response> getByKeys(MutiplePartsDto keys) {
	        try {	       
	        	Map<String, List<QuestionsEntity>> filteredMap = new HashMap<>();
	            for (String key : keys.getKeys()) {
	            QuestionsByPartEntity entity = getByKey.getByKey(key);
	            if(entity != null) {
		        Map<String, List<QuestionsEntity>> existingMap = entity.getQuestionsByPart();
		        if(existingMap != null || !existingMap.isEmpty()) {
	                if (existingMap.containsKey(key)) {
	                    filteredMap.put(key, existingMap.get(key));
	                }}}}
	            if (filteredMap.isEmpty()) {
	                return new ResponseEntity<>(
	                        new Response("No matching keys found", 404, false, null),
	                        HttpStatus.NOT_FOUND
	                );
	            }

	            return ResponseEntity.ok(
	                    new Response("Fetched successfully", 200, true, filteredMap)
	            );

	        } catch (Exception e) {
	            return new ResponseEntity<>(
	                    new Response("Error fetching data: " + e.getMessage(), 500, false, null),
	                    HttpStatus.INTERNAL_SERVER_ERROR
	            );
	        }
	    }  
	    
	    	    
	    // ✅ UPDATE (replace full map)
	    
	    @Override
	    public ResponseEntity<Response> updateByKey(String key, QuestionsDTO dto) {
	        try {
	        	boolean exist = false;
	        	QuestionsByPartEntity entity = getByKey.getByKey(key);
			    Map<String, List<QuestionsEntity>> existingMap = entity.getQuestionsByPart();		           
	           System.out.println(existingMap);
	            List<QuestionsEntity> incomingQuestions = existingMap.get(key);
	            System.out.println(incomingQuestions);
                if(incomingQuestions != null || !incomingQuestions.isEmpty()) {
	            for(QuestionsEntity q : incomingQuestions) {
	                if (dto.getQuestionId() != 0 && q.getQuestionId() == dto.getQuestionId()) {
	                    q.setQuestion(dto.getQuestion());
	                    System.out.println("id");
	                    exist = true; 
	                    break;}}
	            if(!exist) {
	            long seq = sequenceGenerator.generateSequence("question_sequence");
	            dto.setQuestionId(seq);  
	            incomingQuestions.add(mapToEntity(dto));
	            }}
	            // Replace only that key
	            existingMap.put(key,incomingQuestions );
               System.out.println("key");
	            entity.setQuestionsByPart(existingMap);
	            System.out.println("hmm");
	            return ResponseEntity.ok(
	                    new Response("Updated key: " + key, 200, true, repository.save(entity))
	            );
	        }catch (Exception e) {
	            return new ResponseEntity<>(
	                    new Response("Error updating key: " + e.getMessage(), 500, false, null),
	                    HttpStatus.INTERNAL_SERVER_ERROR
	            );
	        }
	    }
	   
	    // ✅ DELETE
	    @Override
	    public ResponseEntity<Response> deleteQuestionByKeyAndId(String key, long questionId) {
	        try {
	            QuestionsByPartEntity entity = getByKey.getByKey(key);

	            Map<String, List<QuestionsEntity>> existingMap = entity.getQuestionsByPart();

	            // ✅ Check key exists
	            if (!existingMap.containsKey(key)) {
	                return new ResponseEntity<>(
	                        new Response("Key not found: " + key, 404, false, null),
	                        HttpStatus.NOT_FOUND
	                );
	            }

	            List<QuestionsEntity> questionsList = existingMap.get(key);

	            // ✅ Remove question based on questionId
	            boolean removed = questionsList.removeIf(q -> q.getQuestionId() == questionId);

	            if (!removed) {
	                return new ResponseEntity<>(
	                        new Response("QuestionId not found: " + questionId, 404, false, null),
	                        HttpStatus.NOT_FOUND
	                );
	            }

	            // ✅ Optional: remove key if list becomes empty
	            if (questionsList.isEmpty()) {
	                existingMap.remove(key);
	            } else {
	                existingMap.put(key, questionsList);
	            }

	            entity.setQuestionsByPart(existingMap);
	            repository.save(entity);

	            return ResponseEntity.ok(
	                    new Response("Deleted questionId: " + questionId + " from key: " + key, 200, true, null)
	            );

	        } catch (Exception e) {
	            return new ResponseEntity<>(
	                    new Response("Error deleting question: " + e.getMessage(), 500, false, null),
	                    HttpStatus.INTERNAL_SERVER_ERROR
	            );
	        }
	    }}
