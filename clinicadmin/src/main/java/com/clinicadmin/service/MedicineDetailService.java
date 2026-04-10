package com.clinicadmin.service;

import org.springframework.http.ResponseEntity;

import com.clinicadmin.dto.MedicineDetailDTO;
import com.clinicadmin.dto.Response;

public interface MedicineDetailService {

    Response addMedicine(MedicineDetailDTO dto);

    Response getMedicineById(String id);

    Response getAllMedicines();

    Response updateMedicine(String id, MedicineDetailDTO dto);

    Response deleteMedicine(String id);
    
    public ResponseEntity<Response> fetchMedicineInventory(String medicineId);
    	   
}
