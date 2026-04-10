package com.clinicadmin.service.impl;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.MedicineDetailDTO;
import com.clinicadmin.dto.MedicineInventoryDto;
import com.clinicadmin.dto.Response;
import com.clinicadmin.feignclient.PharmacyManagementFeignClient;
import com.clinicadmin.service.MedicineDetailService;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MedicineDetailServiceImpl implements MedicineDetailService {

    private final PharmacyManagementFeignClient pharmacyFeignClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Response addMedicine(MedicineDetailDTO dto) {
        try {
            return pharmacyFeignClient.addMedicine(dto);
        } catch (FeignException ex) {
            return extractFeignError(ex);
        }
    }

    @Override
    public Response getMedicineById(String id) {
        try {
            return pharmacyFeignClient.getMedicineById(id);
        } catch (FeignException ex) {
            return extractFeignError(ex);
        }
    }

    @Override
    public Response getAllMedicines() {
        try {
            return pharmacyFeignClient.getAllMedicines();
        } catch (FeignException ex) {
            return extractFeignError(ex);
        }
    }

    @Override
    public Response updateMedicine(String id, MedicineDetailDTO dto) {
        try {
            return pharmacyFeignClient.updateMedicine(id, dto);
        } catch (FeignException ex) {
            return extractFeignError(ex);
        }
    }

    @Override
    public Response deleteMedicine(String id) {
        try {
            return pharmacyFeignClient.deleteMedicine(id);
        } catch (FeignException ex) {
            return extractFeignError(ex);
        }
    }
    
    
    public ResponseEntity<Response> fetchMedicineInventory(String medicineId){
    	ResponseEntity<MedicineInventoryDto> respns = null;
    	Response res = new Response();
    	try {
    		respns = pharmacyFeignClient.getMedicineInventory(medicineId);
    		if(respns != null) {
    			res.setMessage("MedicineInventory retrieved successfully");
    			res.setStatus(200);
    			res.setSuccess(true);
    			res.setData(respns.getBody());
    		}else {
    			res.setMessage("MedicineInventory not found");
    			res.setStatus(404);
    			res.setSuccess(false);
    		}}catch(FeignException e) {}
    	return ResponseEntity.status(res.getStatus()).body(res);
    	
    }

    private Response extractFeignError(FeignException ex) {
        try {
            return objectMapper.readValue(ex.contentUTF8(), Response.class);
        } catch (Exception parseEx) {
            Response fallback = new Response();
            fallback.setStatus(500);
            fallback.setMessage("Unexpected error from pharmacy service: " + ex.getMessage());
            fallback.setData(null);
            return fallback;
        }
    }
    
    
}
