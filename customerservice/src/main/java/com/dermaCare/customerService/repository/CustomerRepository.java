package com.dermaCare.customerService.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dermaCare.customerService.entity.Customer;

import java.util.List;
import java.util.Optional; // Import Optional

@Repository
public interface CustomerRepository extends MongoRepository<Customer, String> {
	
	public Optional<Customer> findByMobileNumber(long mobileNumber);
	
	public Optional<Customer> findByMobileNumber(String mobileNumber);
	
	public Optional<Customer> findByEmailId(String emailId);

	public List<Customer> findByfullName(String fullName);
	
	public Customer deleteByMobileNumber(String mobileNumber);
	
	 public Customer findFirstByOrderByIdDesc();
	 
	 boolean existsByCustomerId(String customerId); 
	 
	 public Customer findByEmailIdAndMobileNumber(String email,String mobileNumber);
	
	 public Customer findByDeviceId(String deviceId);


}