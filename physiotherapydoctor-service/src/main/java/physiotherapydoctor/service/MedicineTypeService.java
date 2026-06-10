package physiotherapydoctor.service;

import physiotherapydoctor.dto.MedicineTypeDTO;
import physiotherapydoctor.dto.Response;

public interface MedicineTypeService {
	Response addMedicineType(MedicineTypeDTO dto);

//    Response getMedicineTypesByClinicId(String clinicId);
	Response searchOrAddMedicineType(MedicineTypeDTO dto);

	Response getMedicineTypesById(String id);

	Response getAllMedicineTypes();
}
