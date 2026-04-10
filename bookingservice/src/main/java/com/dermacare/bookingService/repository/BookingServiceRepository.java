package com.dermacare.bookingService.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import com.dermacare.bookingService.dto.BookingResponse;
import com.dermacare.bookingService.entity.Booking;

@Repository
public interface BookingServiceRepository extends MongoRepository<Booking,String> {

	 public  List<Booking> findByMobileNumber(String mobileNumber);
	 public  List<Booking> findByDoctorId(String doctorId);
	 public  List<Booking> findByBranchId(String branchId);
	 public  List<Booking> findBySubServiceId(String subServiceId);
	 public  List<Booking> findByClinicId(String clinicId);
	 public Optional<Booking> findByBookingId(String bookingId);
	 @Query("{$or: [ { 'name': ?0 }, { 'bookingId': ?0 }, { 'patientId': ?0 } ]}")
	 public List<Booking> findByNameIgnoreCaseOrBookingIdOrPatientId(String input);
	 public List<Booking> findByClinicIdAndDoctorId(String clinicId,String doctorId);
	 public List<Booking> findByPatientId(String patientId);
	public List<Booking> findByRelationIgnoreCaseAndCustomerIdAndNameIgnoreCase(String relation, String customerId,String name);
	public Optional<Booking> findByBookingIdAndPatientIdAndMobileNumber(String bookingId, String patientId,
			String mobileNumber);  
	public Booking findByMobileNumberAndPatientIdAndBookingId(String mobileNumber, String patientId,String bid);
	public List<Booking> findByClinicIdAndBranchId(String clinicId, String branchId);
	public List<Booking> findByClinicIdAndBranchIdAndServiceDateOrderByServicetimeAsc(String clinicId,String branchId,String serviceDate);
	public List<Booking> findByCustomerId(String customerId);
	public Booking findByPatientIdAndFollowupDate(String pId,String followupdate);
	public List<Booking> findByNameIgnoreCase(String input);
	public Booking findByServiceDateAndServicetimeAndDoctorId(String date, String time, String doctorId);
    
	public List<Booking> findByCustomerIdAndClinicId(String customerId,String clinicId);
	public List<Booking> findByNameContainingIgnoreCaseAndClinicId(String input,String clinicId);
	public List<Booking> findByPatientIdAndClinicId(String patientId,String clinicId);
	public  List<Booking> findByMobileNumberAndClinicId(String mobileNumber,String clinicId);
	public List<Booking> findByCustomerIdAndStatusIgnoreCase(String customerId, String string);
}
