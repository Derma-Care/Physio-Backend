package com.dermacare.doctorservice.service;

import com.dermacare.doctorservice.dto.DoctorSaveDetailsDTO;
import com.dermacare.doctorservice.dto.Response;

public interface DoctorSaveDetailsService {

    Response saveDoctorDetails(DoctorSaveDetailsDTO dto);

    Response getDoctorDetailsById(String id);

    Response updateDoctorDetails(String id, DoctorSaveDetailsDTO dto);

    Response deleteDoctorDetails(String id);

    Response getAllDoctorDetails();

	Response getVisitHistoryByPatientAndBooking(String patientId, String bookingId);

//	Response getRevisitPrescriptions(String patientId, String bookingId);
	Response getVisitHistoryByPatient(String patientId);

	Response getVisitHistoryByPatientAndDoctor(String patientId, String doctorId);

	Response getInProgressDetails(String patientId, String bookingId);
    Response getDoctorDetailsByBookingId(String bookingId);

	Response getDoctorDetailsByCustomerId(String customerId);
	
	public Response updateDoctorDetailsByBookingId(String id, DoctorSaveDetailsDTO dto);
	 
	 public DoctorSaveDetailsDTO getDoctorLatestDetailsByCustomerId(String customerId);
		    
	
}
