package com.dermacare.doctorservice.service;

import com.dermacare.doctorservice.dto.MedicineTypeDTO;
import com.dermacare.doctorservice.dto.Response;

public interface MedicineTypeService {
    Response addMedicineType(MedicineTypeDTO dto);
//    Response getMedicineTypesByClinicId(String clinicId);
	Response searchOrAddMedicineType(MedicineTypeDTO dto);
	Response getMedicineTypesById(String id);
	Response getAllMedicineTypes();
}
