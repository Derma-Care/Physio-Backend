package com.dermaCare.customerService.service;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.dermaCare.customerService.dto.TherapyRecordDTO;
import com.dermaCare.customerService.entity.TherapyRecord;
import com.dermaCare.customerService.repository.TherapyRecordRepository;
import com.dermaCare.customerService.util.Response;
import lombok.RequiredArgsConstructor;

@Service
public class TherapyRecordServiceImpl implements TherapyRecordService{

	 @Autowired
	    private TherapyRecordRepository repository;

	    @Override
	    public ResponseEntity<?> createTherapyRecord(TherapyRecordDTO dto) {

	        Response response = new Response();

	        TherapyRecord entity = mapToEntity(dto);

	        TherapyRecord saved = repository.save(entity);

	        response.setMessage("Therapy Record Created Successfully");
	        response.setStatus(HttpStatus.CREATED.value());
	        response.setSuccess(true);
	        response.setData(mapToDTO(saved));

	        return new ResponseEntity<>(response, HttpStatus.CREATED);
	    }

	    @Override
	    public ResponseEntity<?> getAllTherapyRecords() {

	        Response response = new Response();

	        List<TherapyRecordDTO> list = repository.findAll()
	                .stream()
	                .map(this::mapToDTO)
	                .collect(Collectors.toList());

	        response.setMessage("Therapy Records Retrieved Successfully");
	        response.setStatus(HttpStatus.OK.value());
	        response.setSuccess(true);
	        response.setData(list);

	        return new ResponseEntity<>(response, HttpStatus.OK);
	    }

	    @Override
	    public ResponseEntity<?> getTherapyRecordById(String id) {

	        Response response = new Response();

	        TherapyRecord entity = repository.findById(id)
	                .orElseThrow(() ->
	                        new RuntimeException("Therapy Record Not Found"));

	        response.setMessage("Therapy Record Retrieved Successfully");
	        response.setStatus(HttpStatus.OK.value());
	        response.setSuccess(true);
	        response.setData(mapToDTO(entity));

	        return new ResponseEntity<>(response, HttpStatus.OK);
	    }

	    @Override
	    public ResponseEntity<?> updateTherapyRecord(
	            String id,
	            TherapyRecordDTO dto) {

	        Response response = new Response();

	        TherapyRecord existing = repository.findById(id)
	                .orElseThrow(() ->
	                        new RuntimeException("Therapy Record Not Found"));

	        existing.setClincinid(dto.getClincinid());
	        existing.setBrnchid(dto.getBrnchid());
	        existing.setPatientid(dto.getPatientid());
	        existing.setName(dto.getName());
	        existing.setDoctorid(dto.getDoctorid());

	        if (dto.getSetsdone() != null) {
	            existing.setSetsdone(Integer.parseInt(dto.getSetsdone()));
	        }

	        existing.setRepitationdone(dto.isRepitationdone());
	        existing.setSessioncompleted(dto.getSessioncompleted());

	        if (dto.getNotes() != null) {
	            existing.setNotes(dto.getNotes());
	        }

	        // BEFORE IMAGE
	        if (dto.getBeforeImage() != null) {
	            existing.setBeforeImage(
	                    Base64.getDecoder().decode(dto.getBeforeImage())
	            );
	        }

	        // AFTER IMAGE
	        if (dto.getAfterImage() != null) {
	            existing.setAfterImage(
	                    Base64.getDecoder().decode(dto.getAfterImage())
	            );
	        }

	        // BEFORE VIDEO
	        if (dto.getBeforeVideo() != null) {
	            existing.setBeforeVideo(
	                    Base64.getDecoder().decode(dto.getBeforeVideo())
	            );
	        }

	        // AFTER VIDEO
	        if (dto.getAfterVideo() != null) {
	            existing.setAfterVideo(
	                    Base64.getDecoder().decode(dto.getAfterVideo())
	            );
	        }

	        TherapyRecord updated = repository.save(existing);

	        response.setMessage("Therapy Record Updated Successfully");
	        response.setStatus(HttpStatus.OK.value());
	        response.setSuccess(true);
	        response.setData(mapToDTO(updated));

	        return new ResponseEntity<>(response, HttpStatus.OK);
	    }
	    
	    @Override
	    public ResponseEntity<?> deleteTherapyRecord(String id) {

	        Response response = new Response();

	        TherapyRecord entity = repository.findById(id)
	                .orElseThrow(() ->
	                        new RuntimeException("Therapy Record Not Found"));

	        repository.delete(entity);

	        response.setMessage("Therapy Record Deleted Successfully");
	        response.setStatus(HttpStatus.OK.value());
	        response.setSuccess(true);

	        return new ResponseEntity<>(response, HttpStatus.OK);
	    }

	    @Override
	    public ResponseEntity<?> getByClinicBranchAndPatient(
	            String clinicId,
	            String branchId,
	            String patientId) {

	        Response response = new Response();

	        List<TherapyRecordDTO> records =
	                repository.findByClincinidAndBrnchidAndPatientid(
	                        clinicId,
	                        branchId,
	                        patientId)
	                .stream()
	                .map(this::mapToDTO)
	                .collect(Collectors.toList());

	        response.setMessage("Therapy Records Retrieved Successfully");
	        response.setStatus(HttpStatus.OK.value());
	        response.setSuccess(true);
	        response.setData(records);

	        return new ResponseEntity<>(response, HttpStatus.OK);
	    }

	    @Override
	    public ResponseEntity<?> getByClinicBranchPatientAndTherapyRecordId(
	            String clinicId,
	            String branchId,
	            String patientId,
	            String therapyRecordId) {

	        Response response = new Response();

	        TherapyRecord record =
	                repository
	                .findByClincinidAndBrnchidAndPatientidAndTherapyrecordid(
	                        clinicId,
	                        branchId,
	                        patientId,
	                        therapyRecordId)
	                .orElseThrow(() ->
	                        new RuntimeException("Therapy Record Not Found"));

	        response.setMessage("Therapy Record Retrieved Successfully");
	        response.setStatus(HttpStatus.OK.value());
	        response.setSuccess(true);
	        response.setData(mapToDTO(record));

	        return new ResponseEntity<>(response, HttpStatus.OK);
	    }

	    // mapToEntity and mapToDTO methods remain same
	
	    // ========================= DTO -> ENTITY =========================

	    private TherapyRecord mapToEntity(TherapyRecordDTO dto) {

	        return TherapyRecord.builder()
	                .therapyrecordid(dto.getTherapyrecordid())
	                .clincinid(dto.getClincinid())
	                .brnchid(dto.getBrnchid())
	                .patientid(dto.getPatientid())
	                .name(dto.getName())
	                .doctorid(dto.getDoctorid())

	                .setsdone(dto.getSetsdone() != null
	                        ? Integer.parseInt(dto.getSetsdone())
	                        : null)

	                .repitationdone(dto.isRepitationdone())

	                .sessioncompleted(dto.getSessioncompleted())

	                .notes(dto.getNotes())

	                // BEFORE IMAGE
	                .beforeImage(dto.getBeforeImage() != null
	                        ? Base64.getDecoder()
	                                .decode(dto.getBeforeImage())
	                        : null)

	                // AFTER IMAGE
	                .afterImage(dto.getAfterImage() != null
	                        ? Base64.getDecoder()
	                                .decode(dto.getAfterImage())
	                        : null)

	                // BEFORE VIDEO
	                .beforeVideo(dto.getBeforeVideo() != null
	                        ? Base64.getDecoder()
	                                .decode(dto.getBeforeVideo())
	                        : null)

	                // AFTER VIDEO
	                .afterVideo(dto.getAfterVideo() != null
	                        ? Base64.getDecoder()
	                                .decode(dto.getAfterVideo())
	                        : null)

	                .build();
	    }

	    // ========================= ENTITY -> DTO =========================

	    private TherapyRecordDTO mapToDTO(TherapyRecord entity) {

	        return TherapyRecordDTO.builder()

	                .therapyrecordid(entity.getTherapyrecordid())
	                .clincinid(entity.getClincinid())
	                .brnchid(entity.getBrnchid())
	                .patientid(entity.getPatientid())
	                .name(entity.getName())
	                .doctorid(entity.getDoctorid())

	                .setsdone(entity.getSetsdone() != null
	                        ? String.valueOf(entity.getSetsdone())
	                        : null)

	                .repitationdone(entity.isRepitationdone())

	                .sessioncompleted(entity.getSessioncompleted())

	                .notes(entity.getNotes())

	                // BEFORE IMAGE
	                .beforeImage(entity.getBeforeImage() != null
	                        ? Base64.getEncoder()
	                                .encodeToString(entity.getBeforeImage())
	                        : null)

	                // AFTER IMAGE
	                .afterImage(entity.getAfterImage() != null
	                        ? Base64.getEncoder()
	                                .encodeToString(entity.getAfterImage())
	                        : null)

	                // BEFORE VIDEO
	                .beforeVideo(entity.getBeforeVideo() != null
	                        ? Base64.getEncoder()
	                                .encodeToString(entity.getBeforeVideo())
	                        : null)

	                // AFTER VIDEO
	                .afterVideo(entity.getAfterVideo() != null
	                        ? Base64.getEncoder()
	                                .encodeToString(entity.getAfterVideo())
	                        : null)

	                .build();
	    }
	    
	  
	}
