package com.dermacare.doctorservice.service;



import org.springframework.http.ResponseEntity;

public interface BookingService {

    ResponseEntity<?> getAppointmentsByPatientId(String patientId);

    ResponseEntity<?> searchAppointmentsByInput(String input);

    ResponseEntity<?> getTodaysAppointments(String clinicId, String doctorId);

    ResponseEntity<?> getFilteredAppointments(String clinicId, String doctorId, String number);

    ResponseEntity<?> getCompletedAppointments(String clinicId, String doctorId);

    ResponseEntity<?> getConsultationTypeCounts(String clinicId, String doctorId);

	ResponseEntity<?> getInProgressAppointments(String mobileNumber);

	ResponseEntity<?> getAllBookedServicesByDoctorId(String doctorId);

	ResponseEntity<?> getDoctorFutureAppointments(String doctorId);
}

