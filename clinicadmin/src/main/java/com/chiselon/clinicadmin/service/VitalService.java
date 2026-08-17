package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.dto.VitalsDTO;

public interface VitalService {

//	public Response postVitalsById(String bookingId, VitalsDTO dto);

	public Response updateVitals(String bookingId, String patientId, VitalsDTO dto);

	public Response deleteVitals(String bookingId, String patiendId);

	Response getPatientByBookingIdAndPatientId(String bookingId, String patientId);

	Response postVitals(String bookingId, VitalsDTO dto);

}