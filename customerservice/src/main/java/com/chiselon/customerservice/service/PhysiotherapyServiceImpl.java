package com.chiselon.customerservice.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.chiselon.customerservice.dto.MutiplePartsDto;
import com.chiselon.customerservice.dto.QuestionsByPartDTO;
import com.chiselon.customerservice.dto.QuestionsDTO;
import com.chiselon.customerservice.entity.QuestionsByPartEntity;
import com.chiselon.customerservice.entity.QuestionsEntity;
import com.chiselon.customerservice.feignClient.PhysioFeign;
import com.chiselon.customerservice.repository.PhysiotherapyRepo;
import com.chiselon.customerservice.util.ExtractFeignMessage;
import com.chiselon.customerservice.util.GetByKey;
import com.chiselon.customerservice.util.KeyCloakTokenStore;
import com.chiselon.customerservice.util.Response;
import com.chiselon.customerservice.util.SequenceGeneratorService;
import com.chiselon.customerservice.util.PysioQuestionsRes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class PhysiotherapyServiceImpl implements PhysiotherapyService {
	
	    @Autowired
	    private PhysiotherapyRepo repository;
	    
	    @Autowired
	    private GetByKey getByKey;
	    
	    @Autowired
	    private PhysioFeign physioFeign;
	  
	    @Autowired
	    private SequenceGeneratorService sequenceGenerator;
	    
	    @Autowired
	    private KeyCloakTokenStore keyCloakTokenStore;
	   
	    
	    
	    private QuestionsEntity mapToEntity(QuestionsDTO d) {
        log.debug("Mapping QuestionsDTO to Entity questionId={}", d.getQuestionId());
	        return new QuestionsEntity(
	                d.getQuestionId(),
	                d.getQuestion(),
	                d.getType(),
	                d.getOptions()
	        );
	    }

	    private QuestionsDTO mapToDTO(QuestionsEntity e) {
        log.debug("Mapping Entity to DTO questionId={}", e.getQuestionId());
	        return new QuestionsDTO(
	                e.getQuestionId(),
	                e.getQuestion(),
	                e.getType(),
	                e.getOptions()
	        );
	    }

	    // ✅ CREATE
	    @Override
	    @Secured("ROLE_CUSTOMER")
	    @RateLimiter(name = "physiotherapyService", fallbackMethod = "createFallback")
    public ResponseEntity<Response> create(QuestionsByPartDTO dto) {
        log.info("Entering create partsCount={}", dto.getQuestionsByPart()!=null?dto.getQuestionsByPart().size():0);
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
            log.error("Operation failed", e);
	            return new ResponseEntity<>(
	                    new Response("Error: " + e.getMessage(), 500, false, null),
	                    HttpStatus.INTERNAL_SERVER_ERROR
	            );
	        }
	    }

	    // ✅ GET ALL
	    @Override
	    @Secured("ROLE_CUSTOMER")
	    @RateLimiter(name = "physiotherapyService", fallbackMethod = "getAllFallback")
    public ResponseEntity<PysioQuestionsRes> getAll() {
        log.info("Entering getAll");
	        try {
	            return ResponseEntity.ok(
	                    new PysioQuestionsRes("Fetched", 200, true,new ObjectMapper().convertValue(repository.findAll(), new TypeReference<List<QuestionsByPartDTO>>() {
						}))
	            );
	        } catch (Exception e) {
            log.error("Operation failed", e);
	            return new ResponseEntity<>(
	                    new PysioQuestionsRes(e.getMessage(), 500, false, null),
	                    HttpStatus.INTERNAL_SERVER_ERROR
	            );
	        }
	    }

	    
	    @Override
	    @Secured("ROLE_CUSTOMER")
	    @RateLimiter(name = "physiotherapyService", fallbackMethod = "getByKeysFallback")
    public ResponseEntity<Response> getByKeys(MutiplePartsDto keys) {
        log.info("Entering getByKeys keysCount={}", keys.getKeys()!=null?keys.getKeys().size():0);
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
            log.error("Operation failed", e);
	            return new ResponseEntity<>(
	                    new Response("Error fetching data: " + e.getMessage(), 500, false, null),
	                    HttpStatus.INTERNAL_SERVER_ERROR
	            );
	        }
	    }  
	    
	    	    
	    // ✅ UPDATE (replace full map)
	    
	    @Override
	    @Secured("ROLE_CUSTOMER")
	    @RateLimiter(name = "physiotherapyService", fallbackMethod = "updateByKeyFallback")
    public ResponseEntity<Response> updateByKey(String key, QuestionsDTO dto) {
        log.info("Entering updateByKey key={} questionId={}", key, dto.getQuestionId());
	        try {
	        	boolean exist = false;
	        	QuestionsByPartEntity entity = getByKey.getByKey(key);
			    Map<String, List<QuestionsEntity>> existingMap = entity.getQuestionsByPart();		           
	           log.debug("Existing map size={}", existingMap.size());
	            List<QuestionsEntity> incomingQuestions = existingMap.get(key);
	            log.debug("Incoming questions count={}", incomingQuestions!=null?incomingQuestions.size():0);
                if(incomingQuestions != null || !incomingQuestions.isEmpty()) {
	            for(QuestionsEntity q : incomingQuestions) {
	                if (dto.getQuestionId() != 0 && q.getQuestionId() == dto.getQuestionId()) {
	                    q.setQuestion(dto.getQuestion());
	                 //   System.out.println("id");
	                    exist = true; 
	                    break;}}
	            if(!exist) {
	            long seq = sequenceGenerator.generateSequence("question_sequence");
	            dto.setQuestionId(seq);  
	            incomingQuestions.add(mapToEntity(dto));
	            }}
	            // Replace only that key
	            existingMap.put(key,incomingQuestions );
              /// System.out.println("key");
	            entity.setQuestionsByPart(existingMap);
	          ////  System.out.println("hmm");
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
	    @Secured("ROLE_CUSTOMER")
	    @RateLimiter(name = "physiotherapyService", fallbackMethod = "deleteQuestionByKeyAndIdFallback")
    public ResponseEntity<Response> deleteQuestionByKeyAndId(String key, long questionId) {
        log.info("Entering deleteQuestionByKeyAndId key={} questionId={}", key, questionId);
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
            log.error("Operation failed", e);
	            return new ResponseEntity<>(
	                    new Response("Error deleting question: " + e.getMessage(), 500, false, null),
	                    HttpStatus.INTERNAL_SERVER_ERROR
	            );
	        }
	    }
	    
	    @Secured("ROLE_CUSTOMER")
	    @RateLimiter(name = "physiotherapyService", fallbackMethod = "getExerciseSessionsWithRecordsFallback")
    public ResponseEntity<Response> getExerciseSessionsWithRecords(String clinicId,
				String branchId,  String bookingId,  String patientId, String therapistId,
				String therapistRecordId) {
	        Response response = new Response();
	        try {
	        	return physioFeign.getExerciseSessionsWithRecords(keyCloakTokenStore.getAccess_token(),clinicId, branchId, bookingId, patientId,therapistId, therapistRecordId);

	        } catch (FeignException e) {      
	            response.setStatus(e.status());
	            response.setMessage(ExtractFeignMessage.clearMessage(e));
	            response.setSuccess(false);
	        } return ResponseEntity.status(response.getStatus()).body(response);}




    // ================= RATE LIMIT FALLBACKS =================

    public ResponseEntity<Response> createFallback(QuestionsByPartDTO dto, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public ResponseEntity<PysioQuestionsRes> getAllFallback(Exception ex) {
        return ResponseEntity.status(429)
                .body(new PysioQuestionsRes(
                        "Too many requests. Please try again after some time.",
                        429,
                        false,
                        null));
    }

    public ResponseEntity<Response> getByKeysFallback(MutiplePartsDto keys, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public ResponseEntity<Response> updateByKeyFallback(
            String key,
            QuestionsDTO dto,
            Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public ResponseEntity<Response> deleteQuestionByKeyAndIdFallback(
            String key,
            long questionId,
            Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public ResponseEntity<Response> getExerciseSessionsWithRecordsFallback(
            String clinicId,
            String branchId,
            String bookingId,
            String patientId,
            String therapistId,
            String therapistRecordId,
            Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public ResponseEntity<Response> buildRateLimitResponse() {
        log.warn("Returning physiotherapy rate limit response");
        return ResponseEntity.status(429)
                .body(new Response(
                        "Too many requests. Please try again after some time.",
                        429,
                        false,
                        null));
    }

}