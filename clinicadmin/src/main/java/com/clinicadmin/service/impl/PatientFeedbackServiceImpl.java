package com.clinicadmin.service.impl;

import com.clinicadmin.dto.*;
import com.clinicadmin.entity.*;
import com.clinicadmin.repository.PatientFeedbackRepository;
import com.clinicadmin.service.PatientFeedbackService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientFeedbackServiceImpl implements PatientFeedbackService {

    @Autowired
    private PatientFeedbackRepository repository;

    // ================= CREATE =================

    @Override
    public Response createFeedback(PatientFeedbackDTO dto) {

        PatientFeedback feedback = mapToEntity(dto);

        PatientFeedback saved = repository.save(feedback);

        Response response = new Response();

        response.setSuccess(true);
        response.setMessage("Feedback created successfully");
        response.setStatus(HttpStatus.CREATED.value());
        response.setData(mapToDTO(saved));

        return response;
    }

    // ================= GET ALL =================

    @Override
    public Response getAllFeedbacks() {

        List<PatientFeedbackDTO> list = repository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        Response response = new Response();

        response.setSuccess(true);
        response.setMessage("All feedbacks fetched successfully");
        response.setStatus(HttpStatus.OK.value());
        response.setData(list);

        return response;
    }

    // ================= GET BY ID =================

    @Override
    public Response getFeedbackById(String id) {

        PatientFeedback feedback = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));

        Response response = new Response();

        response.setSuccess(true);
        response.setMessage("Feedback fetched successfully");
        response.setStatus(HttpStatus.OK.value());
        response.setData(mapToDTO(feedback));

        return response;
    }

    // ================= UPDATE =================

    @Override
    public Response updateFeedback(String id, PatientFeedbackDTO dto) {

        PatientFeedback existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));

        // ================= BASIC DETAILS =================

        existing.setPatientId(dto.getPatientId());
        existing.setPatientName(dto.getPatientName());
        existing.setPatientPhone(dto.getPatientPhone());
        existing.setDate(dto.getDate());

        // ================= HOSPITAL FEEDBACK =================

        HospitalFeedback hospitalFeedback = new HospitalFeedback();

        hospitalFeedback.setFeedbackText(
                dto.getHospitalFeedback().getFeedbackText()
        );

        hospitalFeedback.setRating(
                dto.getHospitalFeedback().getRating()
        );

        existing.setHospitalFeedback(hospitalFeedback);

        // ================= DOCTOR FEEDBACK =================

        DoctorFeedback doctorFeedback = new DoctorFeedback();

        doctorFeedback.setTargetId(
                dto.getDoctorFeedback().getTargetId()
        );

        doctorFeedback.setFeedbackText(
                dto.getDoctorFeedback().getFeedbackText()
        );

        doctorFeedback.setRating(
                dto.getDoctorFeedback().getRating()
        );

        existing.setDoctorFeedback(doctorFeedback);

        // ================= RECEPTIONIST FEEDBACK =================

        ReceptionistFeedback receptionistFeedback =
                new ReceptionistFeedback();

        receptionistFeedback.setTargetId(
                dto.getReceptionistFeedback().getTargetId()
        );

        receptionistFeedback.setFeedbackText(
                dto.getReceptionistFeedback().getFeedbackText()
        );

        receptionistFeedback.setRating(
                dto.getReceptionistFeedback().getRating()
        );

        existing.setReceptionistFeedback(receptionistFeedback);

        // ================= THERAPIST FEEDBACK =================

        TherapistFeedback therapistFeedback =
                new TherapistFeedback();

        therapistFeedback.setTargetId(
                dto.getTherapistFeedback().getTargetId()
        );

        therapistFeedback.setFeedbackText(
                dto.getTherapistFeedback().getFeedbackText()
        );

        therapistFeedback.setRating(
                dto.getTherapistFeedback().getRating()
        );

        existing.setTherapistFeedback(therapistFeedback);

        // ================= SAVE =================

        PatientFeedback updated = repository.save(existing);

        Response response = new Response();

        response.setSuccess(true);
        response.setMessage("Feedback updated successfully");
        response.setStatus(HttpStatus.OK.value());
        response.setData(mapToDTO(updated));

        return response;
    }

    // ================= DELETE =================

    @Override
    public Response deleteFeedback(String id) {

        PatientFeedback feedback = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));

        repository.delete(feedback);

        Response response = new Response();

        response.setSuccess(true);
        response.setMessage("Feedback deleted successfully");
        response.setStatus(HttpStatus.OK.value());
        response.setData("Deleted Successfully");

        return response;
    }

    // ================= MAP ENTITY TO DTO =================

    private PatientFeedbackDTO mapToDTO(PatientFeedback feedback) {

        if (feedback == null) {
            return null;
        }

        PatientFeedbackDTO dto = new PatientFeedbackDTO();

        dto.setId(feedback.getId());
        dto.setPatientId(feedback.getPatientId());
        dto.setPatientName(feedback.getPatientName());
        dto.setPatientPhone(feedback.getPatientPhone());
        dto.setDate(feedback.getDate());

        dto.setHospitalFeedback(
                mapHospitalToDTO(feedback.getHospitalFeedback())
        );

        dto.setDoctorFeedback(
                mapDoctorToDTO(feedback.getDoctorFeedback())
        );

        dto.setReceptionistFeedback(
                mapReceptionistToDTO(feedback.getReceptionistFeedback())
        );

        dto.setTherapistFeedback(
                mapTherapistToDTO(feedback.getTherapistFeedback())
        );

        return dto;
    }

    // ================= MAP DTO TO ENTITY =================

    private PatientFeedback mapToEntity(PatientFeedbackDTO dto) {

        if (dto == null) {
            return null;
        }

        PatientFeedback feedback = new PatientFeedback();

        feedback.setId(dto.getId());
        feedback.setPatientId(dto.getPatientId());
        feedback.setPatientName(dto.getPatientName());
        feedback.setPatientPhone(dto.getPatientPhone());
        feedback.setDate(dto.getDate());

        feedback.setHospitalFeedback(
                mapHospitalToEntity(dto.getHospitalFeedback())
        );

        feedback.setDoctorFeedback(
                mapDoctorToEntity(dto.getDoctorFeedback())
        );

        feedback.setReceptionistFeedback(
                mapReceptionistToEntity(dto.getReceptionistFeedback())
        );

        feedback.setTherapistFeedback(
                mapTherapistToEntity(dto.getTherapistFeedback())
        );

        return feedback;
    }

    // ================= UPDATE ENTITY =================

    private void updateEntity(PatientFeedback feedback,
                              PatientFeedbackDTO dto) {

        feedback.setPatientId(dto.getPatientId());
        feedback.setPatientName(dto.getPatientName());
        feedback.setPatientPhone(dto.getPatientPhone());
        feedback.setDate(dto.getDate());

        feedback.setHospitalFeedback(
                mapHospitalToEntity(dto.getHospitalFeedback())
        );

        feedback.setDoctorFeedback(
                mapDoctorToEntity(dto.getDoctorFeedback())
        );

        feedback.setReceptionistFeedback(
                mapReceptionistToEntity(dto.getReceptionistFeedback())
        );

        feedback.setTherapistFeedback(
                mapTherapistToEntity(dto.getTherapistFeedback())
        );
    }

    // ================= HOSPITAL =================

    private HospitalFeedbackDTO mapHospitalToDTO(HospitalFeedback entity) {

        if (entity == null) {
            return null;
        }

        HospitalFeedbackDTO dto = new HospitalFeedbackDTO();

        dto.setFeedbackText(entity.getFeedbackText());
        dto.setRating(entity.getRating());

        return dto;
    }

    private HospitalFeedback mapHospitalToEntity(HospitalFeedbackDTO dto) {

        if (dto == null) {
            return null;
        }

        HospitalFeedback entity = new HospitalFeedback();

        entity.setFeedbackText(dto.getFeedbackText());
        entity.setRating(dto.getRating());

        return entity;
    }

    // ================= DOCTOR =================

    private DoctorFeedbackDTO mapDoctorToDTO(DoctorFeedback entity) {

        if (entity == null) {
            return null;
        }

        DoctorFeedbackDTO dto = new DoctorFeedbackDTO();

        dto.setTargetId(entity.getTargetId());
        dto.setFeedbackText(entity.getFeedbackText());
        dto.setRating(entity.getRating());

        return dto;
    }

    private DoctorFeedback mapDoctorToEntity(DoctorFeedbackDTO dto) {

        if (dto == null) {
            return null;
        }

        DoctorFeedback entity = new DoctorFeedback();

        entity.setTargetId(dto.getTargetId());
        entity.setFeedbackText(dto.getFeedbackText());
        entity.setRating(dto.getRating());

        return entity;
    }

    // ================= RECEPTIONIST =================

    private ReceptionistFeedbackDTO mapReceptionistToDTO(
            ReceptionistFeedback entity) {

        if (entity == null) {
            return null;
        }

        ReceptionistFeedbackDTO dto = new ReceptionistFeedbackDTO();

        dto.setTargetId(entity.getTargetId());
        dto.setFeedbackText(entity.getFeedbackText());
        dto.setRating(entity.getRating());

        return dto;
    }

    private ReceptionistFeedback mapReceptionistToEntity(
            ReceptionistFeedbackDTO dto) {

        if (dto == null) {
            return null;
        }

        ReceptionistFeedback entity = new ReceptionistFeedback();

        entity.setTargetId(dto.getTargetId());
        entity.setFeedbackText(dto.getFeedbackText());
        entity.setRating(dto.getRating());

        return entity;
    }

    // ================= THERAPIST =================

    private TherapistFeedbackDTO mapTherapistToDTO(
            TherapistFeedback entity) {

        if (entity == null) {
            return null;
        }

        TherapistFeedbackDTO dto = new TherapistFeedbackDTO();

        dto.setTargetId(entity.getTargetId());
        dto.setFeedbackText(entity.getFeedbackText());
        dto.setRating(entity.getRating());

        return dto;
    }

    private TherapistFeedback mapTherapistToEntity(
            TherapistFeedbackDTO dto) {

        if (dto == null) {
            return null;
        }

        TherapistFeedback entity = new TherapistFeedback();

        entity.setTargetId(dto.getTargetId());
        entity.setFeedbackText(dto.getFeedbackText());
        entity.setRating(dto.getRating());

        return entity;
    }
}