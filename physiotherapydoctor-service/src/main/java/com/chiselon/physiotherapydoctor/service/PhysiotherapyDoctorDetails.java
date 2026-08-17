package com.chiselon.physiotherapydoctor.service;

import org.springframework.http.ResponseEntity;

import com.chiselon.physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
import com.chiselon.physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
import com.chiselon.physiotherapydoctor.dto.Response;

public interface PhysiotherapyDoctorDetails {

	 Response getPhysioDoctorDetails(String clinicId,String branchId);
	 
//	============From Doctor service ccms======================================= 
	 
//		Response registerDoctor(DoctorDTO doctorDTO);
//		Response changePassword(ChangeDoctorPasswordDTO updateDTO);
		Response changePassword(String username, ChangeDoctorPasswordDTO updateDTO);
		Response updateDoctorAvailability(String doctorId ,DoctorAvailabilityStatusDTO availabilityDTO);

		public ResponseEntity<?> getAllDoctors();
		public ResponseEntity<?> getDoctorById(String id);
		public ResponseEntity<?> getDoctorByClinicAndDoctorId(String clinicId,
				String doctorId);
		public ResponseEntity<?> getDoctorsByHospitalById(String clinicId);


//		public ResponseEntity<?> getDoctorsBySubServiceId(String hsptlId,String subServiceId);
//		public ResponseEntity<?> getAllDoctorsBySubServiceId(String subServiceId);
//
		public ResponseEntity<?> getDoctorFutureAppointments(String doctorId, int page);

		public ResponseEntity<Response> getDiseasesFromClinicAdmin(String hospitalId);
		//public ResponseEntity<Response> getLabTestsFromClinicAdmin(String hospitalId);

}
