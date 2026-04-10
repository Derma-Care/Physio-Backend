package com.sureCare.CustomerService.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import com.dermaCare.customerService.dto.ConsultationDTO;
import com.dermaCare.customerService.dto.CustomerDTO;
import com.dermaCare.customerService.entity.ConsultationEntity;
import com.dermaCare.customerService.entity.Customer;
import com.dermaCare.customerService.repository.ConsultationRep;
import com.dermaCare.customerService.repository.CustomerRepository;
import com.dermaCare.customerService.service.CustomerServiceImpl;
import com.dermaCare.customerService.util.HelperForConversion;
import com.dermaCare.customerService.util.Response;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceImplTest {
	
	@Mock
	private  CustomerRepository rep;
	
	@Mock
	private  HelperForConversion  helperForConversion;
	
	@Mock
	private ConsultationRep consultationRep;
	    
	@InjectMocks
	private CustomerServiceImpl service;	
	
	
	private Customer customer;
	private CustomerDTO customerDTO;
	private ConsultationDTO condto;
	private ConsultationEntity conentity;
	
	@BeforeEach
	public void setUp() {
		 customer =new Customer(null,
				    "CR_1", null,                         // customerId
				    "John Doe",                         // fullName
				    "9876543210",                       // mobileNumber
				    "male",                             // gender
				    "fcm_token_123",                    // fcm
				    "john.doe@example.com",            // emailId
				    "REF123",                           // referCode
				    "1990-05-15" ,false                       // dateOfBirth
				);
		 customerDTO = new CustomerDTO(
				    "CR_1",                          // customerId
				    "John Doe",                         // fullName
				    "9876543210",                       // mobileNumber
				    "male",                             // gender
				    "fcm_token_123",                    // fcm
				    "john.doe@example.com",            // emailId
				    "REF123",                           // referCode
				    "1990-05-15"                        // dateOfBirth
				);
		 
		 condto = new ConsultationDTO("68285b3a42da7c18e16f7a39","video consultation");
		 conentity = new ConsultationEntity(null,"68285b3a42da7c18e16f7a39","video consultation");
		 }
	
	@AfterEach
	public void cleanUp() throws Exception {
		customer = null;
		service = null;
	}
	
	@Test
    void testSaveCustomerBasicDetails_Success() {
        when(rep.save(any(Customer.class))).thenReturn(customer);
        Response response = service.saveCustomerBasicDetails(customerDTO);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatus());
        assertEquals("Details saved successfullly", response.getMessage());
        assertEquals(customerDTO, response.getData());
    }
	@Test
	void testSaveCustomerBasicDetails_MobileNumberExists() {
	    when(rep.findByMobileNumber(customerDTO.getMobileNumber()))
	        .thenReturn(Optional.of(customer));

	    Response response = service.saveCustomerBasicDetails(customerDTO);

	    assertFalse(response.isSuccess());
	    assertEquals(409, response.getStatus());
	    assertEquals("MobileNumber Already Exist", response.getMessage());
	    assertNull(response.getData());

	    verify(rep, never()).save(any());
	}
	
	@Test
	void testSaveCustomerBasicDetails_NullInput() {
	    Response response = service.saveCustomerBasicDetails(null);

	    assertFalse(response.isSuccess());
	    assertEquals(0, response.getStatus());
	    assertNull(response.getMessage()); // could be "null" or actual exception
	    assertNull(response.getData());

	    verify(rep, never()).save(any());
	}
	
	@Test
	void testSaveCustomerBasicDetails_ExceptionThrown() {
	    when(rep.findByMobileNumber(anyString()))
	        .thenThrow(new RuntimeException("Database error"));

	    Response response = service.saveCustomerBasicDetails(customerDTO);

	    assertFalse(response.isSuccess());
	    assertEquals(500, response.getStatus());
	    assertEquals("Database error", response.getMessage());
	    assertNull(response.getData());
	}
	
	
	@Test
	public void getCustomerByMobileNumberSuccess() {
		 when(rep.findByMobileNumber(anyString()))
	        .thenReturn(Optional.of(customer));

	    Response response = service.getCustomerByMobileNumber("9177675260");

	    assertTrue(response.isSuccess());
	    assertEquals(200, response.getStatus());
	    assertNotNull(response.getData());
	}

	@Test
	public void getCustomerByMobileNumberFailure() {
		 when(rep.findByMobileNumber(anyString()))
	        .thenReturn(null);

	    Response response = service.getCustomerByMobileNumber("9177675260");

	    assertFalse(response.isSuccess());
	    assertEquals(500, response.getStatus());
	    assertNull(response.getData());
	}
	
	@Test
	void getCustomerByMobileNumberFailure_ExceptionThrown() {
	    when(rep.findByMobileNumber(anyString()))
	        .thenThrow(new RuntimeException("MobileNumber Not Found Exception"));

	    Response response = service.getCustomerByMobileNumber("9177675260");

	    assertFalse(response.isSuccess());
	    assertEquals(500, response.getStatus());
	    assertEquals("MobileNumber Not Found Exception", response.getMessage());
	    assertNull(response.getData());
	}
	
	@Test
	public void getAllCustomersSuccess() {
		when(rep.findAll()).thenReturn(List.of(customer));

    Response response = service.getAllCustomers();

    assertTrue(response.isSuccess());
    assertEquals(200, response.getStatus());
    assertNotNull(response.getData());
	}
	
	@Test
	public void getAllCustomersFailure() {
		when(rep.findAll()).thenReturn(null);
    Response response = service.getAllCustomers();

    assertFalse(response.isSuccess());
    assertEquals(404, response.getStatus());
    assertNull(response.getData());
	}
	
	@Test
	void getAllCustomersFailure_ExceptionThrown() {
	    when(rep.findAll())
	        .thenThrow(new NullPointerException("customers not found"));

	    Response response = service.getAllCustomers();

	    assertFalse(response.isSuccess());
	    assertEquals(500, response.getStatus());
	    assertEquals("customers not found", response.getMessage());
	    assertNull(response.getData());
	}
	
	@Test
	public void updateCustomerBasicDetailsSuccess() {
	    when(rep.findByMobileNumber(anyString())).thenReturn(Optional.of(customer));
	    when(rep.save(any(Customer.class))).thenReturn(customer);

	    Response response = service.updateCustomerBasicDetails(customerDTO, "9177675260");

	    assertTrue(response.isSuccess());
	    assertEquals(200, response.getStatus());
	    assertEquals("details updated successfullly", response.getMessage());
	    assertNotNull(response.getData());
	}

	@Test
	public void updateCustomerBasicDetailsFailure() {
		when(rep.findByMobileNumber(anyString()))
        .thenReturn(null);

    Response response = service.updateCustomerBasicDetails(customerDTO, null);

    assertFalse(response.isSuccess());
    assertEquals(500, response.getStatus());
    assertNull(response.getData());
		
			}
	
	@Test
	void updateCustomerBasicDetailsFailure_ExceptionThrown() {
	    when(rep.findByMobileNumber(anyString()))
	        .thenThrow(new NullPointerException("mobileNumber Not Exist"));

	    Response response = service.updateCustomerBasicDetails(customerDTO, "9177675260");

	    assertFalse(response.isSuccess());
	    assertEquals(500, response.getStatus());
	    assertEquals("mobileNumber Not Exist", response.getMessage());
	    assertNull(response.getData());
	}
	
	@Test	
	public void deleteCustomerByMobileNumberSuccess() {
			when(rep.findByMobileNumber(anyString()))
	        .thenReturn(Optional.of(customer));
			
			when(rep.deleteByMobileNumber(anyString())).thenReturn(customer);

	    Response response = service.deleteCustomerByMobileNumber("9177675260");

	    assertTrue(response.isSuccess());
	    assertEquals(200, response.getStatus());
	    assertNull(response.getData());
	}
	
	@Test	
	public void deleteCustomerByMobileNumberFailure() {
			when(rep.findByMobileNumber(anyString()))
	        .thenReturn(null);

	    Response response = service.deleteCustomerByMobileNumber("9899898988");

	    assertFalse(response.isSuccess());
	    assertEquals(500, response.getStatus());
	    assertNull(response.getData());
	}
	
	@Test
	void deleteCustomerByMobileNumberFailure_ExceptionThrown() {
	    when(rep.findByMobileNumber(anyString()))
	        .thenThrow(new NullPointerException("mobileNumber Not Exist"));

	    Response response = service.deleteCustomerByMobileNumber("9899898988");

	    assertFalse(response.isSuccess());
	    assertEquals(500, response.getStatus());
	    assertEquals("mobileNumber Not Exist", response.getMessage());
	    assertNull(response.getData());
	}
	
	  @Test
	    void testSavesaveConsultation_Success() {
	        when(consultationRep.save(any(ConsultationEntity.class))).thenReturn(conentity);
	        Response response = service.saveConsultation(condto);
	        assertTrue(response.isSuccess());
	        assertEquals(200, response.getStatus());
	    }
	  
	  @Test
	    void testSavesaveConsultation_Failure() {
	        when(consultationRep.save(any(ConsultationEntity.class))).thenReturn(null);
	        Response response = service.saveConsultation(condto);
	        assertFalse(response.isSuccess());
	        assertEquals(404, response.getStatus());
	    } 
	  
		
		
}
