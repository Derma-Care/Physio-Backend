package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.ReferredDoctorDTO;
import com.chiselon.clinicadmin.dto.Response;

public interface ReferredDoctorService {

    Response addReferralDoctor(ReferredDoctorDTO dto);

    Response getDoctorByReferralId(String referralId);

    Response getAllReferralDoctor();

	Response deleteReferralDoctoById(String id);

	Response updateReferralDoctorById(String id, ReferredDoctorDTO dto);

	Response getReferralDoctorrById(String id);

	Response getReferralDoctorsByClinicId(String clinicId);
}