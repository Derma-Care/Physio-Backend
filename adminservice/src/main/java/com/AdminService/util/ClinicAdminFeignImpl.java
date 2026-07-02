
package com.AdminService.util;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
    
    @CircuitBreaker(name="adminService", fallbackMethod="addDoctorFallback")
    @Retry(name="adminService", fallbackMethod="addDoctorFallback")
    public ResponseEntity<Response> addDoctor(String token, DoctorsDTO dto) {
        return clinicAdminFeign.addDoctor(token, dto);
    }

    public ResponseEntity<Response> addDoctorFallback(String token, DoctorsDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllDoctorsFallback")
    @Retry(name="adminService", fallbackMethod="getAllDoctorsFallback")
    public ResponseEntity<Response> getAllDoctors(String token) {
        return clinicAdminFeign.getAllDoctors(token);
    }

    public ResponseEntity<Response> getAllDoctorsFallback(String token, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getDoctorByIdFallback")
    @Retry(name="adminService", fallbackMethod="getDoctorByIdFallback")
    public ResponseEntity<Response> getDoctorById(String token, String id) {
        return clinicAdminFeign.getDoctorById(token, id);
    }

    public ResponseEntity<Response> getDoctorByIdFallback(String token, String id, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateDoctorByIdFallback")
    @Retry(name="adminService", fallbackMethod="updateDoctorByIdFallback")
    public ResponseEntity<Response> updateDoctorById(String token, String doctorId, DoctorsDTO dto) {
        return clinicAdminFeign.updateDoctorById(token, doctorId, dto);
    }

    public ResponseEntity<Response> updateDoctorByIdFallback(String token, String doctorId, DoctorsDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteDoctorByIdFallback")
    @Retry(name="adminService", fallbackMethod="deleteDoctorByIdFallback")
    public ResponseEntity<Response> deleteDoctorById(String token, String doctorId) {
        return clinicAdminFeign.deleteDoctorById(token, doctorId);
    }

    public ResponseEntity<Response> deleteDoctorByIdFallback(String token, String doctorId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteDoctorsByClinicFallback")
    @Retry(name="adminService", fallbackMethod="deleteDoctorsByClinicFallback")
    public ResponseEntity<Response> deleteDoctorsByClinic(String token, String clinicId) {
        return clinicAdminFeign.deleteDoctorsByClinic(token, clinicId);
    }

    public ResponseEntity<Response> deleteDoctorsByClinicFallback(String token, String clinicId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getDoctorByClinicAndDoctorIdFallback")
    @Retry(name="adminService", fallbackMethod="getDoctorByClinicAndDoctorIdFallback")
    public ResponseEntity<Response> getDoctorByClinicAndDoctorId(String token, String clinicId, String doctorId) {
        return clinicAdminFeign.getDoctorByClinicAndDoctorId(token, clinicId, doctorId);
    }

    public ResponseEntity<Response> getDoctorByClinicAndDoctorIdFallback(String token, String clinicId, String doctorId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getDoctorsByHospitalIdFallback")
    @Retry(name="adminService", fallbackMethod="getDoctorsByHospitalIdFallback")
    public ResponseEntity<Response> getDoctorsByHospitalId(String token, String hospitalId) {
        return clinicAdminFeign.getDoctorsByHospitalId(token, hospitalId);
    }

    public ResponseEntity<Response> getDoctorsByHospitalIdFallback(String token, String hospitalId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getDoctorsByHospitalIdAndBranchIdFallback")
    @Retry(name="adminService", fallbackMethod="getDoctorsByHospitalIdAndBranchIdFallback")
    public ResponseEntity<Response> getDoctorsByHospitalIdAndBranchId(String token, String hospitalId, String branchId) {
        return clinicAdminFeign.getDoctorsByHospitalIdAndBranchId(token, hospitalId, branchId);
    }

    public ResponseEntity<Response> getDoctorsByHospitalIdAndBranchIdFallback(String token, String hospitalId, String branchId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="addDiseasesFallback")
    @Retry(name="adminService", fallbackMethod="addDiseasesFallback")
    public ResponseEntity<Response> addDiseases(String token, Object requestBody) {
        return clinicAdminFeign.addDiseases(token, requestBody);
    }

    public ResponseEntity<Response> addDiseasesFallback(String token, Object requestBody, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllDiseasesFallback")
    @Retry(name="adminService", fallbackMethod="getAllDiseasesFallback")
    public ResponseEntity<Response> getAllDiseases(String token) {
        return clinicAdminFeign.getAllDiseases(token);
    }

    public ResponseEntity<Response> getAllDiseasesFallback(String token, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getDiseaseByDiseaseIdFallback")
    @Retry(name="adminService", fallbackMethod="getDiseaseByDiseaseIdFallback")
    public ResponseEntity<Response> getDiseaseByDiseaseId(String id, String hospitalId) {
        return clinicAdminFeign.getDiseaseByDiseaseId(id, hospitalId);
    }

    public ResponseEntity<Response> getDiseaseByDiseaseIdFallback(String id, String hospitalId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteDiseaseByDiseaseIdFallback")
    @Retry(name="adminService", fallbackMethod="deleteDiseaseByDiseaseIdFallback")
    public ResponseEntity<Response> deleteDiseaseByDiseaseId(String id, String hospitalId) {
        return clinicAdminFeign.deleteDiseaseByDiseaseId(id, hospitalId);
    }

    public ResponseEntity<Response> deleteDiseaseByDiseaseIdFallback(String id, String hospitalId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateDiseaseByDiseaseIdFallback")
    @Retry(name="adminService", fallbackMethod="updateDiseaseByDiseaseIdFallback")
    public ResponseEntity<Response> updateDiseaseByDiseaseId(String id, String hospitalId, ProbableDiagnosisDTO dto) {
        return clinicAdminFeign.updateDiseaseByDiseaseId(id, hospitalId, dto);
    }

    public ResponseEntity<Response> updateDiseaseByDiseaseIdFallback(String id, String hospitalId, ProbableDiagnosisDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getDiseasesByHospitalIdFallback")
    @Retry(name="adminService", fallbackMethod="getDiseasesByHospitalIdFallback")
    public ResponseEntity<ResponseStructure<List<ProbableDiagnosisDTO>>> getDiseasesByHospitalId(String hospitalId) {
        return clinicAdminFeign.getDiseasesByHospitalId(hospitalId);
    }

    public ResponseEntity<ResponseStructure<List<ProbableDiagnosisDTO>>> getDiseasesByHospitalIdFallback(String hospitalId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="addLabTestFallback")
    @Retry(name="adminService", fallbackMethod="addLabTestFallback")
    public ResponseEntity<Response> addLabTest(LabTestDTO dto) {
        return clinicAdminFeign.addLabTest(dto);
    }

    public ResponseEntity<Response> addLabTestFallback(LabTestDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllLabTestsFallback")
    @Retry(name="adminService", fallbackMethod="getAllLabTestsFallback")
    public ResponseEntity<Response> getAllLabTests() {
        return clinicAdminFeign.getAllLabTests();
    }

    public ResponseEntity<Response> getAllLabTestsFallback(Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getLabTestByIdFallback")
    @Retry(name="adminService", fallbackMethod="getLabTestByIdFallback")
    public ResponseEntity<Response> getLabTestById(String id, String hospitalId) {
        return clinicAdminFeign.getLabTestById(id, hospitalId);
    }

    public ResponseEntity<Response> getLabTestByIdFallback(String id, String hospitalId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteLabTestFallback")
    @Retry(name="adminService", fallbackMethod="deleteLabTestFallback")
    public ResponseEntity<Response> deleteLabTest(String id, String hospitalId) {
        return clinicAdminFeign.deleteLabTest(id, hospitalId);
    }

    public ResponseEntity<Response> deleteLabTestFallback(String id, String hospitalId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateLabTestFallback")
    @Retry(name="adminService", fallbackMethod="updateLabTestFallback")
    public ResponseEntity<Response> updateLabTest(String id, String hospitalId, LabTestDTO dto) {
        return clinicAdminFeign.updateLabTest(id, hospitalId, dto);
    }

    public ResponseEntity<Response> updateLabTestFallback(String id, String hospitalId, LabTestDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getLabTestsByHospitalIdFallback")
    @Retry(name="adminService", fallbackMethod="getLabTestsByHospitalIdFallback")
    public ResponseEntity<ResponseStructure<List<LabTestDTO>>> getLabTestsByHospitalId(String hospitalId) {
        return clinicAdminFeign.getLabTestsByHospitalId(hospitalId);
    }

    public ResponseEntity<ResponseStructure<List<LabTestDTO>>> getLabTestsByHospitalIdFallback(String hospitalId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="addTreatmentFallback")
    @Retry(name="adminService", fallbackMethod="addTreatmentFallback")
    public ResponseEntity<Response> addTreatment(TreatmentDTO dto) {
        return clinicAdminFeign.addTreatment(dto);
    }

    public ResponseEntity<Response> addTreatmentFallback(TreatmentDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllTreatmentsFallback")
    @Retry(name="adminService", fallbackMethod="getAllTreatmentsFallback")
    public ResponseEntity<Response> getAllTreatments() {
        return clinicAdminFeign.getAllTreatments();
    }

    public ResponseEntity<Response> getAllTreatmentsFallback(Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getTreatmentByIdFallback")
    @Retry(name="adminService", fallbackMethod="getTreatmentByIdFallback")
    public ResponseEntity<Response> getTreatmentById(String id, String hospitalId) {
        return clinicAdminFeign.getTreatmentById(id, hospitalId);
    }

    public ResponseEntity<Response> getTreatmentByIdFallback(String id, String hospitalId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteTreatmentByIdFallback")
    @Retry(name="adminService", fallbackMethod="deleteTreatmentByIdFallback")
    public ResponseEntity<Response> deleteTreatmentById(String id, String hospitalId) {
        return clinicAdminFeign.deleteTreatmentById(id, hospitalId);
    }

    public ResponseEntity<Response> deleteTreatmentByIdFallback(String id, String hospitalId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateTreatmentByIdFallback")
    @Retry(name="adminService", fallbackMethod="updateTreatmentByIdFallback")
    public ResponseEntity<Response> updateTreatmentById(String id, String hospitalId, TreatmentDTO dto) {
        return clinicAdminFeign.updateTreatmentById(id, hospitalId, dto);
    }

    public ResponseEntity<Response> updateTreatmentByIdFallback(String id, String hospitalId, TreatmentDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getTreatmentsByHospitalIdFallback")
    @Retry(name="adminService", fallbackMethod="getTreatmentsByHospitalIdFallback")
    public ResponseEntity<ResponseStructure<List<TreatmentDTO>>> getTreatmentsByHospitalId(String hospitalId) {
        return clinicAdminFeign.getTreatmentsByHospitalId(hospitalId);
    }

    public ResponseEntity<ResponseStructure<List<TreatmentDTO>>> getTreatmentsByHospitalIdFallback(String hospitalId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="addDoctorSlotFallback")
    @Retry(name="adminService", fallbackMethod="addDoctorSlotFallback")
    public ResponseEntity<Response> addDoctorSlot(String token, String hospitalId, String branchId, String doctorId, DoctorSlotDTO slotDto) {
        return clinicAdminFeign.addDoctorSlot(token, hospitalId, branchId, doctorId, slotDto);
    }

    public ResponseEntity<Response> addDoctorSlotFallback(String token, String hospitalId, String branchId, String doctorId, DoctorSlotDTO slotDto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getDoctorSlotsFallback")
    @Retry(name="adminService", fallbackMethod="getDoctorSlotsFallback")
    public ResponseEntity<Response> getDoctorSlots(String token, String hospitalId, String branchId, String doctorId) {
        return clinicAdminFeign.getDoctorSlots(token, hospitalId, branchId, doctorId);
    }

    public ResponseEntity<Response> getDoctorSlotsFallback(String token, String hospitalId, String branchId, String doctorId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateDoctorSlotFallback")
    @Retry(name="adminService", fallbackMethod="updateDoctorSlotFallback")
    public ResponseEntity<Response> updateDoctorSlot(String token, UpdateSlotRequestDTO request) {
        return clinicAdminFeign.updateDoctorSlot(token, request);
    }

    public ResponseEntity<Response> updateDoctorSlotFallback(String token, UpdateSlotRequestDTO request, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteDoctorSlotFallback")
    @Retry(name="adminService", fallbackMethod="deleteDoctorSlotFallback")
    public Response deleteDoctorSlot(String token, String doctorId, String date, String slot) {
        return clinicAdminFeign.deleteDoctorSlot(token, doctorId, date, slot);
    }

    
    @CircuitBreaker(name = "adminService", fallbackMethod = "deleteDoctorSlotFallback")
    @Retry(name = "adminService", fallbackMethod = "deleteDoctorSlotsBasedOnIdsFallback")
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
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteDoctorSlotsByDateFallback")
    @Retry(name="adminService", fallbackMethod="deleteDoctorSlotsByDateFallback")
    public ResponseEntity<Response> deleteDoctorSlotsByDate(String token, String doctorId, String branchId, String date) {
        return clinicAdminFeign.deleteDoctorSlotsByDate(token, doctorId, branchId, date);
    }

    public ResponseEntity<Response> deleteDoctorSlotsByDateFallback(String token, String doctorId, String branchId, String date, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateDoctorSlotWhileBookingFallback")
    @Retry(name="adminService", fallbackMethod="updateDoctorSlotWhileBookingFallback")
    public boolean updateDoctorSlotWhileBooking(String token, String doctorId, String branchId, String date, String time) {
        return clinicAdminFeign.updateDoctorSlotWhileBooking(token, doctorId, branchId, date, time);
    }

    public boolean updateDoctorSlotWhileBookingFallback(String token, String doctorId, String branchId, String date, String time, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="makingFalseDoctorSlotFallback")
    @Retry(name="adminService", fallbackMethod="makingFalseDoctorSlotFallback")
    public boolean makingFalseDoctorSlot(String token, String doctorId, String branchId, String date, String time) {
        return clinicAdminFeign.makingFalseDoctorSlot(token, doctorId, branchId, date, time);
    }

    public boolean makingFalseDoctorSlotFallback(String token, String doctorId, String branchId, String date, String time, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="generateSlotsFallback")
    @Retry(name="adminService", fallbackMethod="generateSlotsFallback")
    public Response generateSlots(String token, String doctorId, String branchId, String date, int intervalMinutes, String openingTime, String closingTime) {
        return clinicAdminFeign.generateSlots(token, doctorId, branchId, date, intervalMinutes, openingTime, closingTime);
    }

    public Response generateSlotsFallback(String token, String doctorId, String branchId, String date, int intervalMinutes, String openingTime, String closingTime, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getDoctorSlotFallback")
    @Retry(name="adminService", fallbackMethod="getDoctorSlotFallback")
    public ResponseEntity<Response> getDoctorSlot(String hospitalId, String branchId, String doctorId) {
        return clinicAdminFeign.getDoctorSlot(hospitalId, branchId, doctorId);
    }

    public ResponseEntity<Response> getDoctorSlotFallback(String hospitalId, String branchId, String doctorId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="createLabTechnicianFallback")
    @Retry(name="adminService", fallbackMethod="createLabTechnicianFallback")
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> createLabTechnician(LabTechnicianRequestDTO dto) {
        return clinicAdminFeign.createLabTechnician(dto);
    }

    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> createLabTechnicianFallback(LabTechnicianRequestDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllLabTechniciansFallback")
    @Retry(name="adminService", fallbackMethod="getAllLabTechniciansFallback")
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getAllLabTechnicians() {
        return clinicAdminFeign.getAllLabTechnicians();
    }

    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getAllLabTechniciansFallback(Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateLabTechnicianFallback")
    @Retry(name="adminService", fallbackMethod="updateLabTechnicianFallback")
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> updateLabTechnician(String id, LabTechnicianRequestDTO dto) {
        return clinicAdminFeign.updateLabTechnician(id, dto);
    }

    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> updateLabTechnicianFallback(String id, LabTechnicianRequestDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteLabTechnicianFallback")
    @Retry(name="adminService", fallbackMethod="deleteLabTechnicianFallback")
    public ResponseEntity<ResponseStructure<String>> deleteLabTechnician(String id) {
        return clinicAdminFeign.deleteLabTechnician(id);
    }

    public ResponseEntity<ResponseStructure<String>> deleteLabTechnicianFallback(String id, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getLabTechniciansByClinicFallback")
    @Retry(name="adminService", fallbackMethod="getLabTechniciansByClinicFallback")
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinic(String clinicId) {
        return clinicAdminFeign.getLabTechniciansByClinic(clinicId);
    }

    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinicFallback(String clinicId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getLabTechnicianByClinicAndIdFallback")
    @Retry(name="adminService", fallbackMethod="getLabTechnicianByClinicAndIdFallback")
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> getLabTechnicianByClinicAndId(String clinicId, String technicianId) {
        return clinicAdminFeign.getLabTechnicianByClinicAndId(clinicId, technicianId);
    }

    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> getLabTechnicianByClinicAndIdFallback(String clinicId, String technicianId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getLabTechniciansByClinicAndBranchFallback")
    @Retry(name="adminService", fallbackMethod="getLabTechniciansByClinicAndBranchFallback")
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinicAndBranch(String clinicId, String branchId) {
        return clinicAdminFeign.getLabTechniciansByClinicAndBranch(clinicId, branchId);
    }

    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinicAndBranchFallback(String clinicId, String branchId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="createReceptionistFallback")
    @Retry(name="adminService", fallbackMethod="createReceptionistFallback")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> createReceptionist(String token, ReceptionistRequestDTO dto) {
        return clinicAdminFeign.createReceptionist(token, dto);
    }

    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> createReceptionistFallback(String token, ReceptionistRequestDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getReceptionistByIdFallback")
    @Retry(name="adminService", fallbackMethod="getReceptionistByIdFallback")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistById(String token, String id) {
        return clinicAdminFeign.getReceptionistById(token, id);
    }

    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistByIdFallback(String token, String id, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllReceptionistsFallback")
    @Retry(name="adminService", fallbackMethod="getAllReceptionistsFallback")
    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getAllReceptionists(String token) {
        return clinicAdminFeign.getAllReceptionists(token);
    }

    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getAllReceptionistsFallback(String token, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateReceptionistFallback")
    @Retry(name="adminService", fallbackMethod="updateReceptionistFallback")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> updateReceptionist(String token, String id, ReceptionistRequestDTO dto) {
        return clinicAdminFeign.updateReceptionist(token, id, dto);
    }

    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> updateReceptionistFallback(String token, String id, ReceptionistRequestDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteReceptionistFallback")
    @Retry(name="adminService", fallbackMethod="deleteReceptionistFallback")
    public ResponseEntity<ResponseStructure<String>> deleteReceptionist(String token, String id) {
        return clinicAdminFeign.deleteReceptionist(token, id);
    }

    public ResponseEntity<ResponseStructure<String>> deleteReceptionistFallback(String token, String id, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getReceptionistsByClinicFallback")
    @Retry(name="adminService", fallbackMethod="getReceptionistsByClinicFallback")
    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinic(String token, String clinicId) {
        return clinicAdminFeign.getReceptionistsByClinic(token, clinicId);
    }

    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinicFallback(String token, String clinicId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getReceptionistByClinicAndIdFallback")
    @Retry(name="adminService", fallbackMethod="getReceptionistByClinicAndIdFallback")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistByClinicAndId(String token, String clinicId, String receptionistId) {
        return clinicAdminFeign.getReceptionistByClinicAndId(token, clinicId, receptionistId);
    }

    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistByClinicAndIdFallback(String token, String clinicId, String receptionistId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getReceptionistsByClinicAndBranchFallback")
    @Retry(name="adminService", fallbackMethod="getReceptionistsByClinicAndBranchFallback")
    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinicAndBranch(String token, String clinicId, String branchId) {
        return clinicAdminFeign.getReceptionistsByClinicAndBranch(token, clinicId, branchId);
    }

    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinicAndBranchFallback(String token, String clinicId, String branchId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="nurseOnBoardingFallback")
    @Retry(name="adminService", fallbackMethod="nurseOnBoardingFallback")
    public ResponseEntity<Response> nurseOnBoarding(NurseDTO dto) {
        return clinicAdminFeign.nurseOnBoarding(dto);
    }

    public ResponseEntity<Response> nurseOnBoardingFallback(NurseDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllByHospitalFallback")
    @Retry(name="adminService", fallbackMethod="getAllByHospitalFallback")
    public ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllByHospital(String hospitalId) {
        return clinicAdminFeign.getAllByHospital(hospitalId);
    }

    public ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllByHospitalFallback(String hospitalId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getNurseFallback")
    @Retry(name="adminService", fallbackMethod="getNurseFallback")
    public ResponseEntity<ResponseStructure<NurseDTO>> getNurse(String hospitalId, String nurseId) {
        return clinicAdminFeign.getNurse(hospitalId, nurseId);
    }

    public ResponseEntity<ResponseStructure<NurseDTO>> getNurseFallback(String hospitalId, String nurseId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateNurseFallback")
    @Retry(name="adminService", fallbackMethod="updateNurseFallback")
    public ResponseEntity<ResponseStructure<NurseDTO>> updateNurse(String nurseId, NurseDTO dto) {
        return clinicAdminFeign.updateNurse(nurseId, dto);
    }

    public ResponseEntity<ResponseStructure<NurseDTO>> updateNurseFallback(String nurseId, NurseDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteNurseFallback")
    @Retry(name="adminService", fallbackMethod="deleteNurseFallback")
    public ResponseEntity<ResponseStructure<String>> deleteNurse(String hospitalId, String nurseId) {
        return clinicAdminFeign.deleteNurse(hospitalId, nurseId);
    }

    public ResponseEntity<ResponseStructure<String>> deleteNurseFallback(String hospitalId, String nurseId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllNursesByBranchFallback")
    @Retry(name="adminService", fallbackMethod="getAllNursesByBranchFallback")
    public ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllNursesByBranch(String hospitalId, String branchId) {
        return clinicAdminFeign.getAllNursesByBranch(hospitalId, branchId);
    }

    public ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllNursesByBranchFallback(String hospitalId, String branchId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="pharmacistOnBoardingFallback")
    @Retry(name="adminService", fallbackMethod="pharmacistOnBoardingFallback")
    public ResponseEntity<ResponseStructure<PharmacistDTO>> pharmacistOnBoarding(PharmacistDTO dto) {
        return clinicAdminFeign.pharmacistOnBoarding(dto);
    }

    public ResponseEntity<ResponseStructure<PharmacistDTO>> pharmacistOnBoardingFallback(PharmacistDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllByDepartmentFallback")
    @Retry(name="adminService", fallbackMethod="getAllByDepartmentFallback")
    public ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getAllByDepartment(String hospitalId) {
        return clinicAdminFeign.getAllByDepartment(hospitalId);
    }

    public ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getAllByDepartmentFallback(String hospitalId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getPharmacistFallback")
    @Retry(name="adminService", fallbackMethod="getPharmacistFallback")
    public ResponseEntity<ResponseStructure<PharmacistDTO>> getPharmacist(String pharmacistId) {
        return clinicAdminFeign.getPharmacist(pharmacistId);
    }

    public ResponseEntity<ResponseStructure<PharmacistDTO>> getPharmacistFallback(String pharmacistId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updatePharmacistFallback")
    @Retry(name="adminService", fallbackMethod="updatePharmacistFallback")
    public ResponseEntity<ResponseStructure<PharmacistDTO>> updatePharmacist(String pharmacistId, PharmacistDTO dto) {
        return clinicAdminFeign.updatePharmacist(pharmacistId, dto);
    }

    public ResponseEntity<ResponseStructure<PharmacistDTO>> updatePharmacistFallback(String pharmacistId, PharmacistDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deletePharmacistFallback")
    @Retry(name="adminService", fallbackMethod="deletePharmacistFallback")
    public ResponseEntity<ResponseStructure<String>> deletePharmacist(String pharmacistId) {
        return clinicAdminFeign.deletePharmacist(pharmacistId);
    }

    public ResponseEntity<ResponseStructure<String>> deletePharmacistFallback(String pharmacistId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getPharmacistsByHospitalIdAndBranchIdFallback")
    @Retry(name="adminService", fallbackMethod="getPharmacistsByHospitalIdAndBranchIdFallback")
    public ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getPharmacistsByHospitalIdAndBranchId(String hospitalId, String branchId) {
        return clinicAdminFeign.getPharmacistsByHospitalIdAndBranchId(hospitalId, branchId);
    }

    public ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getPharmacistsByHospitalIdAndBranchIdFallback(String hospitalId, String branchId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="addSecurityStaffFallback")
    @Retry(name="adminService", fallbackMethod="addSecurityStaffFallback")
    public ResponseStructure<SecurityStaffDTO> addSecurityStaff(String token, SecurityStaffDTO dto) {
        return clinicAdminFeign.addSecurityStaff(token, dto);
    }

    public ResponseStructure<SecurityStaffDTO> addSecurityStaffFallback(String token, SecurityStaffDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateSecurityStaffFallback")
    @Retry(name="adminService", fallbackMethod="updateSecurityStaffFallback")
    public ResponseStructure<SecurityStaffDTO> updateSecurityStaff(String token, String staffId, SecurityStaffDTO staffRequest) {
        return clinicAdminFeign.updateSecurityStaff(token, staffId, staffRequest);
    }

    public ResponseStructure<SecurityStaffDTO> updateSecurityStaffFallback(String token, String staffId, SecurityStaffDTO staffRequest, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getSecurityStaffByIdFallback")
    @Retry(name="adminService", fallbackMethod="getSecurityStaffByIdFallback")
    public ResponseStructure<SecurityStaffDTO> getSecurityStaffById(String token, String staffId) {
        return clinicAdminFeign.getSecurityStaffById(token, staffId);
    }

    public ResponseStructure<SecurityStaffDTO> getSecurityStaffByIdFallback(String token, String staffId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllByClinicIdFallback")
    @Retry(name="adminService", fallbackMethod="getAllByClinicIdFallback")
    public ResponseStructure<List<SecurityStaffDTO>> getAllByClinicId(String token, String clinicId) {
        return clinicAdminFeign.getAllByClinicId(token, clinicId);
    }

    public ResponseStructure<List<SecurityStaffDTO>> getAllByClinicIdFallback(String token, String clinicId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteSecurityStaffFallback")
    @Retry(name="adminService", fallbackMethod="deleteSecurityStaffFallback")
    public ResponseStructure<String> deleteSecurityStaff(String token, String staffId) {
        return clinicAdminFeign.deleteSecurityStaff(token, staffId);
    }

    public ResponseStructure<String> deleteSecurityStaffFallback(String token, String staffId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getSecurityStaffByClinicIdAndBranchIdFallback")
    @Retry(name="adminService", fallbackMethod="getSecurityStaffByClinicIdAndBranchIdFallback")
    public ResponseStructure<List<SecurityStaffDTO>> getSecurityStaffByClinicIdAndBranchId(String token, String clinicId, String branchId) {
        return clinicAdminFeign.getSecurityStaffByClinicIdAndBranchId(token, clinicId, branchId);
    }

    public ResponseStructure<List<SecurityStaffDTO>> getSecurityStaffByClinicIdAndBranchIdFallback(String token, String clinicId, String branchId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="addWardBoyFallback")
    @Retry(name="adminService", fallbackMethod="addWardBoyFallback")
    public ResponseStructure<WardBoyDTO> addWardBoy(String token, WardBoyDTO dto) {
        return clinicAdminFeign.addWardBoy(token, dto);
    }

    public ResponseStructure<WardBoyDTO> addWardBoyFallback(String token, WardBoyDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateWardBoyFallback")
    @Retry(name="adminService", fallbackMethod="updateWardBoyFallback")
    public ResponseStructure<WardBoyDTO> updateWardBoy(String token, String id, WardBoyDTO dto) {
        return clinicAdminFeign.updateWardBoy(token, id, dto);
    }

    public ResponseStructure<WardBoyDTO> updateWardBoyFallback(String token, String id, WardBoyDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getWardBoyByIdFallback")
    @Retry(name="adminService", fallbackMethod="getWardBoyByIdFallback")
    public ResponseStructure<WardBoyDTO> getWardBoyById(String token, String id) {
        return clinicAdminFeign.getWardBoyById(token, id);
    }

    public ResponseStructure<WardBoyDTO> getWardBoyByIdFallback(String token, String id, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllWardBoysFallback")
    @Retry(name="adminService", fallbackMethod="getAllWardBoysFallback")
    public ResponseStructure<List<WardBoyDTO>> getAllWardBoys(String token) {
        return clinicAdminFeign.getAllWardBoys(token);
    }

    public ResponseStructure<List<WardBoyDTO>> getAllWardBoysFallback(String token, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getWardBoysByClinicIdFallback")
    @Retry(name="adminService", fallbackMethod="getWardBoysByClinicIdFallback")
    public ResponseStructure<List<WardBoyDTO>> getWardBoysByClinicId(String token, String clinicId) {
        return clinicAdminFeign.getWardBoysByClinicId(token, clinicId);
    }

    public ResponseStructure<List<WardBoyDTO>> getWardBoysByClinicIdFallback(String token, String clinicId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getWardBoyByIdAndClinicIdFallback")
    @Retry(name="adminService", fallbackMethod="getWardBoyByIdAndClinicIdFallback")
    public ResponseStructure<WardBoyDTO> getWardBoyByIdAndClinicId(String token, String wardBoyId, String clinicId) {
        return clinicAdminFeign.getWardBoyByIdAndClinicId(token, wardBoyId, clinicId);
    }

    public ResponseStructure<WardBoyDTO> getWardBoyByIdAndClinicIdFallback(String token, String wardBoyId, String clinicId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteWardBoyFallback")
    @Retry(name="adminService", fallbackMethod="deleteWardBoyFallback")
    public ResponseStructure<Void> deleteWardBoy(String token, String id) {
        return clinicAdminFeign.deleteWardBoy(token, id);
    }

    public ResponseStructure<Void> deleteWardBoyFallback(String token, String id, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getWardBoysByClinicIdAndBranchIdFallback")
    @Retry(name="adminService", fallbackMethod="getWardBoysByClinicIdAndBranchIdFallback")
    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicIdAndBranchId(String token, String clinicId, String branchId) {
        return clinicAdminFeign.getWardBoysByClinicIdAndBranchId(token, clinicId, branchId);
    }

    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicIdAndBranchIdFallback(String token, String clinicId, String branchId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="addAdministratorFallback")
    @Retry(name="adminService", fallbackMethod="addAdministratorFallback")
    public ResponseStructure<AdministratorDTO> addAdministrator(String token, AdministratorDTO dto) {
        return clinicAdminFeign.addAdministrator(token, dto);
    }

    public ResponseStructure<AdministratorDTO> addAdministratorFallback(String token, AdministratorDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllAdministratorsByClinicFallback")
    @Retry(name="adminService", fallbackMethod="getAllAdministratorsByClinicFallback")
    public ResponseStructure<List<AdministratorDTO>> getAllAdministratorsByClinic(String token, String clinicId) {
        return clinicAdminFeign.getAllAdministratorsByClinic(token, clinicId);
    }

    public ResponseStructure<List<AdministratorDTO>> getAllAdministratorsByClinicFallback(String token, String clinicId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllAdministratorsByClinicAndBranchFallback")
    @Retry(name="adminService", fallbackMethod="getAllAdministratorsByClinicAndBranchFallback")
    public ResponseStructure<List<AdministratorDTO>> getAllAdministratorsByClinicAndBranch(String token, String clinicId, String branchId) {
        return clinicAdminFeign.getAllAdministratorsByClinicAndBranch(token, clinicId, branchId);
    }

    public ResponseStructure<List<AdministratorDTO>> getAllAdministratorsByClinicAndBranchFallback(String token, String clinicId, String branchId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAdministratorByClinicAndIdFallback")
    @Retry(name="adminService", fallbackMethod="getAdministratorByClinicAndIdFallback")
    public ResponseStructure<AdministratorDTO> getAdministratorByClinicAndId(String token, String clinicId, String adminId) {
        return clinicAdminFeign.getAdministratorByClinicAndId(token, clinicId, adminId);
    }

    public ResponseStructure<AdministratorDTO> getAdministratorByClinicAndIdFallback(String token, String clinicId, String adminId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="getAdministratorByClinicBranchAndAdminIdFallback")
    @Retry(name="adminService", fallbackMethod="getAdministratorByClinicBranchAndAdminIdFallback")
    public ResponseStructure<AdministratorDTO> getAdministratorByClinicBranchAndAdminId(String token, String clinicId, String branchId, String adminId) {
        return clinicAdminFeign.getAdministratorByClinicBranchAndAdminId(token, clinicId, branchId, adminId);
    }

    public ResponseStructure<AdministratorDTO> getAdministratorByClinicBranchAndAdminIdFallback(String token, String clinicId, String branchId, String adminId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateAdministratorFallback")
    @Retry(name="adminService", fallbackMethod="updateAdministratorFallback")
    public ResponseStructure<AdministratorDTO> updateAdministrator(String token, String clinicId, String adminId, AdministratorDTO dto) {
        return clinicAdminFeign.updateAdministrator(token, clinicId, adminId, dto);
    }

    public ResponseStructure<AdministratorDTO> updateAdministratorFallback(String token, String clinicId, String adminId, AdministratorDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="updateAdministratorUsingClinicBranchAndAdminIdFallback")
    @Retry(name="adminService", fallbackMethod="updateAdministratorUsingClinicBranchAndAdminIdFallback")
    public ResponseStructure<AdministratorDTO> updateAdministratorUsingClinicBranchAndAdminId(String token, String clinicId, String branchId, String adminId, AdministratorDTO dto) {
        return clinicAdminFeign.updateAdministratorUsingClinicBranchAndAdminId(token, clinicId, branchId, adminId, dto);
    }

    public ResponseStructure<AdministratorDTO> updateAdministratorUsingClinicBranchAndAdminIdFallback(String token, String clinicId, String branchId, String adminId, AdministratorDTO dto, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteAdministratorFallback")
    @Retry(name="adminService", fallbackMethod="deleteAdministratorFallback")
    public ResponseStructure<String> deleteAdministrator(String token, String clinicId, String adminId) {
        return clinicAdminFeign.deleteAdministrator(token, clinicId, adminId);
    }

    public ResponseStructure<String> deleteAdministratorFallback(String token, String clinicId, String adminId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="deleteAdministratorUsingClinicBranchAndAdminIdFallback")
    @Retry(name="adminService", fallbackMethod="deleteAdministratorUsingClinicBranchAndAdminIdFallback")
    public ResponseStructure<String> deleteAdministratorUsingClinicBranchAndAdminId(String token, String clinicId, String branchId, String adminId) {
        return clinicAdminFeign.deleteAdministratorUsingClinicBranchAndAdminId(token, clinicId, branchId, adminId);
    }

    public ResponseStructure<String> deleteAdministratorUsingClinicBranchAndAdminIdFallback(String token, String clinicId, String branchId, String adminId, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }

    @CircuitBreaker(name="adminService", fallbackMethod="doctorAvailabilityStatusFallback")
    @Retry(name="adminService", fallbackMethod="doctorAvailabilityStatusFallback")
    public ResponseEntity<Response> doctorAvailabilityStatus(String token, String doctorId, DoctorAvailabilityStatusDTO status) {
        return clinicAdminFeign.doctorAvailabilityStatus(token, doctorId, status);
    }

    public ResponseEntity<Response> doctorAvailabilityStatusFallback(String token, String doctorId, DoctorAvailabilityStatusDTO status, Exception ex) {
        throw new RuntimeException("Clinic Admin Service unavailable", ex);
    }
}
