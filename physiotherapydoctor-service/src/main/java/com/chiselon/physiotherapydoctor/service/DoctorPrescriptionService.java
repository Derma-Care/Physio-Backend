package com.chiselon.physiotherapydoctor.service;

import com.chiselon.physiotherapydoctor.dto.DoctorPrescriptionDTO;
import com.chiselon.physiotherapydoctor.dto.MedicineDTO;
import com.chiselon.physiotherapydoctor.dto.Response;

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
