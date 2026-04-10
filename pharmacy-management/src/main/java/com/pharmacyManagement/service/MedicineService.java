package com.pharmacyManagement.service;

import com.pharmacyManagement.dto.MedicineDTO;
import com.pharmacyManagement.dto.MedicineInventoryDto;
import com.pharmacyManagement.dto.Response;

public interface MedicineService {

    Response addMedicine(MedicineDTO dto);

    Response updateMedicine(String id, MedicineDTO dto);

    Response getAllMedicines();

    Response getMedicineById(String id);

    Response deleteMedicine(String id);
    
    Response getMedicineByBarcode(String barcode);
    
    public MedicineInventoryDto getMedicineInventory(String medicineId);

}