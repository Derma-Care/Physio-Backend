package com.chiselon.adminservice.feign;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.chiselon.adminservice.dto.AdministratorDTO;
import com.chiselon.adminservice.dto.DoctorAvailabilityStatusDTO;
import com.chiselon.adminservice.dto.DoctorSlotDTO;
import com.chiselon.adminservice.dto.DoctorsDTO;
import com.chiselon.adminservice.dto.LabTechnicianRequestDTO;
import com.chiselon.adminservice.dto.LabTestDTO;
import com.chiselon.adminservice.dto.NurseDTO;
import com.chiselon.adminservice.dto.PharmacistDTO;
import com.chiselon.adminservice.dto.ProbableDiagnosisDTO;
import com.chiselon.adminservice.dto.ReceptionistRequestDTO;
import com.chiselon.adminservice.dto.SecurityStaffDTO;
//import com.AdminService.dto.SubServicesDto;
import com.chiselon.adminservice.dto.TreatmentDTO;
import com.chiselon.adminservice.dto.UpdateSlotRequestDTO;
import com.chiselon.adminservice.dto.WardBoyDTO;
import com.chiselon.adminservice.util.Response;
import com.chiselon.adminservice.util.ResponseStructure;


@FeignClient(value = "clinicadmin")
public interface ClinicAdminFeign {

//    // ---------------------- Sub-Service APIs ----------------------
//    @GetMapping("/clinic-admin/subService/getAllSubServies")
//    ResponseEntity<ResponseStructure<List<SubServicesDto>>> getAllSubServices();

    // ---------------- Doctor CRUD ---------------- //
    @PostMapping("/clinic-admin/addDoctor")
    ResponseEntity<Response> addDoctor(@RequestHeader("Authorization") String token,@RequestBody DoctorsDTO dto);

    @GetMapping("/clinic-admin/doctors")
    ResponseEntity<Response> getAllDoctors(@RequestHeader("Authorization") String token);

    @GetMapping("/clinic-admin/doctor/{id}")
    ResponseEntity<Response> getDoctorById(@RequestHeader("Authorization") String token,@PathVariable("id") String id);

    @PutMapping("/clinic-admin/updateDoctor/{doctorId}")
    ResponseEntity<Response> updateDoctorById(@RequestHeader("Authorization") String token,@PathVariable("doctorId") String doctorId,
                                              @RequestBody DoctorsDTO dto);

    @DeleteMapping("/clinic-admin/delete-doctor/{doctorId}")
    ResponseEntity<Response> deleteDoctorById(@RequestHeader("Authorization") String token,@PathVariable("doctorId") String doctorId);

    @DeleteMapping("/clinic-admin/delete-doctors-by-clinic/{clinicId}")
    ResponseEntity<Response> deleteDoctorsByClinic(@RequestHeader("Authorization") String token,@PathVariable("clinicId") String clinicId);

    // ---------------- Additional Filters ---------------- //
    @GetMapping("/clinic-admin/clinic/{clinicId}/doctor/{doctorId}")
    ResponseEntity<Response> getDoctorByClinicAndDoctorId(@RequestHeader("Authorization") String token,@PathVariable("clinicId") String clinicId,
                                                          @PathVariable("doctorId") String doctorId);

    @GetMapping("/clinic-admin/doctors/hospitalById/{hospitalId}")
    ResponseEntity<Response> getDoctorsByHospitalId(@RequestHeader("Authorization") String token,@PathVariable("hospitalId") String hospitalId);

    @GetMapping("/clinic-admin/getDoctorsByHospitalIdAndBranchId/{hospitalId}/{branchId}")
    ResponseEntity<Response> getDoctorsByHospitalIdAndBranchId(@RequestHeader("Authorization") String token,@PathVariable("hospitalId") String hospitalId,
                                                               @PathVariable("branchId") String branchId);

    // ---------------------- Disease APIs ----------------------
    @PostMapping("/clinic-admin/addDiseases")
    ResponseEntity<Response> addDiseases(@RequestHeader("Authorization") String token,@RequestBody Object requestBody);

    @GetMapping("/clinic-admin/get-all-diseases")
    ResponseEntity<Response> getAllDiseases(@RequestHeader("Authorization") String token);

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
    

    // ---------------------- Doctor Slot APIs (Added Last) ----------------------
    @PostMapping("/clinic-admin/addDoctorSlots/{hospitalId}/{branchId}/{doctorId}")
    ResponseEntity<Response> addDoctorSlot(@RequestHeader("Authorization") String token,@PathVariable("hospitalId") String hospitalId,
                                           @PathVariable("branchId") String branchId,
                                           @PathVariable("doctorId") String doctorId,
                                           @RequestBody DoctorSlotDTO slotDto);

    @GetMapping("/clinic-admin/getDoctorSlots/{hospitalId}/{branchId}/{doctorId}")
    ResponseEntity<Response> getDoctorSlots(@RequestHeader("Authorization") String token,@PathVariable("hospitalId") String hospitalId,
                                            @PathVariable("branchId") String branchId,
                                            @PathVariable("doctorId") String doctorId);
    
    
    @PutMapping("/clinic-admin/doctor/update-slot")
	public ResponseEntity<Response> updateDoctorSlot(@RequestHeader("Authorization") String token,@RequestBody UpdateSlotRequestDTO request) ;
	
    
//    @GetMapping("/clinic-admin/getDoctorslots/{hospitalId}/{doctorId}")
//	public ResponseEntity<Response> getDoctorSlot(@RequestHeader String token,@PathVariable String hospitalId, @PathVariable String doctorId);
//    
    @DeleteMapping("/clinic-admin/doctorId/{doctorId}/branchId/{branchId}/date/{date}/slot/{slot}")
	public  ResponseEntity<Response>deleteDoctorSlot(@RequestHeader("Authorization") String token,
	        @PathVariable String doctorId,
	        @PathVariable String branchId,
	        @PathVariable String date,
	        @PathVariable String slot);
    
    
    @DeleteMapping("/clinic-admin/doctorId/{doctorId}/{date}/{slot}/slots")
	public Response deleteDoctorSlot(@RequestHeader("Authorization") String token,@PathVariable String doctorId, @PathVariable String date,
			@PathVariable String slot);
    
//    @DeleteMapping("/clinic-admin/delete-by-date/{doctorId}/{date}")
//	public ResponseEntity<Response> deleteDoctorSlotsByDate(@PathVariable String doctorId, @PathVariable String date);
//    
    
    @DeleteMapping("/clinic-admin/delete-by-date/{doctorId}/{branchId}/{date}")
	public ResponseEntity<Response> deleteDoctorSlotsByDate(@RequestHeader("Authorization") String token,
	        @PathVariable String doctorId,
	        @PathVariable String branchId,
	        @PathVariable String date);
    
    
    
    @PutMapping("/clinic-admin/updateDoctorSlotWhileBooking/{doctorId}/{branchId}/{date}/{time}")
	public boolean updateDoctorSlotWhileBooking(@RequestHeader("Authorization") String token,@PathVariable String doctorId,@PathVariable String branchId, @PathVariable String date,
			@PathVariable String time);
    
    
    
    @PutMapping("/clinic-admin/makingFalseDoctorSlot/{doctorId}/{branchId}/{date}/{time}")
	public boolean makingFalseDoctorSlot(@RequestHeader("Authorization") String token,@PathVariable String doctorId,@PathVariable String branchId, @PathVariable String date,
			@PathVariable String time);
    
    
    @GetMapping("/clinic-admin/generateDoctorSlots/{doctorId}/{branchId}/{date}/{intervalMinutes}/{openingTime}/{closingTime}")
	public Response generateSlots(@RequestHeader("Authorization") String token,
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
        		@RequestHeader("Authorization") String token,    @RequestBody ReceptionistRequestDTO dto);

        // ✅ Get Receptionist by ID
        @GetMapping("/clinic-admin/getReceptionistById/{id}")
        ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistById(
        		@RequestHeader("Authorization") String token,  @PathVariable String id);

        // ✅ Get All Receptionists
        @GetMapping("/clinic-admin/getAllReceptionists")
        ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getAllReceptionists(@RequestHeader("Authorization") String token);

        // ✅ Update Receptionist by ID
        @PutMapping("/clinic-admin/updateReceptionist/{id}")
        ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> updateReceptionist(
        		@RequestHeader("Authorization") String token,     @PathVariable String id,
                @RequestBody ReceptionistRequestDTO dto);

        // ✅ Delete Receptionist by ID
        @DeleteMapping("/clinic-admin/deleteReceptionist/{id}")
        ResponseEntity<ResponseStructure<String>> deleteReceptionist(@RequestHeader("Authorization") String token,   @PathVariable String id);

        // ✅ Get Receptionists by Clinic ID
        @GetMapping("/clinic-admin/receptionists/{clinicId}")
        ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinic(@RequestHeader("Authorization") String token,
                @PathVariable String clinicId);

        // ✅ Get Receptionist by Clinic ID and Receptionist ID
        @GetMapping("/clinic-admin/{clinicId}/{receptionistId}")
        ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistByClinicAndId(
        		@RequestHeader("Authorization") String token,  @PathVariable String clinicId,
                @PathVariable String receptionistId);

        // ✅ Get Receptionists by Clinic ID and Branch ID
        @GetMapping("/clinic-admin/getReceptionistsByClinicIdAndBranchId/{clinicId}/{branchId}")
        ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinicAndBranch(
        		@RequestHeader("Authorization") String token,     @PathVariable String clinicId,
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
     ResponseStructure<SecurityStaffDTO> addSecurityStaff(@RequestHeader("Authorization") String token,@RequestBody SecurityStaffDTO dto);

     @PutMapping("/clinic-admin/updateSecurityStaffById/{staffId}")
     ResponseStructure<SecurityStaffDTO> updateSecurityStaff(
    		 @RequestHeader("Authorization") String token,    @PathVariable("staffId") String staffId,
             @RequestBody SecurityStaffDTO staffRequest);

     @GetMapping("/clinic-admin/getSecurityStaffById/{staffId}")
     ResponseStructure<SecurityStaffDTO> getSecurityStaffById(@RequestHeader("Authorization") String token,@PathVariable("staffId") String staffId);

     @GetMapping("/clinic-admin/getAllSecurityStaffByClinicId/{clinicId}")
     ResponseStructure<List<SecurityStaffDTO>> getAllByClinicId(@RequestHeader("Authorization") String token,@PathVariable("clinicId") String clinicId);

     @DeleteMapping("/clinic-admin/deleteSecurityStaffById/{staffId}")
     ResponseStructure<String> deleteSecurityStaff(@RequestHeader("Authorization") String token,@PathVariable("staffId") String staffId);

     @GetMapping("/clinic-admin/getSecurityStaffByClinicIdAndBranchId/{clinicId}/{branchId}")
     ResponseStructure<List<SecurityStaffDTO>> getSecurityStaffByClinicIdAndBranchId(
    		 @RequestHeader("Authorization") String token, @PathVariable("clinicId") String clinicId,
             @PathVariable("branchId") String branchId);
     
                          ///////WardBoy////////////////
     @PostMapping("/clinic-admin/addWardBoy")
     ResponseStructure<WardBoyDTO> addWardBoy(@RequestHeader("Authorization") String token,@RequestBody WardBoyDTO dto);

     @PutMapping("/clinic-admin/updateWardBoyById/{id}")
     ResponseStructure<WardBoyDTO> updateWardBoy(@RequestHeader("Authorization") String token,@PathVariable("id") String id, @RequestBody WardBoyDTO dto);

     @GetMapping("/clinic-admin/getWardBoyById/{id}")
     ResponseStructure<WardBoyDTO> getWardBoyById(@RequestHeader("Authorization") String token,@PathVariable("id") String id);

     @GetMapping("/clinic-admin/getAllWardBoys")
     ResponseStructure<List<WardBoyDTO>> getAllWardBoys(@RequestHeader("Authorization") String token);

     @GetMapping("/clinic-admin/getWardBoysByClinicId/{clinicId}")
     ResponseStructure<List<WardBoyDTO>> getWardBoysByClinicId(@RequestHeader("Authorization") String token,@PathVariable("clinicId") String clinicId);

     @GetMapping("/clinic-admin/getWardBoyByIdAndClinicId/{wardBoyId}/{clinicId}")
     ResponseStructure<WardBoyDTO> getWardBoyByIdAndClinicId(
    		 @RequestHeader("Authorization") String token,     @PathVariable("wardBoyId") String wardBoyId,
             @PathVariable("clinicId") String clinicId);

     @DeleteMapping("/clinic-admin/deleteWardBoyById/{id}")
     ResponseStructure<Void> deleteWardBoy(@RequestHeader("Authorization") String token,@PathVariable("id") String id);

     @GetMapping("/clinic-admin/getWardBoysByClinicIdAndBranchId/{clinicId}/{branchId}")
     ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicIdAndBranchId(
    		 @RequestHeader("Authorization") String token,    @PathVariable("clinicId") String clinicId,
             @PathVariable("branchId") String branchId
     );
     
///////////Administrator//////////
@PostMapping("/clinic-admin/addAdministrator")
ResponseStructure<AdministratorDTO> addAdministrator(@RequestHeader("Authorization") String token,@RequestBody AdministratorDTO dto);

@GetMapping("/clinic-admin/getAllAdministrators/{clinicId}")
ResponseStructure<List<AdministratorDTO>> getAllAdministratorsByClinic(@RequestHeader("Authorization") String token,@PathVariable String clinicId);

@GetMapping("/clinic-admin/getAllAdministrators/{clinicId}/{branchId}")
ResponseStructure<List<AdministratorDTO>> getAllAdministratorsByClinicAndBranch(@RequestHeader("Authorization") String token,@PathVariable String clinicId,
                                                                                @PathVariable String branchId);

@GetMapping("/clinic-admin/getAdministrator/{clinicId}/{adminId}")
ResponseStructure<AdministratorDTO> getAdministratorByClinicAndId(@RequestHeader("Authorization") String token,@PathVariable String clinicId,
                                                                  @PathVariable String adminId);

@GetMapping("/clinic-admin/getAdministrator/{clinicId}/{branchId}/{adminId}")
ResponseStructure<AdministratorDTO> getAdministratorByClinicBranchAndAdminId(@RequestHeader("Authorization") String token,@PathVariable String clinicId,
                                                                             @PathVariable String branchId,
                                                                             @PathVariable String adminId);

@PutMapping("/clinic-admin/updateAdministrator/{clinicId}/{adminId}")
ResponseStructure<AdministratorDTO> updateAdministrator(@RequestHeader("Authorization") String token,@PathVariable String clinicId,
                                                        @PathVariable String adminId,
                                                        @RequestBody AdministratorDTO dto);

@PutMapping("/clinic-admin/updateAdministrator/{clinicId}/{branchId}/{adminId}")
ResponseStructure<AdministratorDTO> updateAdministratorUsingClinicBranchAndAdminId(@RequestHeader("Authorization") String token,@PathVariable String clinicId,
                                                                                    @PathVariable String branchId,
                                                                                    @PathVariable String adminId,
                                                                                    @RequestBody AdministratorDTO dto);

@DeleteMapping("/clinic-admin/deleteAdministrator/{clinicId}/{adminId}")
ResponseStructure<String> deleteAdministrator(@RequestHeader("Authorization") String token,@PathVariable String clinicId,
                                              @PathVariable String adminId);

@DeleteMapping("/clinic-admin/deleteAdministrator/{clinicId}/{branchId}/{adminId}")
ResponseStructure<String> deleteAdministratorUsingClinicBranchAndAdminId(@RequestHeader("Authorization") String token,@PathVariable String clinicId,
                                                                         @PathVariable String branchId,
                                                                         @PathVariable String adminId);
//------------------Doctor Availability----------------------------------------------------------------------------------------
@PostMapping("clinic-admin/doctorId/{doctorId}/availability")
public ResponseEntity<Response> doctorAvailabilityStatus(@RequestHeader("Authorization") String token,@PathVariable String doctorId,
	@RequestBody DoctorAvailabilityStatusDTO status);


//    @PutMapping("/clinic-admin/updateDoctorSlotWhileBooking/{doctorId}/{branchId}/{date}/{time}")
//    public boolean updateDoctorSlotWhileBooking(@PathVariable String doctorId,@PathVariable String branchId, @PathVariable String date,
//                                                @PathVariable String time);

 
// ---------------------- Fallback Method ----------------------
default ResponseEntity<?> clinicAdminServiceFallBack(Exception e) {
    return ResponseEntity.status(503).body(
            new Response(false, null, "CLINIC ADMIN SERVICE NOT AVAILABLE", 503,null,null,null, null,null)
    );
}}
 
        
    
        
    

