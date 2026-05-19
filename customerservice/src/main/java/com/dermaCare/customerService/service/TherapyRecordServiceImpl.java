package com.dermaCare.customerService.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.dermaCare.customerService.dto.TherapyRecordDTO;
import com.dermaCare.customerService.dto.TherophyRecordListDTO;
import com.dermaCare.customerService.entity.TherapyRecord;
import com.dermaCare.customerService.entity.TherophyRecordList;
import com.dermaCare.customerService.repository.TherapyRecordRepository;
import com.dermaCare.customerService.util.Response;


@Service
public class TherapyRecordServiceImpl implements TherapyRecordService{

	 @Autowired
	    private TherapyRecordRepository repository;

	 
	 @Override
	    public ResponseEntity<?> createTherapyRecord(TherapyRecordDTO dto) {

	        Response response = new Response();

	        try {

	            TherapyRecord therapyRecord = mapToEntity(dto);
	            therapyRecord.setStatus("pending");
	           
	            TherapyRecord savedRecord = repository.save(therapyRecord);

	            response.setMessage("Therapy record created successfully");
	            response.setStatus(HttpStatus.CREATED.value());
	            response.setSuccess(true);
	            response.setData(mapToDTO(savedRecord));

	            return new ResponseEntity<>(response, HttpStatus.CREATED);

	        } catch (Exception e) {

	            response.setMessage(e.getMessage());
	            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
	            response.setSuccess(false);	          

	            return new ResponseEntity<>(
	                    response,
	                    HttpStatus.INTERNAL_SERVER_ERROR);
	        }
	    }

	 
	 @Override
	 public ResponseEntity<?> updateTherapyRecord(
	         String therapyrecordid,
	         TherapyRecordDTO dto) {

	     Response response = new Response();

	     try {

	         Optional<TherapyRecord> optional =
	                 repository.findByTherapyrecordid(therapyrecordid);
	         if (optional.isEmpty()) {

	             response.setMessage("Therapy record not found");
	             response.setStatus(HttpStatus.NOT_FOUND.value());
	             response.setSuccess(false);

	             return new ResponseEntity<>(
	                     response,
	                     HttpStatus.NOT_FOUND);
	         }

	         TherapyRecord existing = optional.get();

	         // ================= IF ELSE MAPPING =================

	         if (dto.getClincinid() != null &&
	                 !dto.getClincinid().isEmpty()) {

	             existing.setClincinid(dto.getClincinid());
	         }

	         if (dto.getBrnchid() != null &&
	                 !dto.getBrnchid().isEmpty()) {

	             existing.setBrnchid(dto.getBrnchid());
	         }

	         if (dto.getPatientid() != null &&
	                 !dto.getPatientid().isEmpty()) {

	             existing.setPatientid(dto.getPatientid());
	         }

	         if (dto.getDoctorid() != null &&
	                 !dto.getDoctorid().isEmpty()) {

	             existing.setDoctorid(dto.getDoctorid());
	         }

	         if (dto.getName() != null &&
	                 !dto.getName().isEmpty()) {

	             existing.setName(dto.getName());
	         }

	         if (dto.getStatus() != null &&
	                 !dto.getStatus().isEmpty()) {

	             existing.setStatus(dto.getStatus());
	         }

	         if (dto.getExcerciseId() != null &&
	                 !dto.getExcerciseId().isEmpty()) {

	             existing.setExcerciseId(dto.getExcerciseId());
	         }

	         if (dto.getSessioncountremaining() != null) {
	        	 existing.setSessioncountremaining(dto.getSessioncountremaining());
	        	} else {
	        		existing.setSessioncountremaining(0); // default value
	        	}

	        	if (dto.getFrequancy() != null) {
	        	    dto.setFrequancy(dto.getFrequancy());
	        	} else {
	        		existing.setFrequancy("");
	        	}

	        	if (dto.getDuration() != null) {
	        		existing.setDuration(dto.getDuration());
	        	} else {
	        		existing.setDuration("");
	        	}
	         // ================= THERAPY RECORD LIST =================

	         if (dto.getTherapyrecord() != null &&
	                 !dto.getTherapyrecord().isEmpty()) {
	        	 List<TherophyRecordList> list = existing.getTherapyrecord();                 
	             List<TherophyRecordList> therapyList =
	                     dto.getTherapyrecord()
	                     .stream()
	                     .map(recordDto -> {
	                    	 TherophyRecordList therapy =
	                                 new TherophyRecordList();

	                         if (recordDto.getSetsdone() != null) {
	                             therapy.setSetsdone(
	                                     recordDto.getSetsdone());
	                         }

	                      // ================= REPITATION DONE =================

	                         if (recordDto.getRepitationdone() != null) {

	                             therapy.setRepitationdone(
	                                     recordDto.getRepitationdone());

	                         }

	                         // ================= SESSION COUNT =================

	                         if (recordDto.getSessioncount() != null) {
	                        	 therapy.setSessioncount(
	                                     recordDto.getSessioncount());
	                        try {
	                         TherophyRecordList lst = existing.getTherapyrecord().get(existing.getTherapyrecord().size()-1);
	                         //System.out.println(lst);
	                         int value = lst.getSession().intValue()-recordDto.getSessioncount().intValue();
	                         if(value!=0) {  	 
	                    	  existing.setStatus("Active");
	                    	  existing.setSessioncountremaining(value);
	                    	  }else {
	                    		  existing.setStatus("Completed"); 
	                    		  existing.setSessioncountremaining(value);
	                    	  }}catch(Exception e) {}}
	                         // ================= SESSION =================

	                         if (recordDto.getSession() != null) {

	                             therapy.setSession(
	                                     recordDto.getSession());}	
	                         
	                         if (recordDto.getSessioncompleted() != null) {
	                             therapy.setSessioncompleted(
	                                     recordDto.getSessioncompleted());
	                         }

	                         if (recordDto.getDate() != null) {
	                             therapy.setDate(recordDto.getDate());
	                         }

	                         if (recordDto.getExcerciseId() != null &&
	                                 !recordDto.getExcerciseId().isEmpty()) {

	                             therapy.setExcerciseId(
	                                     recordDto.getExcerciseId());
	                         }

	                         if (recordDto.getNotes() != null &&
	                                 !recordDto.getNotes().isEmpty()) {

	                             therapy.setNotes(
	                                     recordDto.getNotes());
	                         }

	                         // IMAGE & VIDEO MAPPING

	                         if (recordDto.getBeforeImage() != null) {
	                             therapy.setBeforeImage(
	                                     recordDto.getBeforeImage()
	                                     .getBytes());
	                         }

	                         if (recordDto.getAfterImage() != null) {
	                             therapy.setAfterImage(
	                                     recordDto.getAfterImage()
	                                     .getBytes());
	                         }

	                         if (recordDto.getBeforeVideo() != null) {
	                             therapy.setBeforeVideo(
	                                     recordDto.getBeforeVideo()
	                                     .getBytes());
	                         }

	                         if (recordDto.getAfterVideo() != null) {
	                             therapy.setAfterVideo(
	                                     recordDto.getAfterVideo()
	                                     .getBytes());
	                         }
	                         list.add(therapy);
	                         return therapy;

	                     }).toList();
	           
	             existing.setTherapyrecord(list);
	         }

	         TherapyRecord updated = repository.save(existing);

	         response.setMessage("Therapy record updated successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);
	         response.setData(updated);

	         return new ResponseEntity<>(response, HttpStatus.OK);

	     } catch (Exception e) {

	         response.setMessage(e.getMessage());
	         response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
	         response.setSuccess(false);
	       
	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.INTERNAL_SERVER_ERROR);
	     }
	 }
	   
	 @Override
	 public ResponseEntity<?> getAllTherapyRecords() {

	     Response response = new Response();

	     try {

	         List<TherapyRecordDTO> list = repository.findAll()
	                 .stream()
	                 .map(this::mapToDTO)
	                 .collect(Collectors.toList());

	         // ================= EMPTY CHECK =================

	         if (list.isEmpty()) {

	             response.setMessage("No Therapy Records Found");
	             response.setStatus(HttpStatus.OK.value());
	             response.setSuccess(false);
	             response.setData(null);

	             return new ResponseEntity<>(
	                     response,
	                     HttpStatus.OK);
	         }

	         // ================= SUCCESS RESPONSE =================

	         response.setMessage("Therapy Records Retrieved Successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);
	         response.setData(list);

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.OK);

	     } catch (Exception e) {

	         response.setMessage(e.getMessage());
	         response.setStatus(
	                 HttpStatus.INTERNAL_SERVER_ERROR.value());
	         response.setSuccess(false);
	      

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.INTERNAL_SERVER_ERROR);
	     }
	 }

	 @Override
	 public ResponseEntity<?> getTherapyRecordById(String id) {

	     Response response = new Response();

	     try {

	         Optional<TherapyRecord> optional =
	                 repository.findById(id);

	         // ================= NOT FOUND =================

	         if (optional.isEmpty()) {

	             response.setMessage("Therapy Record Not Found");
	             response.setStatus(HttpStatus.OK.value());
	             response.setSuccess(false);
	             response.setData(null);

	             return new ResponseEntity<>(
	                     response,
	                     HttpStatus.OK);
	         }

	         // ================= SUCCESS =================

	         TherapyRecord entity = optional.get();

	         response.setMessage(
	                 "Therapy Record Retrieved Successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);
	         response.setData(mapToDTO(entity));

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.OK);

	     } catch (Exception e) {

	         response.setMessage(
	        		 e.getMessage());
	         response.setStatus(
	                 HttpStatus.INTERNAL_SERVER_ERROR.value());
	         response.setSuccess(false);
	         

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.INTERNAL_SERVER_ERROR);
	     }
	 }

	   
	    
	 @Override
	 public ResponseEntity<?> deleteTherapyRecord(String id) {

	     Response response = new Response();

	     try {

	         Optional<TherapyRecord> optional =
	                 repository.findById(id);

	         // ================= NOT FOUND =================

	         if (optional.isEmpty()) {

	             response.setMessage("Therapy Record Not Found");
	             response.setStatus(HttpStatus.NOT_FOUND.value());
	             response.setSuccess(false);

	             return new ResponseEntity<>(
	                     response,
	                     HttpStatus.NOT_FOUND);
	         }

	         // ================= DELETE =================

	         TherapyRecord entity = optional.get();

	         repository.delete(entity);

	         response.setMessage(
	                 "Therapy Record Deleted Successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.OK);

	     } catch (Exception e) {

	         response.setMessage(
	        		 e.getMessage());
	         response.setStatus(
	                 HttpStatus.INTERNAL_SERVER_ERROR.value());
	         response.setSuccess(false);
	        

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.INTERNAL_SERVER_ERROR);
	     }
	 }

	 @Override
	 public ResponseEntity<?> getByClinicBranchAndPatient(
	         String clinicId,
	         String branchId,
	         String patientId) {

	     Response response = new Response();

	     try {

	         List<TherapyRecordDTO> records =
	                 repository.findByClincinidAndBrnchidAndPatientid(
	                                 clinicId,
	                                 branchId,
	                                 patientId)
	                         .stream()
	                         .map(this::mapToDTO)
	                         .collect(Collectors.toList());

	         // ================= EMPTY CHECK =================

	         if (records.isEmpty()) {

	             response.setMessage("No Therapy Records Found");
	             response.setStatus(HttpStatus.OK.value());
	             response.setSuccess(false);
	             response.setData(null);

	             return new ResponseEntity<>(
	                     response,
	                     HttpStatus.OK);
	         }

	         // ================= SUCCESS =================

	         response.setMessage(
	                 "Therapy Records Retrieved Successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);
	         response.setData(records);

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.OK);

	     } catch (Exception e) {

	         response.setMessage(
	        		 e.getMessage());
	         response.setStatus(
	                 HttpStatus.INTERNAL_SERVER_ERROR.value());
	         response.setSuccess(false);
	        

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.INTERNAL_SERVER_ERROR);
	     }
	 }

	 
	 @Override
	 public ResponseEntity<?> getByClinicBranchPatientAndTherapyRecordId(
	         String clinicId,
	         String branchId,
	         String patientId,
	         String therapyRecordId) {

	     Response response = new Response();

	     try {

	         Optional<TherapyRecord> optional =
	                 repository
	                 .findByClincinidAndBrnchidAndPatientidAndTherapyrecordid(
	                         clinicId,
	                         branchId,
	                         patientId,
	                         therapyRecordId);

	         // ================= NOT FOUND =================

	         if (optional.isEmpty()) {

	             response.setMessage("Therapy Record Not Found");
	             response.setStatus(HttpStatus.OK.value());
	             response.setSuccess(false);
	             response.setData(null);

	             return new ResponseEntity<>(
	                     response,
	                     HttpStatus.OK);
	         }

	         // ================= SUCCESS =================

	         TherapyRecord record = optional.get();

	         response.setMessage(
	                 "Therapy Record Retrieved Successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);
	         response.setData(mapToDTO(record));

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.OK);

	     } catch (Exception e) {

	         response.setMessage(
	        		 e.getMessage());
	         response.setStatus(
	                 HttpStatus.INTERNAL_SERVER_ERROR.value());
	         response.setSuccess(false);
	        

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.INTERNAL_SERVER_ERROR);
	     }
	 }
	    // mapToEntity and mapToDTO methods remain same
	
	    // ========================= DTO -> ENTITY =========================

	   
	    
	    
	    @Override
	    public ResponseEntity<?> getTherapyRecordsByClinicAndBranchAndExercise(
	            String clinicId,
	            String branchId,
	            String exerciseId) {

	        Response response = new Response();

	        try {

	            List<TherapyRecord> records =
	                    repository.findByClincinidAndBrnchidAndExcerciseId(
	                            clinicId,
	                            branchId,
	                            exerciseId);

	            if (records.isEmpty()) {

	                response.setMessage("No therapy records found");
	                response.setStatus(HttpStatus.OK.value());
	                response.setSuccess(false);

	                return new ResponseEntity<>(response, HttpStatus.OK);
	            }

	            response.setMessage("Therapy records fetched successfully");
	            response.setStatus(HttpStatus.OK.value());
	            response.setSuccess(true);
	            response.setData(records);

	            return new ResponseEntity<>(response, HttpStatus.OK);

	        } catch (Exception e) {

	            response.setMessage(e.getMessage());
	            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
	            response.setSuccess(false);
	           	            return new ResponseEntity<>(response,
	                    HttpStatus.INTERNAL_SERVER_ERROR);
	        }
	    }
	    
	    int sessioncompleted = 0;
	    private TherapyRecord mapToEntity(TherapyRecordDTO dto) {
	    	
	        List<TherophyRecordList> therapyList =
	                dto.getTherapyrecord()
	                .stream()
	                .map(this::mapTherapyList)
	                .collect(Collectors.toList());

	        return TherapyRecord.builder()
	                .therapyrecordid(dto.getTherapyrecordid())
	                .clincinid(dto.getClincinid())
	                .brnchid(dto.getBrnchid())
	                .patientid(dto.getPatientid())
	                .doctorid(dto.getDoctorid())
	                .name(dto.getName())
	                .status(dto.getStatus())
	                .excerciseId(dto.getExcerciseId())
	                .sessioncountremaining(sessioncompleted).frequancy(dto.getFrequancy()).duration(dto.getDuration())
	                .therapyrecord(therapyList)
	                .build();
	    }

	    private TherophyRecordList mapTherapyList(
	            TherophyRecordListDTO dto) {
	    	try {
	    	sessioncompleted =  dto.getSession().intValue()-dto.getSessioncount().intValue();          	 
         	
	    	}catch(Exception e) {}
	        return new TherophyRecordList(
	                dto.getSetsdone(),
	                dto.getRepitationdone(),
	                dto.getSessioncount(),
	                dto.getSession(),
	                dto.getSessioncompleted(),
	                dto.getDate(),
	                dto.getExcerciseId(),
	                dto.getNotes(),
	                dto.getBeforeImage() != null
	                        ? dto.getBeforeImage().getBytes()
	                        : null,
	                dto.getAfterImage() != null
	                        ? dto.getAfterImage().getBytes()
	                        : null,
	                dto.getBeforeVideo() != null
	                        ? dto.getBeforeVideo().getBytes()
	                        : null,
	                dto.getAfterVideo() != null
	                        ? dto.getAfterVideo().getBytes()
	                        : null);
	    }
	    
	    
	    private TherapyRecordDTO mapToDTO(TherapyRecord therapyRecord) {

	        List<TherophyRecordListDTO> therapyList =
	                null;

	        if (therapyRecord.getTherapyrecord() != null) {

	            therapyList = therapyRecord.getTherapyrecord()
	                    .stream()
	                    .map(record -> {

	                        TherophyRecordListDTO dto =
	                                new TherophyRecordListDTO();

	                        dto.setSetsdone(record.getSetsdone());
	                        dto.setRepitationdone(
	                                record.getRepitationdone());

	                        dto.setSessioncount(
	                                record.getSessioncount());

	                        dto.setSession(
	                                record.getSession());

	                        dto.setSessioncompleted(
	                                record.getSessioncompleted());

	                        dto.setDate(record.getDate());

	                        dto.setExcerciseId(
	                                record.getExcerciseId());

	                        dto.setNotes(record.getNotes());

	                        // byte[] → String conversion

	                        dto.setBeforeImage(
	                                record.getBeforeImage() != null
	                                        ? new String(
	                                        record.getBeforeImage())
	                                        : null);

	                        dto.setAfterImage(
	                                record.getAfterImage() != null
	                                        ? new String(
	                                        record.getAfterImage())
	                                        : null);

	                        dto.setBeforeVideo(
	                                record.getBeforeVideo() != null
	                                        ? new String(
	                                        record.getBeforeVideo())
	                                        : null);

	                        dto.setAfterVideo(
	                                record.getAfterVideo() != null
	                                        ? new String(
	                                        record.getAfterVideo())
	                                        : null);

	                        return dto;

	                    }).toList();
	        }

	        return TherapyRecordDTO.builder()
	                .therapyrecordid(
	                        therapyRecord.getTherapyrecordid())
	                .clincinid(
	                        therapyRecord.getClincinid())
	                .brnchid(
	                        therapyRecord.getBrnchid())
	                .patientid(
	                        therapyRecord.getPatientid())
	                .doctorid(
	                        therapyRecord.getDoctorid())
	                .name(
	                        therapyRecord.getName())
	                .status(
	                        therapyRecord.getStatus())
	                .sessioncountremaining(therapyRecord.getSessioncountremaining()).frequancy(therapyRecord.getFrequancy()).duration(therapyRecord.getDuration())
	                .excerciseId(
	                        therapyRecord.getExcerciseId())
	                .therapyrecord(therapyList)
	                .build();
	    }
	  
	}