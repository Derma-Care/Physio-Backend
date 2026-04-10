package com.dermacare.doctorservice.service;

import org.springframework.http.ResponseEntity;

import com.dermacare.doctorservice.dto.ChangeDoctorPasswordDTO;
import com.dermacare.doctorservice.dto.DoctorAvailabilityStatusDTO;
import com.dermacare.doctorservice.dto.DoctorLoginDTO;
import com.dermacare.doctorservice.dto.Response;

public interface DoctorService {
   
    Response login(DoctorLoginDTO loginDTO);
//	Response registerDoctor(DoctorDTO doctorDTO);
//	Response changePassword(ChangeDoctorPasswordDTO updateDTO);
	Response changePassword(String username, ChangeDoctorPasswordDTO updateDTO);
	Response updateDoctorAvailability(String doctorId ,DoctorAvailabilityStatusDTO availabilityDTO);

	public ResponseEntity<?> getAllDoctors();
	public ResponseEntity<?> getDoctorById(String id);
	public ResponseEntity<?> getDoctorByClinicAndDoctorId(String clinicId,
			String doctorId);
	public ResponseEntity<?> getDoctorsByHospitalById(String clinicId);
	public ResponseEntity<?> getDoctorsBySubServiceId(String hsptlId,String subServiceId);
	public ResponseEntity<?> getAllDoctorsBySubServiceId(String subServiceId);
	public ResponseEntity<?> getDoctorFutureAppointments(String doctorId);
	public ResponseEntity<Response> getDiseasesFromClinicAdmin(String hospitalId);
	public ResponseEntity<Response> getLabTestsFromClinicAdmin(String hospitalId);

}
