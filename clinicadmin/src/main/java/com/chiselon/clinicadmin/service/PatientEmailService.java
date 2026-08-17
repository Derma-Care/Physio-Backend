package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.PatientEmailDTO;

public interface PatientEmailService {

    void sendPatientEmail(
            PatientEmailDTO dto);
}