package com.AdminService.feign;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
import com.AdminService.dto.SubServicesDto;
import com.AdminService.dto.TreatmentDTO;
import com.AdminService.dto.UpdateSlotRequestDTO;
import com.AdminService.dto.WardBoyDTO;
import com.AdminService.util.Response;
import com.AdminService.util.ResponseStructure;


@FeignClient(value = "clinicadmin")
public interface ClinicAdminFeign {

    // ---------------------- Sub-Service APIs ----------------------
    @GetMapping("/clinic-admin/subService/getAllSubServies")
    ResponseEntity<ResponseStructure<List<SubServicesDto>>> getAllSubServices();

    // ---------------- Doctor CRUD ---------------- //
    @PostMapping("/clinic-admin/addDoctor")
    ResponseEntity<Response> addDoctor(@RequestBody DoctorsDTO dto);

    @GetMapping("/clinic-admin/doctors")
    ResponseEntity<Response> getAllDoctors();

    @GetMapping("/clinic-admin/doctor/{id}")
    ResponseEntity<Response> getDoctorById(@PathVariable("id") String id);

    @PutMapping("/clinic-admin/updateDoctor/{doctorId}")
    ResponseEntity<Response> updateDoctorById(@PathVariable("doctorId") String doctorId,
                                              @RequestBody DoctorsDTO dto);

    @DeleteMapping("/clinic-admin/delete-doctor/{doctorId}")
    ResponseEntity<Response> deleteDoctorById(@PathVariable("doctorId") String doctorId);

    @DeleteMapping("/clinic-admin/delete-doctors-by-clinic/{clinicId}")
    ResponseEntity<Response> deleteDoctorsByClinic(@PathVariable("clinicId") String clinicId);

    // ---------------- Additional Filters ---------------- //
    @GetMapping("/clinic-admin/clinic/{clinicId}/doctor/{doctorId}")
    ResponseEntity<Response> getDoctorByClinicAndDoctorId(@PathVariable("clinicId") String clinicId,
                                                          @PathVariable("doctorId") String doctorId);

    @GetMapping("/clinic-admin/doctors/hospitalById/{hospitalId}")
    ResponseEntity<Response> getDoctorsByHospitalId(@PathVariable("hospitalId") String hospitalId);

    @GetMapping("/clinic-admin/getDoctorsByHospitalIdAndBranchId/{hospitalId}/{branchId}")
    ResponseEntity<Response> getDoctorsByHospitalIdAndBranchId(@PathVariable("hospitalId") String hospitalId,
                                                               @PathVariable("branchId") String branchId);

    // ---------------------- Disease APIs ----------------------
    @PostMapping("/clinic-admin/addDiseases")
    ResponseEntity<Response> addDiseases(@RequestBody Object requestBody);

    @GetMapping("/clinic-admin/get-all-diseases")
    ResponseEntity<Response> getAllDiseases();

    @GetMapping("/clinic-admin/getDisease/{id}/{hospitalId}")
    ResponseEntity<Response> getDiseaseByDiseaseId(@PathVariable("id") String id,
                                                   @PathVariable("hospitalId") String hospitalId);

    @DeleteMapping("/clinic-admin/deleteDisease/{id}/{hospitalId}")
    ResponseEntity<Response> deleteDiseaseByDiseaseId(@PathVariable("id") String id,
                                                      @PathVariable("hospitalId") String hospitalId);

    @PutMapping("/clinic-admin/updateDisease/{id}/{hospitalId}")
    ResponseEntity<Response> updateDiseaseByDiseaseId(@PathVariable("id") String id,
                                                      @PathVariable("hospitalId") String hospitalId,
                                                      @RequestBody ProbableDiagnosisDTO dto);

    @GetMapping("/clinic-admin/diseases/{hospitalId}")
    ResponseEntity<ResponseStructure<List<ProbableDiagnosisDTO>>> getDiseasesByHospitalId(@PathVariable("hospitalId") String hospitalId);

    // ---------------------- Lab Test APIs ----------------------
    @PostMapping("/clinic-admin/labtest/addLabTest")
    ResponseEntity<Response> addLabTest(@RequestBody LabTestDTO dto);

    @GetMapping("/clinic-admin/labtest/getAllLabTests")
    ResponseEntity<Response> getAllLabTests();

    @GetMapping("/clinic-admin/labtest/getLabTestById/{id}/{hospitalId}")
    ResponseEntity<Response> getLabTestById(@PathVariable("id") String id,
                                            @PathVariable("hospitalId") String hospitalId);

    @DeleteMapping("/clinic-admin/labtest/deleteLabTest/{id}/{hospitalId}")
    ResponseEntity<Response> deleteLabTest(@PathVariable("id") String id,
                                           @PathVariable("hospitalId") String hospitalId);

    @PutMapping("/clinic-admin/labtest/updateLabTest/{id}/{hospitalId}")
    ResponseEntity<Response> updateLabTest(@PathVariable("id") String id,
                                           @PathVariable("hospitalId") String hospitalId,
                                           @RequestBody LabTestDTO dto);

    @GetMapping("/clinic-admin/labtests/{hospitalId}")
    ResponseEntity<ResponseStructure<List<LabTestDTO>>> getLabTestsByHospitalId(@PathVariable("hospitalId") String hospitalId);

    // ---------------------- Treatment APIs ----------------------
    @PostMapping("/clinic-admin/treatment/addTreatment")
    ResponseEntity<Response> addTreatment(@RequestBody TreatmentDTO dto);

    @GetMapping("/clinic-admin/treatment/getAllTreatments")
    ResponseEntity<Response> getAllTreatments();

    @GetMapping("/clinic-admin/treatment/getTreatmentById/{id}/{hospitalId}")
    ResponseEntity<Response> getTreatmentById(@PathVariable("id") String id,
                                              @PathVariable("hospitalId") String hospitalId);

    @DeleteMapping("/clinic-admin/treatment/deleteTreatmentById/{id}/{hospitalId}")
    ResponseEntity<Response> deleteTreatmentById(@PathVariable("id") String id,
                                                 @PathVariable("hospitalId") String hospitalId);

    @PutMapping("/clinic-admin/treatment/updateTreatmentById/{id}/{hospitalId}")
    ResponseEntity<Response> updateTreatmentById(@PathVariable("id") String id,
                                                 @PathVariable("hospitalId") String hospitalId,
                                                 @RequestBody TreatmentDTO dto);

    @GetMapping("/clinic-admin/treatments/{hospitalId}")
    ResponseEntity<ResponseStructure<List<TreatmentDTO>>> getTreatmentsByHospitalId(@PathVariable("hospitalId") String hospitalId);
    
    
 

    // ---------------------- Fallback Method ----------------------
    default ResponseEntity<?> clinicAdminServiceFallBack(Exception e) {
        return ResponseEntity.status(503).body(
                new Response(false, null, "CLINIC ADMIN SERVICE NOT AVAILABLE", 503, null, null, null, null, null, null)
        );
    }

    // ---------------------- Doctor Slot APIs (Added Last) ----------------------
    @PostMapping("/clinic-admin/addDoctorSlots/{hospitalId}/{branchId}/{doctorId}")
    ResponseEntity<Response> addDoctorSlot(@PathVariable("hospitalId") String hospitalId,
                                           @PathVariable("branchId") String branchId,
                                           @PathVariable("doctorId") String doctorId,
                                           @RequestBody DoctorSlotDTO slotDto);

    @GetMapping("/clinic-admin/getDoctorSlots/{hospitalId}/{branchId}/{doctorId}")
    ResponseEntity<Response> getDoctorSlots(@PathVariable("hospitalId") String hospitalId,
                                            @PathVariable("branchId") String branchId,
                                            @PathVariable("doctorId") String doctorId);
    
    
    @PutMapping("/clinic-admin/doctor/update-slot")
	public ResponseEntity<Response> updateDoctorSlot(@RequestBody UpdateSlotRequestDTO request) ;
	
    
    @GetMapping("/clinic-admin/getDoctorslots/{hospitalId}/{doctorId}")
	public ResponseEntity<Response> getDoctorSlot(@PathVariable String hospitalId, @PathVariable String doctorId);
    
    @DeleteMapping("/clinic-admin/doctorId/{doctorId}/branchId/{branchId}/date/{date}/slot/{slot}")
	public  ResponseEntity<Response>deleteDoctorSlot(
	        @PathVariable String doctorId,
	        @PathVariable String branchId,
	        @PathVariable String date,
	        @PathVariable String slot);
    
    
    @DeleteMapping("/clinic-admin/doctorId/{doctorId}/{date}/{slot}/slots")
	public Response deleteDoctorSlot(@PathVariable String doctorId, @PathVariable String date,
			@PathVariable String slot);
    
    @DeleteMapping("/clinic-admin/delete-by-date/{doctorId}/{date}")
	public ResponseEntity<Response> deleteDoctorSlotsByDate(@PathVariable String doctorId, @PathVariable String date);
    
    
    @DeleteMapping("/clinic-admin/delete-by-date/{doctorId}/{branchId}/{date}")
	public ResponseEntity<Response> deleteDoctorSlotsByDate(
	        @PathVariable String doctorId,
	        @PathVariable String branchId,
	        @PathVariable String date);
    
    
    
    @PutMapping("/clinic-admin/updateDoctorSlotWhileBooking/{doctorId}/{branchId}/{date}/{time}")
	public boolean updateDoctorSlotWhileBooking(@PathVariable String doctorId,@PathVariable String branchId, @PathVariable String date,
			@PathVariable String time);
    
    
    
    @PutMapping("/clinic-admin/makingFalseDoctorSlot/{doctorId}/{branchId}/{date}/{time}")
	public boolean makingFalseDoctorSlot(@PathVariable String doctorId,@PathVariable String branchId, @PathVariable String date,
			@PathVariable String time);
    
    
    @GetMapping("/clinic-admin/generateDoctorSlots/{doctorId}/{branchId}/{date}/{intervalMinutes}/{openingTime}/{closingTime}")
	public Response generateSlots(
	        @PathVariable String doctorId,
	        @PathVariable String branchId,
	        @PathVariable String date,
	        @PathVariable int intervalMinutes,
	        @PathVariable String openingTime,
	        @PathVariable String closingTime
	);
    
    
    @GetMapping("/clinic-admin/getDoctorSlots/{hospitalId}/{branchId}/{doctorId}")
	public ResponseEntity<Response> getDoctorSlot(
	        @PathVariable String hospitalId, 
	        @PathVariable String branchId,
	        @PathVariable String doctorId);
    
    
    ///////////////LAB TECHNICAN////////////
    ///
    ///
    @PostMapping("/clinic-admin/addLabTechnician")
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> createLabTechnician(
            @RequestBody LabTechnicianRequestDTO dto);
    
   
    
    @GetMapping("/clinic-admin/getAllLabTechnicians")
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getAllLabTechnicians();
    
    
    @PutMapping("/clinic-admin/updateById/{id}")
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> updateLabTechnician(
            @PathVariable String id,
            @RequestBody LabTechnicianRequestDTO dto);
    

    @DeleteMapping("/clinic-admin/deleteById/{id}")
    public ResponseEntity<ResponseStructure<String>> deleteLabTechnician(@PathVariable String id);
    
    
    @GetMapping("/clinic-admin/getLabTechniciansByClinicById/{clinicId}")
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinic(
            @PathVariable String clinicId);
    @GetMapping("/clinic-admin/getLabTechnicianByIdAndClinicId/{clinicId}/{technicianId}")
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> getLabTechnicianByClinicAndId(
            @PathVariable String clinicId,
            @PathVariable String technicianId);
    
    @GetMapping("/clinic-admin/getLabTechniciansByClinicIdAndBranchId/{clinicId}/{branchId}")
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinicAndBranch(
            @PathVariable String clinicId,
            @PathVariable String branchId);
    
    //////////////////Receptionist//////////////

    

        // ✅ Create Receptionist
        @PostMapping("/clinic-admin/createReceptionist")
        ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> createReceptionist(
                @RequestBody ReceptionistRequestDTO dto);

        // ✅ Get Receptionist by ID
        @GetMapping("/clinic-admin/getReceptionistById/{id}")
        ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistById(
                @PathVariable String id);

        // ✅ Get All Receptionists
        @GetMapping("/clinic-admin/getAllReceptionists")
        ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getAllReceptionists();

        // ✅ Update Receptionist by ID
        @PutMapping("/clinic-admin/updateReceptionist/{id}")
        ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> updateReceptionist(
                @PathVariable String id,
                @RequestBody ReceptionistRequestDTO dto);

        // ✅ Delete Receptionist by ID
        @DeleteMapping("/clinic-admin/deleteReceptionist/{id}")
        ResponseEntity<ResponseStructure<String>> deleteReceptionist(@PathVariable String id);

        // ✅ Get Receptionists by Clinic ID
        @GetMapping("/clinic-admin/receptionists/{clinicId}")
        ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinic(
                @PathVariable String clinicId);

        // ✅ Get Receptionist by Clinic ID and Receptionist ID
        @GetMapping("/clinic-admin/{clinicId}/{receptionistId}")
        ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistByClinicAndId(
                @PathVariable String clinicId,
                @PathVariable String receptionistId);

        // ✅ Get Receptionists by Clinic ID and Branch ID
        @GetMapping("/clinic-admin/getReceptionistsByClinicIdAndBranchId/{clinicId}/{branchId}")
        ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinicAndBranch(
                @PathVariable String clinicId,
                @PathVariable String branchId);
        
        @PostMapping("/clinic-admin/addNurse")
        ResponseEntity<Response> nurseOnBoarding(@RequestBody NurseDTO dto);


        // ✅ Get All Nurses by Hospital
        @GetMapping("/clinic-admin/getAllNurses/{hospitalId}")
        ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllByHospital(@PathVariable String hospitalId);

        // ✅ Get Nurse by Hospital and Nurse ID
        @GetMapping("/clinic-admin/getNurse/{hospitalId}/{nurseId}")
        ResponseEntity<ResponseStructure<NurseDTO>> getNurse(
                @PathVariable String hospitalId,
                @PathVariable String nurseId);

        // ✅ Update Nurse
        @PutMapping("/clinic-admin/updateNurse/{nurseId}")
        ResponseEntity<ResponseStructure<NurseDTO>> updateNurse(
                @PathVariable String nurseId,
                @RequestBody NurseDTO dto);

        // ✅ Delete Nurse
        @DeleteMapping("/clinic-admin/deleteNurse/{hospitalId}/{nurseId}")
        ResponseEntity<ResponseStructure<String>> deleteNurse(
                @PathVariable String hospitalId,
                @PathVariable String nurseId);

        // ✅ Get All Nurses by Hospital and Branch
        @GetMapping("/clinic-admin/getAllNursesByBranchIdAndHospiatlId/{hospitalId}/{branchId}")
        ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllNursesByBranch(
                @PathVariable String hospitalId,
                @PathVariable String branchId);
        
        
        /////////////////Pharmacist//////
        
        
   //// Pharmacist ////

     // ✅ Add Pharmacist
     @PostMapping("/clinic-admin/addPharmacist")
     ResponseEntity<ResponseStructure<PharmacistDTO>> pharmacistOnBoarding(@RequestBody PharmacistDTO dto);

     // ✅ Get All Pharmacists by Hospital
     @GetMapping("/clinic-admin/getAllPharmacists/{hospitalId}")
     ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getAllByDepartment(@PathVariable String hospitalId);

     // ✅ Get Single Pharmacist
     @GetMapping("/clinic-admin/getPharmacist/{pharmacistId}")
     ResponseEntity<ResponseStructure<PharmacistDTO>> getPharmacist(@PathVariable String pharmacistId);

     // ✅ Update Pharmacist
     @PutMapping("/clinic-admin/updatePharmacist/{pharmacistId}")
     ResponseEntity<ResponseStructure<PharmacistDTO>> updatePharmacist(
             @PathVariable String pharmacistId,
             @RequestBody PharmacistDTO dto);

     // ✅ Delete Pharmacist
     @DeleteMapping("/clinic-admin/deletePharmacist/{pharmacistId}")
     ResponseEntity<ResponseStructure<String>> deletePharmacist(@PathVariable String pharmacistId);

     // ✅ Get Pharmacists by Hospital and Branch
     @GetMapping("/clinic-admin/getPharmacistsByHospitalIdAndBranchId/{hospitalId}/{branchId}")
     ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getPharmacistsByHospitalIdAndBranchId(
             @PathVariable String hospitalId,
             @PathVariable String branchId);

        
        /////////////Security staff/////////////////////
       
     @PostMapping("/clinic-admin/addSecurityStaff")
     ResponseStructure<SecurityStaffDTO> addSecurityStaff(@RequestBody SecurityStaffDTO dto);

     @PutMapping("/clinic-admin/updateSecurityStaffById/{staffId}")
     ResponseStructure<SecurityStaffDTO> updateSecurityStaff(
             @PathVariable("staffId") String staffId,
             @RequestBody SecurityStaffDTO staffRequest);

     @GetMapping("/clinic-admin/getSecurityStaffById/{staffId}")
     ResponseStructure<SecurityStaffDTO> getSecurityStaffById(@PathVariable("staffId") String staffId);

     @GetMapping("/clinic-admin/getAllSecurityStaffByClinicId/{clinicId}")
     ResponseStructure<List<SecurityStaffDTO>> getAllByClinicId(@PathVariable("clinicId") String clinicId);

     @DeleteMapping("/clinic-admin/deleteSecurityStaffById/{staffId}")
     ResponseStructure<String> deleteSecurityStaff(@PathVariable("staffId") String staffId);

     @GetMapping("/clinic-admin/getSecurityStaffByClinicIdAndBranchId/{clinicId}/{branchId}")
     ResponseStructure<List<SecurityStaffDTO>> getSecurityStaffByClinicIdAndBranchId(
             @PathVariable("clinicId") String clinicId,
             @PathVariable("branchId") String branchId);
     
                          ///////WardBoy////////////////
     @PostMapping("/clinic-admin/addWardBoy")
     ResponseStructure<WardBoyDTO> addWardBoy(@RequestBody WardBoyDTO dto);

     @PutMapping("/clinic-admin/updateWardBoyById/{id}")
     ResponseStructure<WardBoyDTO> updateWardBoy(@PathVariable("id") String id, @RequestBody WardBoyDTO dto);

     @GetMapping("/clinic-admin/getWardBoyById/{id}")
     ResponseStructure<WardBoyDTO> getWardBoyById(@PathVariable("id") String id);

     @GetMapping("/clinic-admin/getAllWardBoys")
     ResponseStructure<List<WardBoyDTO>> getAllWardBoys();

     @GetMapping("/clinic-admin/getWardBoysByClinicId/{clinicId}")
     ResponseStructure<List<WardBoyDTO>> getWardBoysByClinicId(@PathVariable("clinicId") String clinicId);

     @GetMapping("/clinic-admin/getWardBoyByIdAndClinicId/{wardBoyId}/{clinicId}")
     ResponseStructure<WardBoyDTO> getWardBoyByIdAndClinicId(
             @PathVariable("wardBoyId") String wardBoyId,
             @PathVariable("clinicId") String clinicId);

     @DeleteMapping("/clinic-admin/deleteWardBoyById/{id}")
     ResponseStructure<Void> deleteWardBoy(@PathVariable("id") String id);

     @GetMapping("/clinic-admin/getWardBoysByClinicIdAndBranchId/{clinicId}/{branchId}")
     ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicIdAndBranchId(
             @PathVariable("clinicId") String clinicId,
             @PathVariable("branchId") String branchId
     );
     
///////////Administrator//////////
@PostMapping("/clinic-admin/addAdministrator")
ResponseStructure<AdministratorDTO> addAdministrator(@RequestBody AdministratorDTO dto);

@GetMapping("/clinic-admin/getAllAdministrators/{clinicId}")
ResponseStructure<List<AdministratorDTO>> getAllAdministratorsByClinic(@PathVariable String clinicId);

@GetMapping("/clinic-admin/getAllAdministrators/{clinicId}/{branchId}")
ResponseStructure<List<AdministratorDTO>> getAllAdministratorsByClinicAndBranch(@PathVariable String clinicId,
                                                                                @PathVariable String branchId);

@GetMapping("/clinic-admin/getAdministrator/{clinicId}/{adminId}")
ResponseStructure<AdministratorDTO> getAdministratorByClinicAndId(@PathVariable String clinicId,
                                                                  @PathVariable String adminId);

@GetMapping("/clinic-admin/getAdministrator/{clinicId}/{branchId}/{adminId}")
ResponseStructure<AdministratorDTO> getAdministratorByClinicBranchAndAdminId(@PathVariable String clinicId,
                                                                             @PathVariable String branchId,
                                                                             @PathVariable String adminId);

@PutMapping("/clinic-admin/updateAdministrator/{clinicId}/{adminId}")
ResponseStructure<AdministratorDTO> updateAdministrator(@PathVariable String clinicId,
                                                        @PathVariable String adminId,
                                                        @RequestBody AdministratorDTO dto);

@PutMapping("/clinic-admin/updateAdministrator/{clinicId}/{branchId}/{adminId}")
ResponseStructure<AdministratorDTO> updateAdministratorUsingClinicBranchAndAdminId(@PathVariable String clinicId,
                                                                                    @PathVariable String branchId,
                                                                                    @PathVariable String adminId,
                                                                                    @RequestBody AdministratorDTO dto);

@DeleteMapping("/clinic-admin/deleteAdministrator/{clinicId}/{adminId}")
ResponseStructure<String> deleteAdministrator(@PathVariable String clinicId,
                                              @PathVariable String adminId);

@DeleteMapping("/clinic-admin/deleteAdministrator/{clinicId}/{branchId}/{adminId}")
ResponseStructure<String> deleteAdministratorUsingClinicBranchAndAdminId(@PathVariable String clinicId,
                                                                         @PathVariable String branchId,
                                                                         @PathVariable String adminId);
//------------------Doctor Availability----------------------------------------------------------------------------------------
@PostMapping("clinic-admin/doctorId/{doctorId}/availability")
public ResponseEntity<Response> doctorAvailabilityStatus(@PathVariable String doctorId,
	@RequestBody DoctorAvailabilityStatusDTO status);
}
 
 
        
    
        
    

