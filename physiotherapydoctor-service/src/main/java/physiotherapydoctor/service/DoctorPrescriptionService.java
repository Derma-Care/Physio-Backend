package physiotherapydoctor.service;

import physiotherapydoctor.dto.DoctorPrescriptionDTO;
import physiotherapydoctor.dto.MedicineDTO;
import physiotherapydoctor.dto.Response;

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
