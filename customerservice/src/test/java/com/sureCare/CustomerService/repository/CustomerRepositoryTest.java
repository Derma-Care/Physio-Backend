package com.sureCare.CustomerService.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import com.dermaCare.customerService.entity.Customer;
import com.dermaCare.customerService.repository.CustomerRepository;

@DataMongoTest
public class CustomerRepositoryTest {
	
	@Autowired
	private CustomerRepository customerRepository;
	
	public Customer customer;

	@BeforeEach
	void setUp() {
		 customer = new Customer(
		            null, null,null,                        // id
		            "9876543210",                 // mobileNumber
		            "Alice Johnson", null,             // fullName
		            "Female",                     // gender
		            "alice.johnson@example.com",  // emailId
		            "1995-07-15",                 // dateOfBirth
		            "REF98765" ,false                  // referCode
		        );
		 
		 customerRepository.save(customer);
	}
	@AfterEach
	void tearDown() {
		customer = null;
		customerRepository.deleteAll();
		
	}

@Test
void testfindByMobileNumber() {
	Optional<Customer> c =  customerRepository.findByMobileNumber("9876543210");
	assertEquals(c.get().getMobileNumber(),customer.getMobileNumber());
	assertEquals(c.get().getDateOfBirth(),customer.getDateOfBirth());
	assertEquals(c.get().getEmailId(),customer.getEmailId());
	assertEquals(c.get().getFullName(),customer.getFullName());
	assertEquals(c.get().getGender(),customer.getGender());
	assertNotNull(c);
	}

@Test
void testfindByMobileNumberNotFound() {
	Optional<Customer> c =  customerRepository.findByMobileNumber("987654321011");
	assertFalse(c.isPresent());
}


@Test

void testFindByEmailIdFound() {
	Optional<Customer> c =  customerRepository.findByEmailId( "alice.johnson@example.com");
	assertEquals(c.get().getMobileNumber(),customer.getMobileNumber());
	assertEquals(c.get().getDateOfBirth(),customer.getDateOfBirth());
	assertEquals(c.get().getEmailId(),customer.getEmailId());
	assertEquals(c.get().getFullName(),customer.getFullName());
	assertEquals(c.get().getGender(),customer.getGender());
	assertNotNull(c);
}
@Test
void testfindByEmailNotFound() {
	Optional<Customer> c =  customerRepository.findByEmailId("shravan@example.com");
	assertFalse(c.isPresent());
}


@Test

void testFindByfullNameFound() {
	 List<Customer> c =  customerRepository.findByfullName("Alice Johnson");
	assertEquals(c.get(0).getMobileNumber(),customer.getMobileNumber());
	assertEquals(c.get(0).getDateOfBirth(),customer.getDateOfBirth());
	assertEquals(c.get(0).getEmailId(),customer.getEmailId());
	assertEquals(c.get(0).getFullName(),customer.getFullName());
	assertEquals(c.get(0).getGender(),customer.getGender());
	assertNotNull(c);
}

@Test
void testfindByFullNameNotFound() {
	 List<Customer> c =  customerRepository.findByfullName("kk");
	assertTrue(c.isEmpty());
}

@Test
void deleteByMobileNumberFound() {
	Customer c =  customerRepository.deleteByMobileNumber("9876543210");
		assertEquals(c.getMobileNumber(),customer.getMobileNumber());
		assertEquals(c.getDateOfBirth(),customer.getDateOfBirth());
		assertEquals(c.getEmailId(),customer.getEmailId());
		assertEquals(c.getFullName(),customer.getFullName());
		assertEquals(c.getGender(),customer.getGender());
		assertNotNull(c);
	
}

@Test
void  deleteByMobileNumberNotFound() {
	Customer c =  customerRepository.deleteByMobileNumber("9876543210232");
	assertNull(c);
}


@Test
void testFindFirstByOrderByIdDescFound() {
	Customer c =  customerRepository.findFirstByOrderByIdDesc();
	assertEquals(c.getMobileNumber(),customer.getMobileNumber());
	assertEquals(c.getDateOfBirth(),customer.getDateOfBirth());
	assertEquals(c.getEmailId(),customer.getEmailId());
	assertEquals(c.getFullName(),customer.getFullName());
	assertEquals(c.getGender(),customer.getGender());
	assertNotNull(c);
}

@Test
void testFindFirstByOrderByIdDescNotFound() {
	Optional<Customer> c =  customerRepository.findByEmailId("shravan@example.com");
	assertFalse(c.isPresent());
}
	
}
