package com.chiselon.physiotherapydoctor.service;

import com.chiselon.physiotherapydoctor.dto.MedicineTypeDTO;
import com.chiselon.physiotherapydoctor.dto.Response;

public interface MedicineTypeService {
	Response addMedicineType(MedicineTypeDTO dto);

//    Response getMedicineTypesByClinicId(String clinicId);
	Response searchOrAddMedicineType(MedicineTypeDTO dto);

	Response getMedicineTypesById(String id);

	Response getAllMedicineTypes();
}
