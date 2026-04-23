package com.clinicadmin.service;

import java.util.List;

import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.TherapistRecordDTO;

public interface TherapistRecordService {

    ResponseStructure<TherapistRecordDTO> saveRecord(TherapistRecordDTO dto);

    ResponseStructure<TherapistRecordDTO> getByIds(
            String clinicId, String branchId, String therapistRecordId,String sessionId);

	ResponseStructure<List<TherapistRecordDTO>> getByPatientIdAndBookingId(String patientId, String bookingId);

}