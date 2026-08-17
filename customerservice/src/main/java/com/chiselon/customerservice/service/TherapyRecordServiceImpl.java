package com.chiselon.customerservice.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chiselon.customerservice.dto.TherapyRecordDTO;
import com.chiselon.customerservice.dto.TherophyRecordListDTO;
import com.chiselon.customerservice.entity.TherapyRecord;
import com.chiselon.customerservice.entity.TherophyRecordList;
import com.chiselon.customerservice.repository.TherapyRecordRepository;
import com.chiselon.customerservice.util.Response;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class TherapyRecordServiceImpl implements TherapyRecordService{

	 @Autowired
	    private TherapyRecordRepository repository;
	 
	 @Autowired
	    private S3Service s3Service; 

	 @Override
	 @Secured("ROLE_CUSTOMER")
	 @RateLimiter(name = "therapyRecordService", fallbackMethod = "createTherapyRecordFallback")
	 public ResponseEntity<?> createTherapyRecord(TherapyRecordDTO dto) {

	     Response response = new Response();

	     log.info(
	             "Create therapy record request received. patientId={}, doctorId={}, exerciseId={}",
	             dto.getPatientid(),
	             dto.getDoctorid(),
	             dto.getExcerciseId());

	     try {

	         TherapyRecord therapyRecord = mapToEntity(dto);

	         log.info(
	                 "Persisting therapy record. patientId={}, exerciseId={}",
	                 dto.getPatientid(),
	                 dto.getExcerciseId());

	         TherapyRecord savedRecord = repository.save(therapyRecord);

	         log.info(
	                 "Therapy record created successfully. therapyRecordId={}, patientId={}, exerciseId={}",
	                 savedRecord.getTherapyrecordid(),
	                 savedRecord.getPatientid(),
	                 savedRecord.getExcerciseId());

	         response.setMessage("Therapy record created successfully");
	         response.setStatus(HttpStatus.CREATED.value());
	         response.setSuccess(true);
	         response.setData(mapToDTO(savedRecord));

	         return new ResponseEntity<>(response, HttpStatus.CREATED);

	     } catch (Exception e) {

	         log.error(
	                 "Failed to create therapy record. patientId={}, doctorId={}, exerciseId={}",
	                 dto.getPatientid(),
	                 dto.getDoctorid(),
	                 dto.getExcerciseId(),
	                 e);

	         response.setMessage(e.getMessage());
	         response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
	         response.setSuccess(false);

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.INTERNAL_SERVER_ERROR);
	     }
	 }
	 
	 @Override
	 @Secured("ROLE_CUSTOMER")
	 @RateLimiter(name = "therapyRecordService", fallbackMethod = "updateTherapyRecordFallback")
	 public ResponseEntity<?> updateTherapyRecord(
	         String therapyrecordid,
	         String excerciseId,
	         TherapyRecordDTO dto) {

	     Response response = new Response();

	     log.info(
	             "Update therapy record request received. therapyRecordId={}, exerciseId={}",
	             therapyrecordid,
	             excerciseId);

	     try {

	         log.info(
	                 "Searching therapy record. therapyRecordId={}, exerciseId={}",
	                 therapyrecordid,
	                 excerciseId);

	         Optional<TherapyRecord> optional =
	                 repository.findByTherapyrecordidAndExcerciseId(
	                         therapyrecordid,
	                         excerciseId);

	         if (optional.isEmpty()) {

	             log.warn(
	                     "Therapy record not found. therapyRecordId={}, exerciseId={}",
	                     therapyrecordid,
	                     excerciseId);

	             response.setMessage("Therapy record not found");
	             response.setStatus(HttpStatus.NOT_FOUND.value());
	             response.setSuccess(false);

	             return new ResponseEntity<>(
	                     response,
	                     HttpStatus.NOT_FOUND);
	         }

	         TherapyRecord existing = optional.get();

	         log.info(
	                 "Therapy record found. therapyRecordId={}, patientId={}, currentStatus={}",
	                 existing.getTherapyrecordid(),
	                 existing.getPatientid(),
	                 existing.getStatus());

	         String oldStatus = existing.getStatus();

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
	             existing.setSessioncountremaining(
	                     dto.getSessioncountremaining());
	         } else {
	             existing.setSessioncountremaining(0);
	         }

	         if (dto.getFrequancy() != null) {
	             existing.setFrequancy(dto.getFrequancy());
	         } else {
	             existing.setFrequancy("");
	         }

	         if (dto.getDuration() != null) {
	             existing.setDuration(dto.getDuration());
	         } else {
	             existing.setDuration("");
	         }

	         if (dto.getTherapyrecord() != null &&
	                 !dto.getTherapyrecord().isEmpty()) {

	             log.info(
	                     "Processing therapy record updates. therapyRecordId={}, recordsCount={}",
	                     therapyrecordid,
	                     dto.getTherapyrecord().size());

	             List<TherophyRecordList> list =
	                     existing.getTherapyrecord();

	             dto.getTherapyrecord()
	                     .stream()
	                     .map(recordDto -> {

	                         TherophyRecordList therapy =
	                                 new TherophyRecordList();

	                         if (recordDto.getSessioncount() != null) {

	                             therapy.setSessioncount(
	                                     recordDto.getSessioncount());

	                             try {

	                                 TherophyRecordList lst =
	                                         existing.getTherapyrecord()
	                                                 .get(existing.getTherapyrecord().size() - 1);

	                                 int size =
	                                         existing.getTherapyrecord().size();

	                                 int add =
	                                         size + recordDto.getSessioncount();

	                                 int value =
	                                         lst.getSession() - add;

	                                 if (value != 0) {

	                                     existing.setStatus("Active");
	                                     existing.setSessioncountremaining(value);

	                                     log.info(
	                                             "Therapy status changed to Active. therapyRecordId={}, remainingSessions={}",
	                                             existing.getTherapyrecordid(),
	                                             value);

	                                 } else {

	                                     existing.setStatus("Completed");
	                                     existing.setSessioncountremaining(value);

	                                     log.info(
	                                             "Therapy completed. therapyRecordId={}, remainingSessions={}",
	                                             existing.getTherapyrecordid(),
	                                             value);
	                                 }

	                             } catch (Exception e) {

	                                 log.error(
	                                         "Session calculation failed. therapyRecordId={}, exerciseId={}",
	                                         existing.getTherapyrecordid(),
	                                         recordDto.getExcerciseId(),
	                                         e);
	                             }
	                         }

	                         list.add(therapy);
	                         return therapy;

	                     }).toList();

	             existing.setTherapyrecord(list);
	         }

	         log.info(
	                 "Persisting therapy record update. therapyRecordId={}, status={}",
	                 existing.getTherapyrecordid(),
	                 existing.getStatus());

	         TherapyRecord updated = repository.save(existing);

	         if (!String.valueOf(oldStatus)
	                 .equalsIgnoreCase(
	                         String.valueOf(updated.getStatus()))) {

	             log.info(
	                     "Therapy status transitioned. therapyRecordId={}, oldStatus={}, newStatus={}",
	                     updated.getTherapyrecordid(),
	                     oldStatus,
	                     updated.getStatus());
	         }

	         log.info(
	                 "Therapy record updated successfully. therapyRecordId={}, status={}, remainingSessions={}",
	                 updated.getTherapyrecordid(),
	                 updated.getStatus(),
	                 updated.getSessioncountremaining());

	         response.setMessage("Therapy record updated successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);
	         response.setData(updated);

	         return new ResponseEntity<>(response, HttpStatus.OK);

	     } catch (Exception e) {

	         log.error(
	                 "Failed to update therapy record. therapyRecordId={}, exerciseId={}",
	                 therapyrecordid,
	                 excerciseId,
	                 e);

	         response.setMessage(e.getMessage());
	         response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
	         response.setSuccess(false);

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.INTERNAL_SERVER_ERROR);
	     }
	 }
	 
	 @Override
	 @Secured("ROLE_CUSTOMER")
	 @RateLimiter(name = "therapyRecordService", fallbackMethod = "getAllTherapyRecordsFallback")
	 public ResponseEntity<?> getAllTherapyRecords() {

	     Response response = new Response();

	     log.info("Fetch all therapy records request received");

	     try {

	         List<TherapyRecordDTO> list = repository.findAll()
	                 .stream()
	                 .map(this::mapToDTO)
	                 .collect(Collectors.toList());

	         log.info(
	                 "Therapy records fetched from database. count={}",
	                 list.size());

	         if (list.isEmpty()) {

	             log.warn("No therapy records found");

	             response.setMessage("No Therapy Records Found");
	             response.setStatus(HttpStatus.OK.value());
	             response.setSuccess(false);
	             response.setData(null);

	             return new ResponseEntity<>(
	                     response,
	                     HttpStatus.OK);
	         }

	         log.info(
	                 "Returning therapy records successfully. count={}",
	                 list.size());

	         response.setMessage("Therapy Records Retrieved Successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);
	         response.setData(list);

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.OK);

	     } catch (Exception e) {

	         log.error(
	                 "Failed to fetch therapy records",
	                 e);

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
	 @Secured("ROLE_CUSTOMER")
	 @RateLimiter(name = "therapyRecordService", fallbackMethod = "getTherapyRecordByIdFallback")
	 public ResponseEntity<?> getTherapyRecordById(String id) {

	     Response response = new Response();

	     log.info(
	             "Fetch therapy record request received. id={}",
	             id);

	     try {

	         Optional<TherapyRecord> optional =
	                 repository.findById(id);

	         if (optional.isEmpty()) {

	             log.warn(
	                     "Therapy record not found. id={}",
	                     id);

	             response.setMessage("Therapy Record Not Found");
	             response.setStatus(HttpStatus.OK.value());
	             response.setSuccess(false);
	             response.setData(null);

	             return new ResponseEntity<>(
	                     response,
	                     HttpStatus.OK);
	         }

	         TherapyRecord entity = optional.get();

	         log.info(
	                 "Therapy record retrieved successfully. therapyRecordId={}, patientId={}, status={}",
	                 entity.getTherapyrecordid(),
	                 entity.getPatientid(),
	                 entity.getStatus());

	         response.setMessage(
	                 "Therapy Record Retrieved Successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);
	         response.setData(mapToDTO(entity));

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.OK);

	     } catch (Exception e) {

	         log.error(
	                 "Failed to fetch therapy record. id={}",
	                 id,
	                 e);

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
	 @Secured("ROLE_CUSTOMER")
	 @RateLimiter(name = "therapyRecordService", fallbackMethod = "deleteTherapyRecordFallback")
	 public ResponseEntity<?> deleteTherapyRecord(String id) {

	     Response response = new Response();

	     log.info(
	             "Delete therapy record request received. id={}",
	             id);

	     try {

	         Optional<TherapyRecord> optional =
	                 repository.findById(id);

	         if (optional.isEmpty()) {

	             log.warn(
	                     "Therapy record not found for deletion. id={}",
	                     id);

	             response.setMessage("Therapy Record Not Found");
	             response.setStatus(HttpStatus.NOT_FOUND.value());
	             response.setSuccess(false);

	             return new ResponseEntity<>(
	                     response,
	                     HttpStatus.NOT_FOUND);
	         }

	         TherapyRecord entity = optional.get();

	         log.info(
	                 "Deleting therapy record. therapyRecordId={}, patientId={}, exerciseId={}",
	                 entity.getTherapyrecordid(),
	                 entity.getPatientid(),
	                 entity.getExcerciseId());

	         repository.delete(entity);

	         log.info(
	                 "Therapy record deleted successfully. therapyRecordId={}, patientId={}, exerciseId={}",
	                 entity.getTherapyrecordid(),
	                 entity.getPatientid(),
	                 entity.getExcerciseId());

	         response.setMessage(
	                 "Therapy Record Deleted Successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.OK);

	     } catch (Exception e) {

	         log.error(
	                 "Failed to delete therapy record. id={}",
	                 id,
	                 e);

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
	 @Secured("ROLE_CUSTOMER")
	 @RateLimiter(name = "therapyRecordService", fallbackMethod = "getByClinicBranchAndPatientFallback")
	 public ResponseEntity<?> getByClinicBranchAndPatient(
	         String clinicId,
	         String branchId,
	         String patientId) {

	     Response response = new Response();

	     log.info(
	             "Fetch therapy records request received. clinicId={}, branchId={}, patientId={}",
	             clinicId,
	             branchId,
	             patientId);

	     try {

	         List<TherapyRecordDTO> records =
	                 repository.findByClincinidAndBrnchidAndPatientid(
	                                 clinicId,
	                                 branchId,
	                                 patientId)
	                         .stream()
	                         .map(this::mapToDTO)
	                         .collect(Collectors.toList());

	         if (records.isEmpty()) {

	             log.warn(
	                     "No therapy records found. clinicId={}, branchId={}, patientId={}",
	                     clinicId,
	                     branchId,
	                     patientId);

	             response.setMessage("No Therapy Records Found");
	             response.setStatus(HttpStatus.OK.value());
	             response.setSuccess(false);
	             response.setData(null);

	             return new ResponseEntity<>(response, HttpStatus.OK);
	         }

	         log.info(
	                 "Therapy records retrieved successfully. count={}, clinicId={}, branchId={}, patientId={}",
	                 records.size(),
	                 clinicId,
	                 branchId,
	                 patientId);

	         response.setMessage("Therapy Records Retrieved Successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);
	         response.setData(records);

	         return new ResponseEntity<>(response, HttpStatus.OK);

	     } catch (Exception e) {

	         log.error(
	                 "Failed to fetch therapy records. clinicId={}, branchId={}, patientId={}",
	                 clinicId,
	                 branchId,
	                 patientId,
	                 e);

	         response.setMessage(e.getMessage());
	         response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
	         response.setSuccess(false);

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.INTERNAL_SERVER_ERROR);
	     }
	 }
	 @Override
	 @Secured("ROLE_CUSTOMER")
	 @RateLimiter(name = "therapyRecordService", fallbackMethod = "getByClinicBranchPatientAndTherapyRecordIdFallback")
	 public ResponseEntity<?> getByClinicBranchPatientAndTherapyRecordId(
	         String clinicId,
	         String branchId,
	         String patientId,
	         String therapyRecordId) {

	     Response response = new Response();

	     log.info(
	             "Fetch therapy record request received. clinicId={}, branchId={}, patientId={}, therapyRecordId={}",
	             clinicId,
	             branchId,
	             patientId,
	             therapyRecordId);

	     try {

	         Optional<TherapyRecord> optional =
	                 repository.findByClincinidAndBrnchidAndPatientidAndTherapyrecordid(
	                         clinicId,
	                         branchId,
	                         patientId,
	                         therapyRecordId);

	         if (optional.isEmpty()) {

	             log.warn(
	                     "Therapy record not found. clinicId={}, branchId={}, patientId={}, therapyRecordId={}",
	                     clinicId,
	                     branchId,
	                     patientId,
	                     therapyRecordId);

	             response.setMessage("Therapy Record Not Found");
	             response.setStatus(HttpStatus.OK.value());
	             response.setSuccess(false);
	             response.setData(null);

	             return new ResponseEntity<>(response, HttpStatus.OK);
	         }

	         TherapyRecord record = optional.get();

	         log.info(
	                 "Therapy record retrieved successfully. therapyRecordId={}, patientId={}, status={}",
	                 record.getTherapyrecordid(),
	                 record.getPatientid(),
	                 record.getStatus());

	         response.setMessage("Therapy Record Retrieved Successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);
	         response.setData(mapToDTO(record));

	         return new ResponseEntity<>(response, HttpStatus.OK);

	     } catch (Exception e) {

	         log.error(
	                 "Failed to fetch therapy record. clinicId={}, branchId={}, patientId={}, therapyRecordId={}",
	                 clinicId,
	                 branchId,
	                 patientId,
	                 therapyRecordId,
	                 e);

	         response.setMessage(e.getMessage());
	         response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
	         response.setSuccess(false);

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.INTERNAL_SERVER_ERROR);
	     }
	 }
	 @Override
	 @Transactional(readOnly = true)
	 @Secured("ROLE_CUSTOMER")
	 @RateLimiter(name = "therapyRecordService", fallbackMethod = "getTherapyRecordsByClinicAndBranchAndExerciseFallback")
	 public ResponseEntity<?> getTherapyRecordsByClinicAndBranchAndExercise(
	         String clinicId,
	         String branchId,
	         String therapistid,
	         String patientid,
	         String exerciseId) {

	     Response response = new Response();

	     log.info(
	             "Fetch therapy records by exercise request received. clinicId={}, branchId={}, therapistId={}, patientId={}, exerciseId={}",
	             clinicId,
	             branchId,
	             therapistid,
	             patientid,
	             exerciseId);

	     try {

	         List<TherapyRecord> records =
	                 repository.findByClincinidAndBrnchidAndTherapyrecordidAndPatientidAndExcerciseId(
	                         clinicId,
	                         branchId,
	                         therapistid,
	                         patientid,
	                         exerciseId);

	         if (records.isEmpty()) {

	             log.warn(
	                     "No therapy records found. clinicId={}, branchId={}, therapistId={}, patientId={}, exerciseId={}",
	                     clinicId,
	                     branchId,
	                     therapistid,
	                     patientid,
	                     exerciseId);

	             response.setMessage("No therapy records found");
	             response.setStatus(HttpStatus.OK.value());
	             response.setSuccess(false);

	             return new ResponseEntity<>(response, HttpStatus.OK);
	         }

	         log.info(
	                 "Therapy records retrieved successfully. count={}, exerciseId={}",
	                 records.size(),
	                 exerciseId);

	         response.setMessage("Therapy records fetched successfully");
	         response.setStatus(HttpStatus.OK.value());
	         response.setSuccess(true);
	         response.setData(
	                 records.stream()
	                         .map(this::mapToDTO)
	                         .collect(Collectors.toList()));

	         return new ResponseEntity<>(response, HttpStatus.OK);

	     } catch (Exception e) {

	         log.error(
	                 "Failed to fetch therapy records by exercise. clinicId={}, branchId={}, therapistId={}, patientId={}, exerciseId={}",
	                 clinicId,
	                 branchId,
	                 therapistid,
	                 patientid,
	                 exerciseId,
	                 e);

	         response.setMessage(e.getMessage());
	         response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
	         response.setSuccess(false);

	         return new ResponseEntity<>(
	                 response,
	                 HttpStatus.INTERNAL_SERVER_ERROR);
	     }
	 }
	 
	 int sessioncompleted = 0;
	    String status = null;
	    private TherapyRecord mapToEntity(TherapyRecordDTO dto) {
	    	
	        List<TherophyRecordList> therapyList =
	                dto.getTherapyrecord()
	                .stream()
	                .map(this::mapTherapyList)
	                .collect(Collectors.toList());

	        return TherapyRecord.builder()
	                .therapyrecordid(dto.getTherapyrecordid())
	                .status(status)
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
	    	//sessioncompleted =  dto.getSession().intValue()-dto.getSessioncount().intValue();          	 
	     	if(dto.getSessioncount().intValue() != 0) {
	    	status = "Active";
	     	}else {
	     		status = "pending";	
	     	}}catch(Exception e) {}
	    	 try {
				 int value = dto.getSession().intValue() - dto.getSessioncount().intValue();				
				// System.out.println(value);
				 if(value!=0) {
					 status ="Active";
					 sessioncompleted = value;
				 }else {
					 status = "Completed";
					 sessioncompleted = value;
				 }}catch(Exception e) {}
	    	 return new TherophyRecordList(
	    	            dto.getSetsdone(),
	    	            dto.getRepitationdone(),
	    	            dto.getSessioncount(),
	    	            dto.getSession(),
	    	            dto.getSessioncompleted(),
	    	            dto.getDate(),
	    	            dto.getExcerciseId(),
	    	            dto.getNotes(),
	    	            (dto.getBeforeImage() != null && !dto.getBeforeImage().isBlank()) ? dto.getBeforeImage() : null,
	    	            	    (dto.getAfterImage()  != null && !dto.getAfterImage().isBlank())  ? dto.getAfterImage()  : null,
	    	            	    (dto.getBeforeVideo() != null && !dto.getBeforeVideo().isBlank()) ? dto.getBeforeVideo() : null,
	    	            	    (dto.getAfterVideo()  != null && !dto.getAfterVideo().isBlank())  ? dto.getAfterVideo()  : null
	    	    );
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

	                     // ✅ Read from record (entity), convert S3 key → signed URL
	                        dto.setBeforeImage(toSignedUrl(record.getBeforeImage()));
	                        dto.setAfterImage(toSignedUrl(record.getAfterImage()));
	                        dto.setBeforeVideo(toSignedUrl(record.getBeforeVideo()));
	                        dto.setAfterVideo(toSignedUrl(record.getAfterVideo()));
	                        return dto;

	                    }).toList();
	        }

	        return TherapyRecordDTO.builder()
	                .therapyrecordid(
	                        therapyRecord.getTherapyrecordid())
	                .clincinid(
	                        therapyRecord.getClincinid())
	                .id(therapyRecord.getId())
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
	 // ── Convert file key to signed URL, returns null if key is null ──
	    private String toSignedUrl(String fileKey) {
	        if (fileKey == null || fileKey.isBlank()) return null;
	        return s3Service.generateSignedUrl(fileKey);
	    }
	 

    // ================= RATE LIMIT FALLBACKS =================

    public ResponseEntity<?> createTherapyRecordFallback(
            TherapyRecordDTO dto, Exception ex) {
        return buildRateLimitResponse();
    }

    public ResponseEntity<?> updateTherapyRecordFallback(
            String therapyrecordid,
            String excerciseId,
            TherapyRecordDTO dto,
            Exception ex) {
        return buildRateLimitResponse();
    }

    public ResponseEntity<?> getAllTherapyRecordsFallback(Exception ex) {
        return buildRateLimitResponse();
    }

    public ResponseEntity<?> getTherapyRecordByIdFallback(
            String id, Exception ex) {
        return buildRateLimitResponse();
    }

    public ResponseEntity<?> deleteTherapyRecordFallback(
            String id, Exception ex) {
        return buildRateLimitResponse();
    }

    public ResponseEntity<?> getByClinicBranchAndPatientFallback(
            String clinicId,
            String branchId,
            String patientId,
            Exception ex) {
        return buildRateLimitResponse();
    }

    public ResponseEntity<?> getByClinicBranchPatientAndTherapyRecordIdFallback(
            String clinicId,
            String branchId,
            String patientId,
            String therapyRecordId,
            Exception ex) {
        return buildRateLimitResponse();
    }

    public ResponseEntity<?> getTherapyRecordsByClinicAndBranchAndExerciseFallback(
            String clinicId,
            String branchId,
            String therapistid,
            String patientid,
            String exerciseId,
            Exception ex) {
        return buildRateLimitResponse();
    }

    public ResponseEntity<Response> buildRateLimitResponse() {
        Response response = new Response();
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again after some time.");
        response.setStatus(429);
        return ResponseEntity.status(429).body(response);
    }

}