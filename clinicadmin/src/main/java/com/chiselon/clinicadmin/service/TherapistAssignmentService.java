package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.dto.TherapistAssignmentDTO;

public interface TherapistAssignmentService {

    Response assignTherapist(
            TherapistAssignmentDTO dto);

    Response getAssignedTherapistDetails(
            String therapistRecordId);

	Response updateAssignedStatus(String therapistRecordId, TherapistAssignmentDTO dto);



}