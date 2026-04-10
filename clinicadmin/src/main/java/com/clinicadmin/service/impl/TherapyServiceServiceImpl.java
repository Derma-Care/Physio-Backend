package com.clinicadmin.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.TherapyExercisesDTO;
import com.clinicadmin.dto.TherapyServiceDTO;
import com.clinicadmin.entity.TherapyExercises;
import com.clinicadmin.entity.TherapyService;
import com.clinicadmin.repository.TherapyExercisesRepository;
import com.clinicadmin.repository.TherapyServiceRepository;
import com.clinicadmin.service.TherapyServiceService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TherapyServiceServiceImpl implements TherapyServiceService {
	
    @Autowired
    private  TherapyServiceRepository repository;
    
    @Autowired
    private TherapyExercisesRepository exercisesRepository;

    //  CREATETHERAPY
    @Override
    public Response createTherapy(TherapyServiceDTO dto) {

        TherapyService therapy = mapToEntity(dto);
        TherapyService saved = repository.save(therapy);

        Response response = new Response();
        response.setSuccess(true);
        response.setData(mapToDTO(saved));
        response.setMessage("Created successfully");
        response.setStatus(HttpStatus.CREATED.value());

        return response;
    }

    @Override
    public Response getByClinicAndBranch(String clinicId, String branchId) {

        List<TherapyService> list = repository.findByClinicIdAndBranchId(clinicId, branchId);

        List<Map<String, Object>> finalList = new ArrayList<>();

        for (TherapyService therapy : list) {

            List<String> exerciseIds = therapy.getExerciseIds();

            List<TherapyExercises> exercises = new ArrayList<>();

            if (exerciseIds != null && !exerciseIds.isEmpty()) {
                exercises = exercisesRepository
                        .findByTherapyExercisesIdInAndClinicIdAndBranchId(
                                exerciseIds,
                                clinicId,
                                branchId
                        );
            }

            // 🔥 Convert to required format
            List<Map<String, Object>> exerciseList = new ArrayList<>();

            for (TherapyExercises ex : exercises) {
                Map<String, Object> map = new HashMap<>();
                map.put("exerciseName", ex.getName());
                map.put("exerciseId", ex.getTherapyExercisesId());
                exerciseList.add(map);
            }

            Map<String, Object> data = new LinkedHashMap<>();

       
         data.put("id", therapy.getId());
         data.put("consentType", therapy.getConsentType());
         data.put("therapyName", therapy.getTherapyName());
         data.put("clinicId", therapy.getClinicId());
         data.put("branchId", therapy.getBranchId());
         data.put("noExerciseIdCount",
                 therapy.getExerciseIds() != null ? therapy.getExerciseIds().size() : 0);
         data.put("exercises", exerciseList);

            finalList.add(data);
        }

        Response response = new Response();
        response.setSuccess(true);
        response.setData(finalList);
        response.setMessage("Fetched successfully");
        response.setStatus(HttpStatus.OK.value());

        return response;
    }

    //  GET BY id + clinicId + branchId
    @Override
    public Response getByIdClinicBranch(String id, String clinicId, String branchId) {

        Optional<TherapyService> optional =
                repository.findByIdAndClinicIdAndBranchId(id, clinicId, branchId);

        Response response = new Response();

        if (optional.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Data not found");
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return response;
        }

        response.setSuccess(true);
        response.setData(mapToDTO(optional.get()));
        response.setMessage("Fetched successfully");
        response.setStatus(HttpStatus.OK.value());

        return response;
    }
    
   
    public TherapyServiceDTO getById(String id) {

        Optional<TherapyService> optional =
                repository.findById(id);
        if (optional.isEmpty()) {
            return null;
        }       
        return mapToDTO(optional.get()); 
    }

 //  UPDATE BY ID ONLY
    @Override
    public Response updateTherapyById(String id, TherapyServiceDTO dto) {

        Optional<TherapyService> optional = repository.findById(id);

        Response response = new Response();

        if (optional.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Data not found");
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return response;
        }

        TherapyService existing = optional.get();

        // reuse update logic
        updateEntityFromDTO(existing, dto);

        TherapyService updated = repository.save(existing);

        response.setSuccess(true);
        response.setData(mapToDTO(updated));
        response.setMessage("Updated successfully");
        response.setStatus(HttpStatus.OK.value());

        return response;
    }

    // ✅ DELETE
    @Override
    public Response deleteTherapyById(String id) {

        Optional<TherapyService> optional = repository.findById(id);

        Response response = new Response();

        if (optional.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Data not found");
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return response;
        }

        repository.delete(optional.get());

        response.setSuccess(true);
        response.setMessage("Deleted successfully");
        response.setStatus(HttpStatus.OK.value());

        return response;
    }
    
    

    // ================== MAPPERS ==================

    private TherapyService mapToEntity(TherapyServiceDTO dto) {
        TherapyService therapy = new TherapyService();
        therapy.setId(dto.getId());
        therapy.setConsentType(dto.getConsentType());
        therapy.setExerciseIds(dto.getExerciseIds());
        therapy.setTherapyName(dto.getTherapyName());
        therapy.setClinicId(dto.getClinicId());
        therapy.setBranchId(dto.getBranchId());

        return therapy;
    }

    private TherapyServiceDTO mapToDTO(TherapyService therapy) {
        TherapyServiceDTO dto = new TherapyServiceDTO();
        dto.setId(therapy.getId());
        dto.setConsentType(therapy.getConsentType());
        dto.setExerciseIds(therapy.getExerciseIds());
        dto.setTherapyName(therapy.getTherapyName());
        dto.setClinicId(therapy.getClinicId());
        dto.setBranchId(therapy.getBranchId());

        // ✅ AUTO COUNT LOGIC
        if (therapy.getExerciseIds() != null) {
            dto.setNoExerciseIdCount((therapy.getExerciseIds().size()));
        } else {
            dto.setNoExerciseIdCount(0);
        }

        return dto;
    }

    //  UPDATE SAFE METHOD
    private void updateEntityFromDTO(TherapyService entity, TherapyServiceDTO dto) {

        if (dto.getConsentType() != 0) {
            entity.setConsentType(dto.getConsentType());
        }

        if (dto.getExerciseIds() != null) {
            entity.setExerciseIds(dto.getExerciseIds());
        }

        if (dto.getTherapyName() != null) {
            entity.setTherapyName(dto.getTherapyName());
        }
    }
    @Override
    public Response getTherapyWithExercises(String id, String clinicId, String branchId) {

        Optional<TherapyService> optional =
                repository.findByIdAndClinicIdAndBranchId(id, clinicId, branchId);

        Response response = new Response();

        if (optional.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Therapy not found");
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return response;
        }

        TherapyService therapy = optional.get();

        List<String> exerciseIds = therapy.getExerciseIds();

        List<TherapyExercises> exercises = new ArrayList<>();

        // ✅ Fetch exercises from DB
        if (exerciseIds != null && !exerciseIds.isEmpty()) {
            exercises = exercisesRepository
                    .findByTherapyExercisesIdInAndClinicIdAndBranchId(
                            exerciseIds,
                            clinicId,
                            branchId
                    );
        }

        // 🔥 AUTO CLEANUP (remove invalid/deleted IDs)
        List<String> validIds = exercises.stream()
                .map(TherapyExercises::getTherapyExercisesId)
                .toList();

        if (exerciseIds != null && !exerciseIds.equals(validIds)) {
            therapy.setExerciseIds(validIds);
            repository.save(therapy);
        }

        TherapyServiceDTO dto = mapToDTO(therapy);

        // ✅ Set full exercise data
        dto.setExercises(exercises);

        // 🔥 FIX: Always use DB data for count
        dto.setNoExerciseIdCount(exercises.size());

        response.setSuccess(true);
        response.setData(dto);
        response.setMessage("Fetched successfully with exercises");
        response.setStatus(HttpStatus.OK.value());

        return response;
    }
 
    public TherapyServiceDTO getTherapyWithExercisesWithId(String id) {

        Optional<TherapyService> optional =
                repository.findById(id);       
     //System.out.println(id);
        if (optional.isEmpty()) {          
            return null;}
        TherapyService therapy = optional.get();
        TherapyServiceDTO theryServiceDto =  new ObjectMapper().convertValue(therapy, TherapyServiceDTO.class);
        List<String> exerciseIds = therapy.getExerciseIds();
        List<TherapyExercises> exercisesDto = new ArrayList<>();
        // ✅ 3. Only call DB if list is not empty
        if (exerciseIds != null && !exerciseIds.isEmpty()) {
        	for(String s:exerciseIds) {
        	TherapyExercises ex  = exercisesRepository.findByTherapyExercisesId(s).get();
        	if(ex != null){
        exercisesDto.add(ex);}}
        theryServiceDto.setExercises(exercisesDto);
        return theryServiceDto;
    }else {
    	return null;
    }
}}