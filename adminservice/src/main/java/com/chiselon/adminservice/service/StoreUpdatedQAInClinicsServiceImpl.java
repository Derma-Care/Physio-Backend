package com.chiselon.adminservice.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chiselon.adminservice.dto.QuestionAnswerDTO;
import com.chiselon.adminservice.dto.StoreUpdatedQAInClinicsDTO;
import com.chiselon.adminservice.entity.QuestionAnswer;
import com.chiselon.adminservice.entity.StoreUpdatedQAInClinics;
import com.chiselon.adminservice.repository.QuetionsAndAnswerForAddClinicRepository;
import com.chiselon.adminservice.repository.StoreUpdatedQAInClinicsRepository;
import com.chiselon.adminservice.util.Response;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j

public class StoreUpdatedQAInClinicsServiceImpl implements StoreUpdatedQAInClinicsService {

    @Autowired
    private QuetionsAndAnswerForAddClinicRepository quetionsAndAnswerForAddClinicRepository;

    @Autowired
    private StoreUpdatedQAInClinicsRepository storeUpdatedQAInClinicsRepository;
    @Override
    public Response saveQaAndAnswers(StoreUpdatedQAInClinicsDTO storeUpdatedQAInClinicsDTO) {

        log.info("Started saveQaAndAnswers()");

        Response response = new Response();

        try {
            log.info("Received {} questions for processing",
                    storeUpdatedQAInClinicsDTO.getQuestionsAndAnswers().size());

            // Create and save entity
            StoreUpdatedQAInClinics storeQA = new StoreUpdatedQAInClinics();

            List<QuestionAnswer> ansFromDTO = storeUpdatedQAInClinicsDTO.getQuestionsAndAnswers()
                    .stream()
                    .map(a -> new QuestionAnswer(a.getQuestion(), a.isAnswer()))
                    .collect(Collectors.toList());

            log.debug("Converted DTO questions to entity objects");

            storeQA.setQuestionsAndAnswers(ansFromDTO);

            // Calculate count and score
            long trueCount = ansFromDTO.stream()
                    .filter(QuestionAnswer::isAnswer)
                    .count();

            log.info("Number of answered 'true' questions: {}", trueCount);

            // Calculate score
            double rawScore = (trueCount > 0) ? (trueCount / 2.0) : 0.0;
            long score = Math.round(rawScore);

            log.info("Calculated score: {}", score);

            // Set values
            storeQA.setCount((int) trueCount);
            storeQA.setScore((int) score);
            storeQA.setSubmitedQA(true);

            log.debug("Prepared StoreUpdatedQAInClinics entity for saving");

            // Save to DB
            StoreUpdatedQAInClinics savedQA = storeUpdatedQAInClinicsRepository.save(storeQA);

            log.info("Q&A saved successfully with ID: {}", savedQA.getId());

            // Convert to DTO
            StoreUpdatedQAInClinicsDTO dto = new StoreUpdatedQAInClinicsDTO();
            dto.setId(savedQA.getId());

            List<QuestionAnswerDTO> qaDTO = savedQA.getQuestionsAndAnswers()
                    .stream()
                    .map(b -> new QuestionAnswerDTO(b.getQuestion(), b.isAnswer()))
                    .collect(Collectors.toList());

            dto.setQuestionsAndAnswers(qaDTO);
            dto.setCount(savedQA.getCount());
            dto.setScore(savedQA.getScore());
            dto.setSubmitedQA(savedQA.isSubmitedQA());

            log.debug("Converted saved entity to response DTO");

            // Success response
            response.setSuccess(true);
            response.setData(dto);
            response.setMessage("Questions and answers stored successfully");
            response.setStatus(200);

            log.info("saveQaAndAnswers() completed successfully");

        } catch (Exception ex) {

            log.error("Exception occurred while saving Q&A. Error: {}", ex.getMessage(), ex);

            response.setSuccess(false);
            response.setData(null);
            response.setMessage("An error occurred while saving Q&A: " + ex.getMessage());
            response.setStatus(500);
        }

        return response;
    }

    @Override
    public Response updateQaAndAnswers(String id, StoreUpdatedQAInClinicsDTO storeUpdatedQAInClinicsDTO) {

        log.info("Started updateQaAndAnswers() for ID: {}", id);

        Response response = new Response();

        try {

            log.info("Fetching Q&A record with ID: {}", id);

            Optional<StoreUpdatedQAInClinics> existingQAOpt = storeUpdatedQAInClinicsRepository.findById(id);

            if (existingQAOpt.isEmpty()) {
                log.warn("No Q&A record found for ID: {}", id);

                response.setSuccess(false);
                response.setMessage("No Q&A found for id: " + id);
                response.setStatus(404);
                return response;
            }

            StoreUpdatedQAInClinics existingQA = existingQAOpt.get();

            log.info("Q&A record found. Updating {} questions.",
                    storeUpdatedQAInClinicsDTO.getQuestionsAndAnswers().size());

            // Replace with updated answers
            List<QuestionAnswer> updatedList = storeUpdatedQAInClinicsDTO.getQuestionsAndAnswers()
                    .stream()
                    .map(a -> new QuestionAnswer(a.getQuestion(), a.isAnswer()))
                    .collect(Collectors.toList());

            existingQA.setQuestionsAndAnswers(updatedList);

            log.debug("Updated question list mapped from DTO to entity.");

            // Calculate count
            long trueCount = existingQA.getQuestionsAndAnswers()
                    .stream()
                    .filter(QuestionAnswer::isAnswer)
                    .count();

            log.info("Total 'true' answers after update: {}", trueCount);

            // Calculate score
            double rawScore = (trueCount > 0) ? (trueCount / 2.0) : 0.0;
            long score = Math.round(rawScore);

            log.info("Calculated score: {}", score);

            existingQA.setCount((int) trueCount);
            existingQA.setScore((int) score);

            log.debug("Count and score updated in entity.");

            // Save updated entity
            StoreUpdatedQAInClinics savedQA = storeUpdatedQAInClinicsRepository.save(existingQA);

            log.info("Q&A record updated successfully with ID: {}", savedQA.getId());

            // Prepare DTO
            StoreUpdatedQAInClinicsDTO dto = new StoreUpdatedQAInClinicsDTO();
            dto.setId(savedQA.getId());

            List<QuestionAnswerDTO> qaDTO = savedQA.getQuestionsAndAnswers()
                    .stream()
                    .map(b -> new QuestionAnswerDTO(b.getQuestion(), b.isAnswer()))
                    .collect(Collectors.toList());

            dto.setQuestionsAndAnswers(qaDTO);
            dto.setCount(savedQA.getCount());
            dto.setScore(savedQA.getScore());

            log.debug("Response DTO prepared successfully.");

            // Build response
            response.setSuccess(true);
            response.setData(dto);
            response.setMessage("Q&A updated successfully with count and score");
            response.setStatus(200);

            log.info("updateQaAndAnswers() completed successfully for ID: {}", id);

        } catch (Exception ex) {

            log.error("Exception occurred while updating Q&A for ID: {}. Error: {}", id, ex.getMessage(), ex);

            response.setSuccess(false);
            response.setMessage("Error while updating Q&A: " + ex.getMessage());
            response.setStatus(500);
        }

        return response;
    }
    @Override
    public Response getById(String id) {

        log.info("Started getById() for Q&A ID: {}", id);

        Response response = new Response();

        try {

            log.info("Fetching Q&A record with ID: {}", id);

            Optional<StoreUpdatedQAInClinics> qaOpt = storeUpdatedQAInClinicsRepository.findById(id);

            if (qaOpt.isEmpty()) {

                log.warn("No Q&A record found for ID: {}", id);

                response.setSuccess(false);
                response.setMessage("No Q&A found for id: " + id);
                response.setStatus(404);
                return response;
            }

            StoreUpdatedQAInClinics qa = qaOpt.get();

            log.info("Q&A record found with ID: {}", qa.getId());

            // Convert entity to DTO
            StoreUpdatedQAInClinicsDTO dto = new StoreUpdatedQAInClinicsDTO();
            dto.setId(qa.getId());

            List<QuestionAnswerDTO> qaDTO = qa.getQuestionsAndAnswers()
                    .stream()
                    .map(b -> new QuestionAnswerDTO(b.getQuestion(), b.isAnswer()))
                    .collect(Collectors.toList());

            dto.setQuestionsAndAnswers(qaDTO);
            dto.setCount(qa.getCount());
            dto.setScore(qa.getScore());

            log.debug("Converted entity to DTO. Count: {}, Score: {}",
                    qa.getCount(), qa.getScore());

            response.setSuccess(true);
            response.setData(dto);
            response.setMessage("Q&A retrieved successfully");
            response.setStatus(200);

            log.info("getById() completed successfully for ID: {}", id);

        } catch (Exception ex) {

            log.error("Exception occurred while retrieving Q&A for ID: {}. Error: {}",
                    id, ex.getMessage(), ex);

            response.setSuccess(false);
            response.setMessage("Error while retrieving Q&A: " + ex.getMessage());
            response.setStatus(500);
        }

        return response;
    }
    @Override
    public Response getAll() {

        log.info("Started getAll() to retrieve all Q&A records");

        Response response = new Response();

        try {

            log.info("Fetching all Q&A records from the database");

            List<StoreUpdatedQAInClinics> allQAs = storeUpdatedQAInClinicsRepository.findAll();

            log.info("Retrieved {} Q&A record(s) from the database", allQAs.size());

            List<StoreUpdatedQAInClinicsDTO> dtoList = allQAs.stream().map(qa -> {

                StoreUpdatedQAInClinicsDTO dto = new StoreUpdatedQAInClinicsDTO();
                dto.setId(qa.getId());

                List<QuestionAnswerDTO> qaDTO = qa.getQuestionsAndAnswers()
                        .stream()
                        .map(b -> new QuestionAnswerDTO(b.getQuestion(), b.isAnswer()))
                        .collect(Collectors.toList());

                dto.setQuestionsAndAnswers(qaDTO);
                dto.setCount(qa.getCount());
                dto.setScore(qa.getScore());

                return dto;

            }).collect(Collectors.toList());

            log.debug("Converted {} Q&A record(s) into DTOs", dtoList.size());

            response.setSuccess(true);
            response.setData(dtoList);
            response.setMessage("All Q&A retrieved successfully");
            response.setStatus(200);

            log.info("getAll() completed successfully. Returned {} record(s)", dtoList.size());

        } catch (Exception ex) {

            log.error("Exception occurred while retrieving all Q&A records. Error: {}",
                    ex.getMessage(), ex);

            response.setSuccess(false);
            response.setMessage("Error while retrieving all Q&A: " + ex.getMessage());
            response.setStatus(500);
        }

        return response;
    }

    @Override
    public Response deleteById(String id) {

        log.info("Started deleteById() for Q&A ID: {}", id);

        Response response = new Response();

        try {

            log.info("Checking existence of Q&A record with ID: {}", id);

            Optional<StoreUpdatedQAInClinics> qaOpt = storeUpdatedQAInClinicsRepository.findById(id);

            if (qaOpt.isEmpty()) {

                log.warn("No Q&A record found for ID: {}", id);

                response.setSuccess(false);
                response.setMessage("No Q&A found for id: " + id);
                response.setStatus(404);
                return response;
            }

            log.info("Q&A record found. Proceeding to delete ID: {}", id);

            storeUpdatedQAInClinicsRepository.deleteById(id);

            log.info("Q&A record deleted successfully for ID: {}", id);

            response.setSuccess(true);
            response.setMessage("Q&A deleted successfully for id: " + id);
            response.setStatus(200);

            log.info("deleteById() completed successfully for ID: {}", id);

        } catch (Exception ex) {

            log.error("Exception occurred while deleting Q&A for ID: {}. Error: {}",
                    id, ex.getMessage(), ex);

            response.setSuccess(false);
            response.setMessage("Error while deleting Q&A: " + ex.getMessage());
            response.setStatus(500);
        }

        return response;
    }
}

