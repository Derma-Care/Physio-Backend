package com.clinicadmin.service.impl;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.TherapistRecordDTO;
import com.clinicadmin.entity.TherapistRecord;
import com.clinicadmin.feignclient.PhysiotherapyFeignClient;
import com.clinicadmin.repository.TherapistRecordRepository;
import com.clinicadmin.service.TherapistRecordService;

@Service
public class TherapistRecordServiceImpl implements TherapistRecordService {

    @Autowired
    private TherapistRecordRepository repository;
    
    @Autowired
    private PhysiotherapyFeignClient physiotherapyFeignClient;

    @Override
    public ResponseStructure<TherapistRecordDTO> saveRecord(TherapistRecordDTO dto) {

        // ✅ Basic validation
        if (dto == null) {
            return ResponseStructure.buildResponse(
                    null,
                    "Request body is null",
                    HttpStatus.BAD_REQUEST,
                    400
            );
        }

        TherapistRecord record = mapToEntity(dto);

        // ✅ TherapistRecord status
        record.setStatus("COMPLETED");

        // ================= ENCODE =================

        if (dto.getBeforeImage() != null) {
            record.setBeforeImage(
                    Base64.getEncoder().encodeToString(dto.getBeforeImage().getBytes())
            );
        }

        if (dto.getAfterImage() != null) {
            record.setAfterImage(
                    Base64.getEncoder().encodeToString(dto.getAfterImage().getBytes())
            );
        }

        if (dto.getBeforeVideo() != null) {
            record.setBeforeVideo(
                    Base64.getEncoder().encodeToString(dto.getBeforeVideo().getBytes())
            );
        }

        if (dto.getAfterVideo() != null) {
            record.setAfterVideo(
                    Base64.getEncoder().encodeToString(dto.getAfterVideo().getBytes())
            );
        }

        if (dto.getVoiceRecord() != null) {
            record.setVoiceRecord(
                    Base64.getEncoder().encodeToString(dto.getVoiceRecord().getBytes())
            );
        }

        // ✅ Ensure IDs
        record.setTherapistRecordId(dto.getTherapistRecordId());
        record.setSessionId(dto.getSessionId());

        // ✅ Save therapist record
        TherapistRecord saved = repository.save(record);

        // 🔥 Call Physiotherapy Service
        try {

            if (dto.getTherapistRecordId() != null
                    && !dto.getTherapistRecordId().trim().isEmpty()
                    && dto.getSessionId() != null
                    && !dto.getSessionId().trim().isEmpty()) {

                String therapistRecordId = dto.getTherapistRecordId().trim();
                String sessionId = dto.getSessionId().trim();

                System.out.println("Calling Physio API => "
                        + therapistRecordId + " | " + sessionId);

                physiotherapyFeignClient.updateSessionStatus(
                        therapistRecordId,
                        sessionId
                );

                System.out.println("Physio session status updated successfully");
            } else {
                System.out.println("TherapistRecordId or SessionId is empty");
            }

        } catch (Exception e) {
            System.out.println("Physio update failed");
            e.printStackTrace();
        }

        return ResponseStructure.buildResponse(
                mapToDTO(saved),
                "Record saved successfully",
                HttpStatus.CREATED,
                201
        );
    }
    // ================= GET =================
    @Override
    public ResponseStructure<TherapistRecordDTO> getByIds(
            String clinicId, String branchId, String therapistRecordId,String bookingId) {

        TherapistRecord record = repository
                .findByClinicIdAndBranchIdAndTherapistRecordIdAndBookingId(clinicId, branchId, therapistRecordId,bookingId)
                .orElseThrow(() -> new RuntimeException("Record not found"));

        return ResponseStructure.buildResponse(
                mapToDTO(record),
                "Record fetched successfully",
                HttpStatus.OK,
                200
        );
    }

    // ================= MAPPING =================

    private TherapistRecord mapToEntity(TherapistRecordDTO dto) {

        TherapistRecord record = new TherapistRecord();
//       record.s
        record.setClinicId(dto.getClinicId());
        record.setBranchId(dto.getBranchId());
        record.setPatientId(dto.getPatientId());
        record.setBookingId(dto.getBookingId());
        record.setTherapistId(dto.getTherapistId());

        record.setPatientName(dto.getPatientName());
        record.setTherapy(dto.getTherapy());

        record.setDate(dto.getDate());
        record.setCompletedDate(dto.getCompletedDate());
        record.setCompletedTime(dto.getCompletedTime());

        record.setDuration(dto.getDuration());
        record.setExercises(dto.getExercises());

        record.setPainBefore(dto.getPainBefore());
        record.setPainAfter(dto.getPainAfter());

        record.setTherapistNotes(dto.getTherapistNotes());
        record.setPatientResponse(dto.getPatientResponse());
        record.setSessionId(dto.getSessionId());

        record.setResult(dto.getResult());
//        record.setStatus(dto.getStatus());
        record.setMode(dto.getMode());
        record.setNextPlan(dto.getNextPlan());
        record.setRepetationDone(dto.getRepetationDone());
        record.setSetsDone(dto.getSetsDone());

        return record;
    }

    private TherapistRecordDTO mapToDTO(TherapistRecord record) {

        TherapistRecordDTO dto = new TherapistRecordDTO();

        dto.setId(record.getId());
        dto.setTherapistRecordId(record.getTherapistRecordId());
        dto.setClinicId(record.getClinicId());
        dto.setBranchId(record.getBranchId());

        dto.setPatientId(record.getPatientId());
        dto.setBookingId(record.getBookingId());
        dto.setTherapistId(record.getTherapistId());

        dto.setPatientName(record.getPatientName());
        dto.setTherapy(record.getTherapy());

        dto.setDate(record.getDate());
        dto.setCompletedDate(record.getCompletedDate());
        dto.setCompletedTime(record.getCompletedTime());

        dto.setDuration(record.getDuration());
        dto.setExercises(record.getExercises());

        dto.setPainBefore(record.getPainBefore());
        dto.setPainAfter(record.getPainAfter());

        dto.setTherapistNotes(record.getTherapistNotes());
        dto.setPatientResponse(record.getPatientResponse());
        dto.setSessionId(record.getSessionId());

        dto.setResult(record.getResult());
        dto.setStatus(record.getStatus());
        dto.setMode(record.getMode());
        dto.setNextPlan(record.getNextPlan());
//        dto.setVoiceRecord(record.getVoiceRecord());
        dto.setRepetationDone(record.getRepetationDone());
        dto.setSetsDone(record.getSetsDone());
        

        // ================= DECODE =================

        if (record.getBeforeImage() != null) {
            dto.setBeforeImage(
                    new String(Base64.getDecoder().decode(record.getBeforeImage()))
            );
        }

        if (record.getAfterImage() != null) {
            dto.setAfterImage(
                    new String(Base64.getDecoder().decode(record.getAfterImage()))
            );
        }

        if (record.getBeforeVideo() != null) {
            dto.setBeforeVideo(
                    new String(Base64.getDecoder().decode(record.getBeforeVideo()))
            );
        }

        if (record.getAfterVideo() != null) {
            dto.setAfterVideo(
                    new String(Base64.getDecoder().decode(record.getAfterVideo()))
            );
        }
        if (record.getVoiceRecord() != null) {
            dto.setVoiceRecord(
                new String(Base64.getDecoder().decode(record.getVoiceRecord()))
            );
        }

        return dto;
    }
}