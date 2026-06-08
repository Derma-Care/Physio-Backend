package com.dermacare.bookingService.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
	 ////public  List<Booking> findBySubServiceId(String subServiceId);
	 public  List<Booking> findByClinicId(String clinicId);
	// public Optional<Booking> findByBookingId(String bookingId);
	 @Query("{$or: [ { 'name': ?0 }, { 'bookingId': ?0 }, { 'patientId': ?0 } ]}")
	 public List<Booking> findByNameIgnoreCaseOrBookingIdOrPatientId(String input);
	 Page<Booking> findByNameIgnoreCaseOrBookingIdOrPatientId(
		        String input,
		        Pageable pageable
		);
	 public List<Booking> findByClinicIdAndDoctorId(String clinicId,String doctorId);
	 public List<Booking> findByPatientId(String patientId);
	Page<Booking> findByPatientId(String patientId, Pageable pageable);
	Page<Booking> findByClinicIdAndPatientId(String clinicId,String patientId, Pageable pageable);
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
	public List<Booking> findByServiceDate(String today);
	public List<Booking> findByClinicIdAndBranchIdAndServiceDate(String cId, String bId, String today);
	
	public List<Booking> findByClinicIdAndBranchIdAndServiceDateBetween(String clinicId, String branchId, String format,
			String format2);
	public List<Booking> findByClinicIdAndBranchIdAndServiceDateAndFollowupStatusIn(String clinicId, String branchId,
			String today, List<String> validStatus);
	public List<Booking> findByPatientIdAndBookingId(String patientId,String bookingId);
//	@Query("{ 'bookingId': { $regex: ?0, $options: 'i' } }")
	Optional<Booking> findByBookingIdIgnoreCase(String bookingId);
	
	public List<Booking> findByBookingIdIn(List<String> followup);
	@Query("{ 'clinicId': ?0, 'branchId': ?1, 'doctorId': ?2, "
		     + "'$or': [ "
		     + "{ 'status': ?3 }, "
		     + "{ 'followupStatus': ?3 } "
		     + "] }")
		List<Booking> findByStatusOrFollowupStatus(
		        String clinicId,
		        String branchId,
		        String doctorId,
		        String status);
	public List<Booking> findByClinicIdAndBranchIdAndDoctorIdAndStatusIgnoreCase(String clinicId, String branchId,
			String doctorId, String requiredStatus);
	public List<Booking> findByStatusOrFollowupStatusIgnoreCase(String clinicId, String branchId, String doctorId,
			String status);
	public List<Booking> findByClinicIdAndBranchIdAndDoctorIdAndFollowupStatusIgnoreCase(String clinicId,
			String branchId, String doctorId, String status);
	public List<Booking> findByClinicIdAndDoctorIdAndStatusIgnoreCase(String clinicId, String doctorId,
			String requiredStatus);
	public List<Booking> findByClinicIdAndDoctorIdAndFollowupStatusIgnoreCase(String clinicId, String doctorId,
			String status);
	public Page<Booking> findByClinicIdAndDoctorIdAndServiceDateAndStatusIgnoreCase(String clinicId, String doctorId,
			String todayDate, String string, Pageable pageable);
	public Page<Booking> findByMobileNumber(String mobileNumber, Pageable pageable);
	public Page<Booking> findByPatientIdAndBookingIdAndStatusIgnoreCase(String patientId, String bookingId,
			String string, Pageable pageable);
	public Page<Booking> findByMobileNumberAndStatusIgnoreCase(String number, String string, Pageable pageable);
	public Page<Booking> findByDoctorIdAndServiceDateBetween(String doctorId, String fromDate, String toDate,
			Pageable pageable);
	public Page<Booking> findByClinicIdAndBranchId(String clinicId, String branchId, Pageable pageable);
	public Page<Booking> findByClinicIdAndDoctorIdAndFollowupStatusIgnoreCase(String clinicId, String doctorId,
			String status, Pageable pageable);
	public Page<Booking> findByClinicIdAndDoctorIdAndStatusIgnoreCase(String clinicId, String doctorId, String status,
			Pageable pageable);
	public Page<Booking> findByClinicIdAndBranchIdAndDoctorIdAndFollowupStatusIgnoreCase(String clinicId,
			String branchId, String doctorId, String status, Pageable pageable);
	public Page<Booking> findByClinicIdAndBranchIdAndDoctorIdAndStatusIgnoreCase(String clinicId, String branchId,
			String doctorId, String status, Pageable pageable);
	public Page<Booking> findByClinicIdAndBranchIdAndServiceDate(String cId, String bId, String today,
			Pageable pageable);
	public Page<Booking> findByClinicIdAndBranchIdAndServiceDateBetween(String clinicId, String branchId, String format,
			String format2, Pageable pageable);
	public Page<Booking> findByDoctorId(String doctorId, Pageable pageable);
	public Page<Booking> findByBranchId(String branchId, Pageable pageable);
	public Page<Booking> findByClinicId(String clinicId, Pageable pageable);
	public Page<Booking> findByCustomerId(String customerId, Pageable pageable);
	//public Optional<Booking> findByBookingIdIgnoreCase(String bookingId);
	}

