// DoctorPrescriptionService.java
package com.dermacare.doctorservice.service;

import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.dto.DoctorPrescriptionDTO;
import com.dermacare.doctorservice.dto.MedicineDTO;

public interface DoctorPrescriptionService {
    Response createPrescription(DoctorPrescriptionDTO dto);
    Response getAllPrescriptions();
    Response getPrescriptionById(String id);
    Response getMedicineById(String medicineId);
    Response deletePrescription(String id);
    Response searchMedicinesByName(String keyword);
    Response deleteMedicineById(String medicineId);
    Response getPrescriptionsByClinicId(String clinicId);
	Response updatePrescription(String id, DoctorPrescriptionDTO dto);
	Response updateMedicineById(String medicineId, MedicineDTO dto);

}
