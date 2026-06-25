//package com.sureCare.CustomerService.service;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//import java.util.List;
//import java.util.Optional;
//import static org.mockito.Mockito.*;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import com.dermaCare.customerService.dto.ConsultationDTO;
//import com.dermaCare.customerService.dto.CustomerDTO;
//import com.dermaCare.customerService.entity.ConsultationEntity;
//import com.dermaCare.customerService.entity.Customer;
//import com.dermaCare.customerService.repository.ConsultationRep;
//import com.dermaCare.customerService.repository.CustomerRepository;
//import com.dermaCare.customerService.service.CustomerServiceImpl;
//import com.dermaCare.customerService.util.HelperForConversion;
//import com.dermaCare.customerService.util.Response;
//
//@ExtendWith(MockitoExtension.class)
//public class CustomerServiceImplTest {
//	
//	@Mock
//	private  CustomerRepository rep;
//	
//	@Mock
//	private  HelperForConversion  helperForConversion;
//	
//	@Mock
//	private ConsultationRep consultationRep;
//	    
//	@InjectMocks
//	private CustomerServiceImpl service;	
//	
//	
//	private Customer customer;
//	private CustomerDTO customerDTO;
//	private ConsultationDTO condto;
//	private ConsultationEntity conentity;
//	
//	@BeforeEach
//	public void setUp() {
//		 customer =new Customer(null,
//				    "CR_1", null,                         // customerId
//				    "John Doe",                         // fullName
//				    "9876543210",                       // mobileNumber
//				    "male",                             // gender
//				    "fcm_token_123",                    // fcm
//				    "john.doe@example.com",            // emailId
//				    "REF123",                           // referCode
//				    "1990-05-15" ,false                       // dateOfBirth
//				);
//		 customerDTO = new CustomerDTO(
//				    "CR_1",                          // customerId
//				    "John Doe",                         // fullName
//				    "9876543210",                       // mobileNumber
//				    "male",                             // gender
//				    "fcm_token_123",                    // fcm
//				    "john.doe@example.com",            // emailId
//				    "REF123",                           // referCode
//				    "1990-05-15"                        // dateOfBirth
//				);
//		 
//		 condto = new ConsultationDTO("68285b3a42da7c18e16f7a39","video consultation");
//		 conentity = new ConsultationEntity(null,"68285b3a42da7c18e16f7a39","video consultation");
//		 }
//	
//	@AfterEach
//	public void cleanUp() throws Exception {
//		customer = null;
//		service = null;
//	}
//	
//	
//	  @Test
//	    void testSavesaveConsultation_Success() {
//	        when(consultationRep.save(any(ConsultationEntity.class))).thenReturn(conentity);
//	        Response response = service.saveConsultation(condto);
//	        assertTrue(response.isSuccess());
//	        assertEquals(200, response.getStatus());
//	    }
//	  
//	  @Test
//	    void testSavesaveConsultation_Failure() {
//	        when(consultationRep.save(any(ConsultationEntity.class))).thenReturn(null);
//	        Response response = service.saveConsultation(condto);
//	        assertFalse(response.isSuccess());
//	        assertEquals(404, response.getStatus());
//	    } 
//	  
//		
//		
//}
