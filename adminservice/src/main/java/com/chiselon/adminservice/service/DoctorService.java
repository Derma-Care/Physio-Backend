package com.chiselon.adminservice.service;

import com.chiselon.adminservice.dto.DoctorsDTO;
import com.chiselon.adminservice.util.Response;
import org.springframework.http.ResponseEntity;

public interface DoctorService {
    ResponseEntity<Response> addDoctor(DoctorsDTO dto);
    ResponseEntity<Response> getAllDoctors();
    ResponseEntity<Response> getDoctorById(String doctorId);
    ResponseEntity<Response> updateDoctorById(String doctorId, DoctorsDTO dto);
    ResponseEntity<Response> deleteDoctorById(String doctorId);
    ResponseEntity<Response> deleteDoctorsByClinic(String clinicId);
    ResponseEntity<Response> getDoctorByClinicAndDoctorId(String clinicId, String doctorId);
    ResponseEntity<Response> getDoctorsByHospitalId(String hospitalId);
    ResponseEntity<Response> getDoctorsByHospitalIdAndBranchId(String hospitalId, String branchId);
}
