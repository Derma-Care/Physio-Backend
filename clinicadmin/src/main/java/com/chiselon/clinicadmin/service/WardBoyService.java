package com.chiselon.clinicadmin.service;

import java.util.List;

import com.chiselon.clinicadmin.dto.ResponseStructure;
import com.chiselon.clinicadmin.dto.WardBoyDTO;

public interface WardBoyService {
    
    ResponseStructure<WardBoyDTO> addWardBoy(WardBoyDTO dto);
    
    ResponseStructure<WardBoyDTO> getWardBoyById(String id);
    
    ResponseStructure<List<WardBoyDTO>> getAllWardBoys();
    
    ResponseStructure<WardBoyDTO> updateWardBoy(String id, WardBoyDTO dto);
    
    ResponseStructure<Void> deleteWardBoy(String id);

	ResponseStructure<List<WardBoyDTO>> getWardBoysByClinicId(String clinicId);

	ResponseStructure<WardBoyDTO> getWardBoyByIdAndClinicId(String wardBoyId, String clinicId);

	ResponseStructure<List<WardBoyDTO>> getWardBoysByClinicIdAndBranchId(String clinicId, String branchId);

}