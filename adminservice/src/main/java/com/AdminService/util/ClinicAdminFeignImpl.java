
package com.AdminService.util;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.AdminService.dto.AdministratorDTO;
import com.AdminService.dto.DoctorAvailabilityStatusDTO;
import com.AdminService.dto.DoctorSlotDTO;
import com.AdminService.dto.DoctorsDTO;
import com.AdminService.dto.LabTechnicianRequestDTO;
import com.AdminService.dto.LabTestDTO;
import com.AdminService.dto.NurseDTO;
import com.AdminService.dto.PharmacistDTO;
import com.AdminService.dto.ProbableDiagnosisDTO;
import com.AdminService.dto.ReceptionistRequestDTO;
import com.AdminService.dto.SecurityStaffDTO;
import com.AdminService.dto.TreatmentDTO;
import com.AdminService.dto.UpdateSlotRequestDTO;
import com.AdminService.dto.WardBoyDTO;
import com.AdminService.feign.ClinicAdminFeign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicAdminFeignImpl {

    private final ClinicAdminFeign clinicAdminFeign;
    
    @CircuitBreaker(name="clincAdminService", fallbackMethod="addDoctorFallback")
    @Retry(name = "clincAdminService", fallbackMethod="addDoctorFallback")
    public ResponseEntity<Response> addDoctor(String token, DoctorsDTO dto) {
        return clinicAdminFeign.addDoctor(token, dto);
    }

    public ResponseEntity<Response> addDoctorFallback(String token, DoctorsDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getAllDoctorsFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllDoctorsFallback")
    public ResponseEntity<Response> getAllDoctors(String token) {
        return clinicAdminFeign.getAllDoctors(token);
    }

    public ResponseEntity<Response> getAllDoctorsFallback(String token, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getDoctorByIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getDoctorByIdFallback")
    public ResponseEntity<Response> getDoctorById(String token, String id) {
        return clinicAdminFeign.getDoctorById(token, id);
    }

    public ResponseEntity<Response> getDoctorByIdFallback(String token, String id, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="updateDoctorByIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateDoctorByIdFallback")
    public ResponseEntity<Response> updateDoctorById(String token, String doctorId, DoctorsDTO dto) {
        return clinicAdminFeign.updateDoctorById(token, doctorId, dto);
    }

    public ResponseEntity<Response> updateDoctorByIdFallback(String token, String doctorId, DoctorsDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="deleteDoctorByIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteDoctorByIdFallback")
    public ResponseEntity<Response> deleteDoctorById(String token, String doctorId) {
        return clinicAdminFeign.deleteDoctorById(token, doctorId);
    }

    public ResponseEntity<Response> deleteDoctorByIdFallback(String token, String doctorId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="deleteDoctorsByClinicFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteDoctorsByClinicFallback")
    public ResponseEntity<Response> deleteDoctorsByClinic(String token, String clinicId) {
        return clinicAdminFeign.deleteDoctorsByClinic(token, clinicId);
    }

    public ResponseEntity<Response> deleteDoctorsByClinicFallback(String token, String clinicId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getDoctorByClinicAndDoctorIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getDoctorByClinicAndDoctorIdFallback")
    public ResponseEntity<Response> getDoctorByClinicAndDoctorId(String token, String clinicId, String doctorId) {
        return clinicAdminFeign.getDoctorByClinicAndDoctorId(token, clinicId, doctorId);
    }

    public ResponseEntity<Response> getDoctorByClinicAndDoctorIdFallback(String token, String clinicId, String doctorId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getDoctorsByHospitalIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getDoctorsByHospitalIdFallback")
    public ResponseEntity<Response> getDoctorsByHospitalId(String token, String hospitalId) {
        return clinicAdminFeign.getDoctorsByHospitalId(token, hospitalId);
    }

    public ResponseEntity<Response> getDoctorsByHospitalIdFallback(String token, String hospitalId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getDoctorsByHospitalIdAndBranchIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getDoctorsByHospitalIdAndBranchIdFallback")
    public ResponseEntity<Response> getDoctorsByHospitalIdAndBranchId(String token, String hospitalId, String branchId) {
        return clinicAdminFeign.getDoctorsByHospitalIdAndBranchId(token, hospitalId, branchId);
    }

    public ResponseEntity<Response> getDoctorsByHospitalIdAndBranchIdFallback(String token, String hospitalId, String branchId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="addDiseasesFallback")
    @Retry(name = "clincAdminService", fallbackMethod="addDiseasesFallback")
    public ResponseEntity<Response> addDiseases(String token, Object requestBody) {
        return clinicAdminFeign.addDiseases(token, requestBody);
    }

    public ResponseEntity<Response> addDiseasesFallback(String token, Object requestBody, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getAllDiseasesFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllDiseasesFallback")
    public ResponseEntity<Response> getAllDiseases(String token) {
        return clinicAdminFeign.getAllDiseases(token);
    }

    public ResponseEntity<Response> getAllDiseasesFallback(String token, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getDiseaseByDiseaseIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getDiseaseByDiseaseIdFallback")
    public ResponseEntity<Response> getDiseaseByDiseaseId(String id, String hospitalId) {
        return clinicAdminFeign.getDiseaseByDiseaseId(id, hospitalId);
    }

    public ResponseEntity<Response> getDiseaseByDiseaseIdFallback(String id, String hospitalId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="deleteDiseaseByDiseaseIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteDiseaseByDiseaseIdFallback")
    public ResponseEntity<Response> deleteDiseaseByDiseaseId(String id, String hospitalId) {
        return clinicAdminFeign.deleteDiseaseByDiseaseId(id, hospitalId);
    }

    public ResponseEntity<Response> deleteDiseaseByDiseaseIdFallback(String id, String hospitalId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="updateDiseaseByDiseaseIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateDiseaseByDiseaseIdFallback")
    public ResponseEntity<Response> updateDiseaseByDiseaseId(String id, String hospitalId, ProbableDiagnosisDTO dto) {
        return clinicAdminFeign.updateDiseaseByDiseaseId(id, hospitalId, dto);
    }

    public ResponseEntity<Response> updateDiseaseByDiseaseIdFallback(String id, String hospitalId, ProbableDiagnosisDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getDiseasesByHospitalIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getDiseasesByHospitalIdFallback")
    public ResponseEntity<ResponseStructure<List<ProbableDiagnosisDTO>>> getDiseasesByHospitalId(String hospitalId) {
        return clinicAdminFeign.getDiseasesByHospitalId(hospitalId);
    }

    public ResponseEntity<ResponseStructure<List<ProbableDiagnosisDTO>>> getDiseasesByHospitalIdFallback(String hospitalId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="addLabTestFallback")
    @Retry(name = "clincAdminService", fallbackMethod="addLabTestFallback")
    public ResponseEntity<Response> addLabTest(LabTestDTO dto) {
        return clinicAdminFeign.addLabTest(dto);
    }

    public ResponseEntity<Response> addLabTestFallback(LabTestDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getAllLabTestsFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllLabTestsFallback")
    public ResponseEntity<Response> getAllLabTests() {
        return clinicAdminFeign.getAllLabTests();
    }

    public ResponseEntity<Response> getAllLabTestsFallback(Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getLabTestByIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getLabTestByIdFallback")
    public ResponseEntity<Response> getLabTestById(String id, String hospitalId) {
        return clinicAdminFeign.getLabTestById(id, hospitalId);
    }

    public ResponseEntity<Response> getLabTestByIdFallback(String id, String hospitalId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="deleteLabTestFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteLabTestFallback")
    public ResponseEntity<Response> deleteLabTest(String id, String hospitalId) {
        return clinicAdminFeign.deleteLabTest(id, hospitalId);
    }

    public ResponseEntity<Response> deleteLabTestFallback(String id, String hospitalId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="updateLabTestFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateLabTestFallback")
    public ResponseEntity<Response> updateLabTest(String id, String hospitalId, LabTestDTO dto) {
        return clinicAdminFeign.updateLabTest(id, hospitalId, dto);
    }

    public ResponseEntity<Response> updateLabTestFallback(String id, String hospitalId, LabTestDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getLabTestsByHospitalIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getLabTestsByHospitalIdFallback")
    public ResponseEntity<ResponseStructure<List<LabTestDTO>>> getLabTestsByHospitalId(String hospitalId) {
        return clinicAdminFeign.getLabTestsByHospitalId(hospitalId);
    }

    public ResponseEntity<ResponseStructure<List<LabTestDTO>>> getLabTestsByHospitalIdFallback(String hospitalId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="addTreatmentFallback")
    @Retry(name = "clincAdminService", fallbackMethod="addTreatmentFallback")
    public ResponseEntity<Response> addTreatment(TreatmentDTO dto) {
        return clinicAdminFeign.addTreatment(dto);
    }

    public ResponseEntity<Response> addTreatmentFallback(TreatmentDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getAllTreatmentsFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllTreatmentsFallback")
    public ResponseEntity<Response> getAllTreatments() {
        return clinicAdminFeign.getAllTreatments();
    }

    public ResponseEntity<Response> getAllTreatmentsFallback(Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getTreatmentByIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getTreatmentByIdFallback")
    public ResponseEntity<Response> getTreatmentById(String id, String hospitalId) {
        return clinicAdminFeign.getTreatmentById(id, hospitalId);
    }

    public ResponseEntity<Response> getTreatmentByIdFallback(String id, String hospitalId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="deleteTreatmentByIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteTreatmentByIdFallback")
    public ResponseEntity<Response> deleteTreatmentById(String id, String hospitalId) {
        return clinicAdminFeign.deleteTreatmentById(id, hospitalId);
    }

    public ResponseEntity<Response> deleteTreatmentByIdFallback(String id, String hospitalId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="updateTreatmentByIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateTreatmentByIdFallback")
    public ResponseEntity<Response> updateTreatmentById(String id, String hospitalId, TreatmentDTO dto) {
        return clinicAdminFeign.updateTreatmentById(id, hospitalId, dto);
    }

    public ResponseEntity<Response> updateTreatmentByIdFallback(String id, String hospitalId, TreatmentDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="getTreatmentsByHospitalIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getTreatmentsByHospitalIdFallback")
    public ResponseEntity<ResponseStructure<List<TreatmentDTO>>> getTreatmentsByHospitalId(String hospitalId) {
        return clinicAdminFeign.getTreatmentsByHospitalId(hospitalId);
    }

    public ResponseEntity<ResponseStructure<List<TreatmentDTO>>> getTreatmentsByHospitalIdFallback(String hospitalId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name="clincAdminService", fallbackMethod="addDoctorSlotFallback")
    @Retry(name = "clincAdminService", fallbackMethod="addDoctorSlotFallback")
    public ResponseEntity<Response> addDoctorSlot(String token, String hospitalId, String branchId, String doctorId, DoctorSlotDTO slotDto) {
        return clinicAdminFeign.addDoctorSlot(token, hospitalId, branchId, doctorId, slotDto);
    }

    public ResponseEntity<Response> addDoctorSlotFallback(String token, String hospitalId, String branchId, String doctorId, DoctorSlotDTO slotDto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getDoctorSlotsFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getDoctorSlotsFallback")
    public ResponseEntity<Response> getDoctorSlots(String token, String hospitalId, String branchId, String doctorId) {
        return clinicAdminFeign.getDoctorSlots(token, hospitalId, branchId, doctorId);
    }

    public ResponseEntity<Response> getDoctorSlotsFallback(String token, String hospitalId, String branchId, String doctorId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="updateDoctorSlotFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateDoctorSlotFallback")
    public ResponseEntity<Response> updateDoctorSlot(String token, UpdateSlotRequestDTO request) {
        return clinicAdminFeign.updateDoctorSlot(token, request);
    }

    public ResponseEntity<Response> updateDoctorSlotFallback(String token, UpdateSlotRequestDTO request, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="deleteDoctorSlotFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteDoctorSlotFallback")
    public Response deleteDoctorSlot(String token, String doctorId, String date, String slot) {
        return clinicAdminFeign.deleteDoctorSlot(token, doctorId, date, slot);
    }

    
    @CircuitBreaker(name = "clincAdminService", fallbackMethod = "deleteDoctorSlotFallback")
    @Retry(name = "clincAdminService", fallbackMethod = "deleteDoctorSlotsBasedOnIdsFallback")
    public ResponseEntity<Response> deleteDoctorSlot(String token,
                                     String doctorId,
                                     String branchId,
                                     String date,
                                     String slot) {
     return clinicAdminFeign.deleteDoctorSlot(
                token,
                doctorId,
                branchId,
                date,
                slot);
    }
    
    public Response deleteDoctorSlotsBasedOnIdsFallback(String token,
            String doctorId,
            String branchId,
            String date,
            String slot,
            Exception ex) {

Response response = new Response();
response.setStatus(429);
response.setMessage("Clinic Admin Service is currently unavailable. Unable to delete doctor slot. Error: "
+ ex.getMessage());

return response;
}
    
    public Response deleteDoctorSlotFallback(String token, String doctorId, String date, String slot, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="deleteDoctorSlotsByDateFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteDoctorSlotsByDateFallback")
    public ResponseEntity<Response> deleteDoctorSlotsByDate(String token, String doctorId, String branchId, String date) {
        return clinicAdminFeign.deleteDoctorSlotsByDate(token, doctorId, branchId, date);
    }

    public ResponseEntity<Response> deleteDoctorSlotsByDateFallback(String token, String doctorId, String branchId, String date, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="updateDoctorSlotWhileBookingFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateDoctorSlotWhileBookingFallback")
    public boolean updateDoctorSlotWhileBooking(String token, String doctorId, String branchId, String date, String time) {
        return clinicAdminFeign.updateDoctorSlotWhileBooking(token, doctorId, branchId, date, time);
    }

    public boolean updateDoctorSlotWhileBookingFallback(String token, String doctorId, String branchId, String date, String time, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="makingFalseDoctorSlotFallback")
    @Retry(name = "clincAdminService", fallbackMethod="makingFalseDoctorSlotFallback")
    public boolean makingFalseDoctorSlot(String token, String doctorId, String branchId, String date, String time) {
        return clinicAdminFeign.makingFalseDoctorSlot(token, doctorId, branchId, date, time);
    }

    public boolean makingFalseDoctorSlotFallback(String token, String doctorId, String branchId, String date, String time, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="generateSlotsFallback")
    @Retry(name = "clincAdminService", fallbackMethod="generateSlotsFallback")
    public Response generateSlots(String token, String doctorId, String branchId, String date, int intervalMinutes, String openingTime, String closingTime) {
        return clinicAdminFeign.generateSlots(token, doctorId, branchId, date, intervalMinutes, openingTime, closingTime);
    }

    public Response generateSlotsFallback(String token, String doctorId, String branchId, String date, int intervalMinutes, String openingTime, String closingTime, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getDoctorSlotFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getDoctorSlotFallback")
    public ResponseEntity<Response> getDoctorSlot(String hospitalId, String branchId, String doctorId) {
        return clinicAdminFeign.getDoctorSlot(hospitalId, branchId, doctorId);
    }

    public ResponseEntity<Response> getDoctorSlotFallback(String hospitalId, String branchId, String doctorId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="createLabTechnicianFallback")
    @Retry(name = "clincAdminService", fallbackMethod="createLabTechnicianFallback")
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> createLabTechnician(LabTechnicianRequestDTO dto) {
        return clinicAdminFeign.createLabTechnician(dto);
    }

    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> createLabTechnicianFallback(LabTechnicianRequestDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getAllLabTechniciansFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllLabTechniciansFallback")
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getAllLabTechnicians() {
        return clinicAdminFeign.getAllLabTechnicians();
    }

    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getAllLabTechniciansFallback(Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="updateLabTechnicianFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateLabTechnicianFallback")
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> updateLabTechnician(String id, LabTechnicianRequestDTO dto) {
        return clinicAdminFeign.updateLabTechnician(id, dto);
    }

    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> updateLabTechnicianFallback(String id, LabTechnicianRequestDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="deleteLabTechnicianFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteLabTechnicianFallback")
    public ResponseEntity<ResponseStructure<String>> deleteLabTechnician(String id) {
        return clinicAdminFeign.deleteLabTechnician(id);
    }

    public ResponseEntity<ResponseStructure<String>> deleteLabTechnicianFallback(String id, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getLabTechniciansByClinicFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getLabTechniciansByClinicFallback")
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinic(String clinicId) {
        return clinicAdminFeign.getLabTechniciansByClinic(clinicId);
    }

    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinicFallback(String clinicId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getLabTechnicianByClinicAndIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getLabTechnicianByClinicAndIdFallback")
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> getLabTechnicianByClinicAndId(String clinicId, String technicianId) {
        return clinicAdminFeign.getLabTechnicianByClinicAndId(clinicId, technicianId);
    }

    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> getLabTechnicianByClinicAndIdFallback(String clinicId, String technicianId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getLabTechniciansByClinicAndBranchFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getLabTechniciansByClinicAndBranchFallback")
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinicAndBranch(String clinicId, String branchId) {
        return clinicAdminFeign.getLabTechniciansByClinicAndBranch(clinicId, branchId);
    }

    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinicAndBranchFallback(String clinicId, String branchId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="createReceptionistFallback")
    @Retry(name = "clincAdminService", fallbackMethod="createReceptionistFallback")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> createReceptionist(String token, ReceptionistRequestDTO dto) {
        return clinicAdminFeign.createReceptionist(token, dto);
    }

    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> createReceptionistFallback(String token, ReceptionistRequestDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getReceptionistByIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getReceptionistByIdFallback")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistById(String token, String id) {
        return clinicAdminFeign.getReceptionistById(token, id);
    }

    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistByIdFallback(String token, String id, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getAllReceptionistsFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllReceptionistsFallback")
    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getAllReceptionists(String token) {
        return clinicAdminFeign.getAllReceptionists(token);
    }

    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getAllReceptionistsFallback(String token, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="updateReceptionistFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateReceptionistFallback")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> updateReceptionist(String token, String id, ReceptionistRequestDTO dto) {
        return clinicAdminFeign.updateReceptionist(token, id, dto);
    }

    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> updateReceptionistFallback(String token, String id, ReceptionistRequestDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="deleteReceptionistFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteReceptionistFallback")
    public ResponseEntity<ResponseStructure<String>> deleteReceptionist(String token, String id) {
        return clinicAdminFeign.deleteReceptionist(token, id);
    }

    public ResponseEntity<ResponseStructure<String>> deleteReceptionistFallback(String token, String id, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getReceptionistsByClinicFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getReceptionistsByClinicFallback")
    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinic(String token, String clinicId) {
        return clinicAdminFeign.getReceptionistsByClinic(token, clinicId);
    }

    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinicFallback(String token, String clinicId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getReceptionistByClinicAndIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getReceptionistByClinicAndIdFallback")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistByClinicAndId(String token, String clinicId, String receptionistId) {
        return clinicAdminFeign.getReceptionistByClinicAndId(token, clinicId, receptionistId);
    }

    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistByClinicAndIdFallback(String token, String clinicId, String receptionistId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getReceptionistsByClinicAndBranchFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getReceptionistsByClinicAndBranchFallback")
    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinicAndBranch(String token, String clinicId, String branchId) {
        return clinicAdminFeign.getReceptionistsByClinicAndBranch(token, clinicId, branchId);
    }

    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinicAndBranchFallback(String token, String clinicId, String branchId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="nurseOnBoardingFallback")
    @Retry(name = "clincAdminService", fallbackMethod="nurseOnBoardingFallback")
    public ResponseEntity<Response> nurseOnBoarding(NurseDTO dto) {
        return clinicAdminFeign.nurseOnBoarding(dto);
    }

    public ResponseEntity<Response> nurseOnBoardingFallback(NurseDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getAllByHospitalFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllByHospitalFallback")
    public ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllByHospital(String hospitalId) {
        return clinicAdminFeign.getAllByHospital(hospitalId);
    }

    public ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllByHospitalFallback(String hospitalId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getNurseFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getNurseFallback")
    public ResponseEntity<ResponseStructure<NurseDTO>> getNurse(String hospitalId, String nurseId) {
        return clinicAdminFeign.getNurse(hospitalId, nurseId);
    }

    public ResponseEntity<ResponseStructure<NurseDTO>> getNurseFallback(String hospitalId, String nurseId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="updateNurseFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateNurseFallback")
    public ResponseEntity<ResponseStructure<NurseDTO>> updateNurse(String nurseId, NurseDTO dto) {
        return clinicAdminFeign.updateNurse(nurseId, dto);
    }

    public ResponseEntity<ResponseStructure<NurseDTO>> updateNurseFallback(String nurseId, NurseDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="deleteNurseFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteNurseFallback")
    public ResponseEntity<ResponseStructure<String>> deleteNurse(String hospitalId, String nurseId) {
        return clinicAdminFeign.deleteNurse(hospitalId, nurseId);
    }

    public ResponseEntity<ResponseStructure<String>> deleteNurseFallback(String hospitalId, String nurseId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getAllNursesByBranchFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllNursesByBranchFallback")
    public ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllNursesByBranch(String hospitalId, String branchId) {
        return clinicAdminFeign.getAllNursesByBranch(hospitalId, branchId);
    }

    public ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllNursesByBranchFallback(String hospitalId, String branchId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="pharmacistOnBoardingFallback")
    @Retry(name = "clincAdminService", fallbackMethod="pharmacistOnBoardingFallback")
    public ResponseEntity<ResponseStructure<PharmacistDTO>> pharmacistOnBoarding(PharmacistDTO dto) {
        return clinicAdminFeign.pharmacistOnBoarding(dto);
    }

    public ResponseEntity<ResponseStructure<PharmacistDTO>> pharmacistOnBoardingFallback(PharmacistDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getAllByDepartmentFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllByDepartmentFallback")
    public ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getAllByDepartment(String hospitalId) {
        return clinicAdminFeign.getAllByDepartment(hospitalId);
    }

    public ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getAllByDepartmentFallback(String hospitalId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getPharmacistFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getPharmacistFallback")
    public ResponseEntity<ResponseStructure<PharmacistDTO>> getPharmacist(String pharmacistId) {
        return clinicAdminFeign.getPharmacist(pharmacistId);
    }

    public ResponseEntity<ResponseStructure<PharmacistDTO>> getPharmacistFallback(String pharmacistId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="updatePharmacistFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updatePharmacistFallback")
    public ResponseEntity<ResponseStructure<PharmacistDTO>> updatePharmacist(String pharmacistId, PharmacistDTO dto) {
        return clinicAdminFeign.updatePharmacist(pharmacistId, dto);
    }

    public ResponseEntity<ResponseStructure<PharmacistDTO>> updatePharmacistFallback(String pharmacistId, PharmacistDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="deletePharmacistFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deletePharmacistFallback")
    public ResponseEntity<ResponseStructure<String>> deletePharmacist(String pharmacistId) {
        return clinicAdminFeign.deletePharmacist(pharmacistId);
    }

    public ResponseEntity<ResponseStructure<String>> deletePharmacistFallback(String pharmacistId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getPharmacistsByHospitalIdAndBranchIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getPharmacistsByHospitalIdAndBranchIdFallback")
    public ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getPharmacistsByHospitalIdAndBranchId(String hospitalId, String branchId) {
        return clinicAdminFeign.getPharmacistsByHospitalIdAndBranchId(hospitalId, branchId);
    }

    public ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getPharmacistsByHospitalIdAndBranchIdFallback(String hospitalId, String branchId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="addSecurityStaffFallback")
    @Retry(name = "clincAdminService", fallbackMethod="addSecurityStaffFallback")
    public ResponseStructure<SecurityStaffDTO> addSecurityStaff(String token, SecurityStaffDTO dto) {
        return clinicAdminFeign.addSecurityStaff(token, dto);
    }

    public ResponseStructure<SecurityStaffDTO> addSecurityStaffFallback(String token, SecurityStaffDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="updateSecurityStaffFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateSecurityStaffFallback")
    public ResponseStructure<SecurityStaffDTO> updateSecurityStaff(String token, String staffId, SecurityStaffDTO staffRequest) {
        return clinicAdminFeign.updateSecurityStaff(token, staffId, staffRequest);
    }

    public ResponseStructure<SecurityStaffDTO> updateSecurityStaffFallback(String token, String staffId, SecurityStaffDTO staffRequest, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getSecurityStaffByIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getSecurityStaffByIdFallback")
    public ResponseStructure<SecurityStaffDTO> getSecurityStaffById(String token, String staffId) {
        return clinicAdminFeign.getSecurityStaffById(token, staffId);
    }

    public ResponseStructure<SecurityStaffDTO> getSecurityStaffByIdFallback(String token, String staffId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getAllByClinicIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllByClinicIdFallback")
    public ResponseStructure<List<SecurityStaffDTO>> getAllByClinicId(String token, String clinicId) {
        return clinicAdminFeign.getAllByClinicId(token, clinicId);
    }

    public ResponseStructure<List<SecurityStaffDTO>> getAllByClinicIdFallback(String token, String clinicId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="deleteSecurityStaffFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteSecurityStaffFallback")
    public ResponseStructure<String> deleteSecurityStaff(String token, String staffId) {
        return clinicAdminFeign.deleteSecurityStaff(token, staffId);
    }

    public ResponseStructure<String> deleteSecurityStaffFallback(String token, String staffId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getSecurityStaffByClinicIdAndBranchIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getSecurityStaffByClinicIdAndBranchIdFallback")
    public ResponseStructure<List<SecurityStaffDTO>> getSecurityStaffByClinicIdAndBranchId(String token, String clinicId, String branchId) {
        return clinicAdminFeign.getSecurityStaffByClinicIdAndBranchId(token, clinicId, branchId);
    }

    public ResponseStructure<List<SecurityStaffDTO>> getSecurityStaffByClinicIdAndBranchIdFallback(String token, String clinicId, String branchId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="addWardBoyFallback")
    @Retry(name = "clincAdminService", fallbackMethod="addWardBoyFallback")
    public ResponseStructure<WardBoyDTO> addWardBoy(String token, WardBoyDTO dto) {
        return clinicAdminFeign.addWardBoy(token, dto);
    }

    public ResponseStructure<WardBoyDTO> addWardBoyFallback(String token, WardBoyDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="updateWardBoyFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateWardBoyFallback")
    public ResponseStructure<WardBoyDTO> updateWardBoy(String token, String id, WardBoyDTO dto) {
        return clinicAdminFeign.updateWardBoy(token, id, dto);
    }

    public ResponseStructure<WardBoyDTO> updateWardBoyFallback(String token, String id, WardBoyDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getWardBoyByIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getWardBoyByIdFallback")
    public ResponseStructure<WardBoyDTO> getWardBoyById(String token, String id) {
        return clinicAdminFeign.getWardBoyById(token, id);
    }

    public ResponseStructure<WardBoyDTO> getWardBoyByIdFallback(String token, String id, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getAllWardBoysFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllWardBoysFallback")
    public ResponseStructure<List<WardBoyDTO>> getAllWardBoys(String token) {
        return clinicAdminFeign.getAllWardBoys(token);
    }

    public ResponseStructure<List<WardBoyDTO>> getAllWardBoysFallback(String token, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getWardBoysByClinicIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getWardBoysByClinicIdFallback")
    public ResponseStructure<List<WardBoyDTO>> getWardBoysByClinicId(String token, String clinicId) {
        return clinicAdminFeign.getWardBoysByClinicId(token, clinicId);
    }

    public ResponseStructure<List<WardBoyDTO>> getWardBoysByClinicIdFallback(String token, String clinicId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getWardBoyByIdAndClinicIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getWardBoyByIdAndClinicIdFallback")
    public ResponseStructure<WardBoyDTO> getWardBoyByIdAndClinicId(String token, String wardBoyId, String clinicId) {
        return clinicAdminFeign.getWardBoyByIdAndClinicId(token, wardBoyId, clinicId);
    }

    public ResponseStructure<WardBoyDTO> getWardBoyByIdAndClinicIdFallback(String token, String wardBoyId, String clinicId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="deleteWardBoyFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteWardBoyFallback")
    public ResponseStructure<Void> deleteWardBoy(String token, String id) {
        return clinicAdminFeign.deleteWardBoy(token, id);
    }

    public ResponseStructure<Void> deleteWardBoyFallback(String token, String id, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getWardBoysByClinicIdAndBranchIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getWardBoysByClinicIdAndBranchIdFallback")
    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicIdAndBranchId(String token, String clinicId, String branchId) {
        return clinicAdminFeign.getWardBoysByClinicIdAndBranchId(token, clinicId, branchId);
    }

    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicIdAndBranchIdFallback(String token, String clinicId, String branchId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="addAdministratorFallback")
    @Retry(name = "clincAdminService", fallbackMethod="addAdministratorFallback")
    public ResponseStructure<AdministratorDTO> addAdministrator(String token, AdministratorDTO dto) {
        return clinicAdminFeign.addAdministrator(token, dto);
    }

    public ResponseStructure<AdministratorDTO> addAdministratorFallback(String token, AdministratorDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getAllAdministratorsByClinicFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllAdministratorsByClinicFallback")
    public ResponseStructure<List<AdministratorDTO>> getAllAdministratorsByClinic(String token, String clinicId) {
        return clinicAdminFeign.getAllAdministratorsByClinic(token, clinicId);
    }

    public ResponseStructure<List<AdministratorDTO>> getAllAdministratorsByClinicFallback(String token, String clinicId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getAllAdministratorsByClinicAndBranchFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAllAdministratorsByClinicAndBranchFallback")
    public ResponseStructure<List<AdministratorDTO>> getAllAdministratorsByClinicAndBranch(String token, String clinicId, String branchId) {
        return clinicAdminFeign.getAllAdministratorsByClinicAndBranch(token, clinicId, branchId);
    }

    public ResponseStructure<List<AdministratorDTO>> getAllAdministratorsByClinicAndBranchFallback(String token, String clinicId, String branchId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getAdministratorByClinicAndIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAdministratorByClinicAndIdFallback")
    public ResponseStructure<AdministratorDTO> getAdministratorByClinicAndId(String token, String clinicId, String adminId) {
        return clinicAdminFeign.getAdministratorByClinicAndId(token, clinicId, adminId);
    }

    public ResponseStructure<AdministratorDTO> getAdministratorByClinicAndIdFallback(String token, String clinicId, String adminId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="getAdministratorByClinicBranchAndAdminIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="getAdministratorByClinicBranchAndAdminIdFallback")
    public ResponseStructure<AdministratorDTO> getAdministratorByClinicBranchAndAdminId(String token, String clinicId, String branchId, String adminId) {
        return clinicAdminFeign.getAdministratorByClinicBranchAndAdminId(token, clinicId, branchId, adminId);
    }

    public ResponseStructure<AdministratorDTO> getAdministratorByClinicBranchAndAdminIdFallback(String token, String clinicId, String branchId, String adminId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="updateAdministratorFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateAdministratorFallback")
    public ResponseStructure<AdministratorDTO> updateAdministrator(String token, String clinicId, String adminId, AdministratorDTO dto) {
        return clinicAdminFeign.updateAdministrator(token, clinicId, adminId, dto);
    }

    public ResponseStructure<AdministratorDTO> updateAdministratorFallback(String token, String clinicId, String adminId, AdministratorDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="updateAdministratorUsingClinicBranchAndAdminIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="updateAdministratorUsingClinicBranchAndAdminIdFallback")
    public ResponseStructure<AdministratorDTO> updateAdministratorUsingClinicBranchAndAdminId(String token, String clinicId, String branchId, String adminId, AdministratorDTO dto) {
        return clinicAdminFeign.updateAdministratorUsingClinicBranchAndAdminId(token, clinicId, branchId, adminId, dto);
    }

    public ResponseStructure<AdministratorDTO> updateAdministratorUsingClinicBranchAndAdminIdFallback(String token, String clinicId, String branchId, String adminId, AdministratorDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="deleteAdministratorFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteAdministratorFallback")
    public ResponseStructure<String> deleteAdministrator(String token, String clinicId, String adminId) {
        return clinicAdminFeign.deleteAdministrator(token, clinicId, adminId);
    }

    public ResponseStructure<String> deleteAdministratorFallback(String token, String clinicId, String adminId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="deleteAdministratorUsingClinicBranchAndAdminIdFallback")
    @Retry(name = "clincAdminService", fallbackMethod="deleteAdministratorUsingClinicBranchAndAdminIdFallback")
    public ResponseStructure<String> deleteAdministratorUsingClinicBranchAndAdminId(String token, String clinicId, String branchId, String adminId) {
        return clinicAdminFeign.deleteAdministratorUsingClinicBranchAndAdminId(token, clinicId, branchId, adminId);
    }

    public ResponseStructure<String> deleteAdministratorUsingClinicBranchAndAdminIdFallback(String token, String clinicId, String branchId, String adminId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "clincAdminService", fallbackMethod="doctorAvailabilityStatusFallback")
    @Retry(name = "clincAdminService", fallbackMethod="doctorAvailabilityStatusFallback")
    public ResponseEntity<Response> doctorAvailabilityStatus(String token, String doctorId, DoctorAvailabilityStatusDTO status) {
        return clinicAdminFeign.doctorAvailabilityStatus(token, doctorId, status);
    }

    public ResponseEntity<Response> doctorAvailabilityStatusFallback(String token, String doctorId, DoctorAvailabilityStatusDTO status, Exception ex) {
        throw getFallbackException(ex);
    }

    private RuntimeException getFallbackException(Throwable ex) {

        if (ex instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Booking service is currently unavailable after multiple retry attempts."
                    );      
        }else if (ex instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Booking Service is temporarily unavailable"); 
    }else{ return new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again after some time.");}
    }

}