package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.PatientMessageDTO;
import com.chiselon.clinicadmin.dto.Response;

public interface PatientMessageService {

    Response savePatientMessage(
            PatientMessageDTO dto
    );
}