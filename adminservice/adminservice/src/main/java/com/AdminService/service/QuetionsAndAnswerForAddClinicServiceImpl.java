package com.AdminService.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.AdminService.dto.QuestionAnswerDTO;
import com.AdminService.dto.QuetionsAndAnswerForAddClinicDTO;
import com.AdminService.entity.QuestionAnswer;
import com.AdminService.entity.QuetionsAndAnswerForAddClinic;
import com.AdminService.repository.QuetionsAndAnswerForAddClinicRepository;
import com.AdminService.util.Response;
import feign.FeignException;import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j

public class QuetionsAndAnswerForAddClinicServiceImpl implements QuetionsAndAnswerForAddClinicService {

	@Autowired
	private QuetionsAndAnswerForAddClinicRepository repository;

	// ✅ Save Questions only if not already present
	@Override
	public Response saveQuetions(QuetionsAndAnswerForAddClinicDTO dto) {

	    log.info("Saving clinic questions.");

	    if (repository.count() > 0) {
	        log.warn("Questions already exist. Save operation not allowed.");

	        Response response = new Response();
	        response.setSuccess(false);
	        response.setMessage("Questions already exist. Please use update instead.");
	        response.setStatus(400);
	        return response;
	    }

	    QuetionsAndAnswerForAddClinic entity = new QuetionsAndAnswerForAddClinic();

	    List<QuestionAnswer> qaList = dto.getQuestionsAndAnswers().stream()
	            .map(qaDto -> new QuestionAnswer(qaDto.getQuestion(), qaDto.isAnswer()))
	            .collect(Collectors.toList());

	    entity.setQuestionsAndAnswers(qaList);

	    QuetionsAndAnswerForAddClinic saved = repository.save(entity);

	    log.info("Clinic questions saved successfully. Id: {}", saved.getId());

	    QuetionsAndAnswerForAddClinicDTO toSaveDTO = convertToDTO(saved);

	    Response response = new Response();
	    response.setSuccess(true);
	    response.setData(toSaveDTO);
	    response.setMessage("Questions added successfully");
	    response.setStatus(200);
	    return response;
	}

	// ✅ Update existing Questions
	@Override
	public Response updateQuetions(QuetionsAndAnswerForAddClinicDTO dto) {

	    log.info("Updating clinic questions.");

	    QuetionsAndAnswerForAddClinic existing = repository.findAll().stream().findFirst().orElse(null);

	    Response response = new Response();

	    if (existing == null) {

	        log.warn("No clinic questions found to update.");

	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage("No questions found. Please save first.");
	        response.setStatus(404);
	        return response;
	    }

	    List<QuestionAnswer> qaList = dto.getQuestionsAndAnswers().stream()
	            .map(qaDto -> new QuestionAnswer(qaDto.getQuestion(), qaDto.isAnswer()))
	            .collect(Collectors.toList());

	    existing.setQuestionsAndAnswers(qaList);

	    QuetionsAndAnswerForAddClinic updated = repository.save(existing);

	    log.info("Clinic questions updated successfully. Id: {}", updated.getId());

	    QuetionsAndAnswerForAddClinicDTO updatedDTO = convertToDTO(updated);

	    response.setSuccess(true);
	    response.setData(updatedDTO);
	    response.setMessage("Questions updated successfully");
	    response.setStatus(200);
	    return response;
	}

	@Override
	public Response getQuetions() {

	    log.info("Fetching clinic questions.");

	    QuetionsAndAnswerForAddClinic existing = repository.findAll().stream().findFirst().orElse(null);

	    Response response = new Response();

	    if (existing == null) {

	        log.warn("No clinic questions found.");

	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage("No questions found");
	        response.setStatus(404);
	        return response;
	    }

	    log.info("Clinic questions fetched successfully. Id: {}", existing.getId());

	    QuetionsAndAnswerForAddClinicDTO dto = convertToDTO(existing);

	    response.setSuccess(true);
	    response.setData(dto);
	    response.setMessage("Questions fetched successfully");
	    response.setStatus(200);
	    return response;
	}

	// ✅ Utility method for mapping Entity → DTO
	private QuetionsAndAnswerForAddClinicDTO convertToDTO(QuetionsAndAnswerForAddClinic entity) {
		QuetionsAndAnswerForAddClinicDTO dto = new QuetionsAndAnswerForAddClinicDTO();
		dto.setId(entity.getId());
		List<QuestionAnswerDTO> dtoList = entity.getQuestionsAndAnswers().stream()
				.map(qaEntity -> new QuestionAnswerDTO(qaEntity.getQuestion(), qaEntity.isAnswer()))
				.collect(Collectors.toList());
		dto.setQuestionsAndAnswers(dtoList);
		return dto;
	}

}