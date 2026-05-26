package physiotherapydoctor.service;


import org.springframework.http.ResponseEntity;

import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.TreatmentDTO;

public interface TreatmentService {
    ResponseEntity<Response> addTreatment(TreatmentDTO dto);
    ResponseEntity<Response> getAllTreatments();
    ResponseEntity<Response> getTreatmentById(String id, String hospitalId);
    ResponseEntity<Response> deleteTreatmentById(String id, String hospitalId);
    ResponseEntity<Response> updateTreatmentById(String id, String hospitalId, TreatmentDTO dto);
}


