package com.dermacare.doctorservice.service;

import org.springframework.http.ResponseEntity;

import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.dto.TreatmentDTO;

public interface TreatmentService {
    ResponseEntity<Response> addTreatment(TreatmentDTO dto);
    ResponseEntity<Response> getAllTreatments();
    ResponseEntity<Response> getTreatmentById(String id, String hospitalId);
    ResponseEntity<Response> deleteTreatmentById(String id, String hospitalId);
    ResponseEntity<Response> updateTreatmentById(String id, String hospitalId, TreatmentDTO dto);
}

