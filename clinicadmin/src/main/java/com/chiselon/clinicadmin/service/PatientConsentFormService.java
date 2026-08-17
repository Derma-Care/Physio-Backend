package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.PatientConsentFormDTO;
import com.chiselon.clinicadmin.dto.Response;

public interface PatientConsentFormService {

	public Response getPatientDetailsForFormUsingBooking(String bookingId, String patientId, String mobileNumber);
	public Response updatePatientConsentForm(String id, PatientConsentFormDTO dto);

}
