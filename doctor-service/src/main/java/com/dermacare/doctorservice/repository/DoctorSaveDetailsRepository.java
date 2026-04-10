package com.dermacare.doctorservice.repository;


import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.dermacare.doctorservice.model.DoctorSaveDetails;

@Repository
public interface DoctorSaveDetailsRepository extends MongoRepository<DoctorSaveDetails, String> {

	List<DoctorSaveDetails> findByPatientId(String patientId);
	List<DoctorSaveDetails> findByPatientIdAndBookingId(String patientId, String bookingId);
	DoctorSaveDetails findByBookingId(String bookingId);
	List<DoctorSaveDetails> findByCustomerId(String customerId);
	List<DoctorSaveDetails> findByDoctorIdAndPatientId(String doctorId, String patientId);
	List<DoctorSaveDetails> findByDoctorIdAndPatientIdAndSubServiceId(
	        String doctorId,
	        String patientId,
	        String subServiceId
	);
	
	}
