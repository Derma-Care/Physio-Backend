package com.dermaCare.customerService.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dermaCare.customerService.entity.CustomerRating;

@Repository
public interface CustomerRatingRepository extends MongoRepository<CustomerRating, ObjectId> {
	
	List<CustomerRating> findByBranchIdAndDoctorId(String bId, String doctorId);
	List<CustomerRating> findByHospitalId(String clinicId);
	List<CustomerRating> findByBranchId(String bId);
	List<CustomerRating> findByDoctorId(String doctorId); 	
	CustomerRating findByHospitalIdAndDoctorIdAndCustomerMobileNumberAndPatientIdAndAppointmentId(String hid,String did,String cnum,String pid,String aid);
	CustomerRating findByBranchIdAndDoctorIdAndAppointmentId(String hid,String did,String aid);
}
