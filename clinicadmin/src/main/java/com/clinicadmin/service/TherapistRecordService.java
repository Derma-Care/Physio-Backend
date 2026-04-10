package com.clinicadmin.service;

import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.TherapistRecordDTO;

public interface TherapistRecordService {

    ResponseStructure<TherapistRecordDTO> saveRecord(TherapistRecordDTO dto);

    ResponseStructure<TherapistRecordDTO> getByIds(
            String clinicId, String branchId, String therapistRecordId,String sessionId);
}