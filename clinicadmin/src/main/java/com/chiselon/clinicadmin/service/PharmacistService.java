package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.PharmacistDTO;
import com.chiselon.clinicadmin.dto.Response;

public interface PharmacistService {
    Response pharmacistOnboarding(PharmacistDTO dto);
    Response getPharmacistById(String pharmacistId);
    Response updatePharmacist(String pharmacistId, PharmacistDTO dto);
    Response deletePharmacist(String pharmacistId);
//    Response pharmacistLogin(PharmacistLoginDTO loginDTO);
//    Response resetLoginPassword(ResetPharmacistLoginPasswordDTO dto);
	Response getAllPharmacistsByHospitalId(String hospitalId);
//	ResponseEntity<Response> getPrescriptionsByClinicId(String clinicId);
//	ResponseEntity<Response> deleteMedicine(String medicineId);
//	ResponseEntity<Response> deletePrescription(String id);
//	ResponseEntity<Response> searchMedicines(String keyword);
//	ResponseEntity<Response> getMedicineById(String medicineId);
//	ResponseEntity<Response> getPrescriptionById(String id);
//	ResponseEntity<Response> getAllPrescriptions();
//	ResponseEntity<Response> createPrescription(DoctorPrescriptionDTO dto);
//	ResponseEntity<Response> getMedicineTypes(String clinicId);
//	ResponseEntity<Response> searchOrAddMedicineType(MedicineTypeDTO dto);
//	ResponseEntity<Response> updateMedicine(String medicineId, MedicineDTO dto);
//	Response getPharmacistsByClinicAndBranch(String clinicId, String branchId);
	Response getPharmacistsByClinicIdAndBranchId(String clinicId, String branchId);
//	ResponseEntity<Response> getMedicineType(String Id);
//	ResponseEntity<Response> getAllMedicineTypes();
	
//	Response getPharmacistsByClinicIdAndBranchId(String clinicId, String branchId);


}