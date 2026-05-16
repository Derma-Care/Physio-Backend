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
	            existing.setNotes(Base64.getDecoder().decode(dto.getNotes()));
	        }

	        if (dto.getImage() != null) {
	            existing.setImage(Base64.getDecoder().decode(dto.getImage()));
	        }

	        if (dto.getVideo() != null) {
	            existing.setVideo(Base64.getDecoder().decode(dto.getVideo()));
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
	                .notes(dto.getNotes() != null
	                        ? Base64.getDecoder().decode(dto.getNotes())
	                        : null)
	                .image(dto.getImage() != null
	                        ? Base64.getDecoder().decode(dto.getImage())
	                        : null)
	                .video(dto.getVideo() != null
	                        ? Base64.getDecoder().decode(dto.getVideo())
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
	                .notes(entity.getNotes() != null
	                        ? Base64.getEncoder().encodeToString(entity.getNotes())
	                        : null)
	                .image(entity.getImage() != null
	                        ? Base64.getEncoder().encodeToString(entity.getImage())
	                        : null)
	                .video(entity.getVideo() != null
	                        ? Base64.getEncoder().encodeToString(entity.getVideo())
	                        : null)
	                .build();
	    }
	    
	  
	}