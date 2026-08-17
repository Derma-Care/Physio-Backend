package com.chiselon.adminservice.service;

import com.chiselon.adminservice.dto.ReceptionistRequestDTO;
import com.chiselon.adminservice.util.ResponseStructure;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ReceptionistService {

    ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> createReceptionist(ReceptionistRequestDTO dto);

    ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistById(String id);

    ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getAllReceptionists();

    ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> updateReceptionist(String id, ReceptionistRequestDTO dto);

    ResponseEntity<ResponseStructure<String>> deleteReceptionist(String id);

    ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinic(String clinicId);

    ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistByClinicAndId(String clinicId, String receptionistId);

    ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinicAndBranch(String clinicId, String branchId);
}
