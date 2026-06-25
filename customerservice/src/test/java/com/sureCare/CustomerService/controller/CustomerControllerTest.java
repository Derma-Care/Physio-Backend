//package com.sureCare.CustomerService.controller;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//import java.util.ArrayList;
//import java.util.List;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//import com.dermaCare.customerService.controller.CustomerController;
//import com.dermaCare.customerService.dto.ConsultationDTO;
//import com.dermaCare.customerService.dto.CustomerDTO;
//import com.dermaCare.customerService.entity.ConsultationEntity;
//import com.dermaCare.customerService.service.CustomerService;
//import com.dermaCare.customerService.util.Response;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//@WebMvcTest(CustomerController.class)
//public class CustomerControllerTest {
//	
//	@Autowired
//	private MockMvc mockMvc;
//		
//	@MockitoBean
//	private CustomerService customerService;
//	
//	private CustomerDTO	customerDTO;
//	private CustomerDTO	customerDTO_1;
//	private Response responseSuccess;
//	private Response listResponse;
//	private Response resFailure;
//	private Response deleteRes;
//	List<CustomerDTO> c = new ArrayList<>();
//	List<CustomerDTO> empty = new ArrayList<>();
//	private ConsultationDTO condto;
//	private ConsultationEntity conentity;
//	private Response consulResponsesuccess;
//	private Response consulResponseFailure;
//	@BeforeEach
//	public void setUp() {
//	 customerDTO = new CustomerDTO(
//			    "CR_1",                          // customerId
//			    "Shravan",                         // fullName
//			    "9876543210",                       // mobileNumber
//			    "male",                             // gender
//			    "fcm_token_123",                    // fcm
//			    "shravan@example.com",            // emailId
//			    "REF123",                           // referCode
//			    "1990-05-15"                        // dateOfBirth
//			);
//	 customerDTO_1 = new CustomerDTO(
//			    "CR_2",                          // customerId
//			    "rajesh",                         // fullName
//			    "9876543210",                       // mobileNumber
//			    "male",                             // gender
//			    "fcm_token_123",                    // fcm
//			    "rajesh@example.com",            // emailId
//			    "REF11",                           // referCode
//			    "1990-05-19"                        // dateOfBirth
//			);
//	 responseSuccess = new Response("Updated successfully",200,true,customerDTO);
//	 resFailure = new Response("No Customer Found With Given MobileNumber",404,false,null);
//	 deleteRes = new Response("Deleted successfully",200,true,customerDTO);
//	 c.add(customerDTO);
//	 c.add(customerDTO_1);
//	 listResponse = new Response("fetched successfully",200,true,c);
//	 condto = new ConsultationDTO("68285b3a42da7c18e16f7a39","video consultation");
//	 conentity = new ConsultationEntity(null,"68285b3a42da7c18e16f7a39","video consultation");
//	 consulResponsesuccess = new Response("consultation saved successfully",200,true, condto );
//	 consulResponseFailure = new Response("Unable save consultation",404,false,null );
//	}
//	
//	@AfterEach
//	public void tearDown() {
//		customerDTO = null;
//		 responseSuccess = null;
//		 resFailure = null;
//		 
//	}	
//		
//		@Test
//		public void saveFavouriteDoctorSuccess() throws Exception {
//			when(customerService.saveConsultation(condto)).thenReturn(consulResponsesuccess);
//			mockMvc.perform(post("/customer/createConsultation").
//					contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().
//							writeValueAsString(condto))).andExpect(status().isOk());}
//		
//		
//		@Test
//		public void saveFavouriteDoctorFailure() throws Exception {
//			when(customerService.saveConsultation(condto)).thenReturn(consulResponseFailure);
//			mockMvc.perform(post("/customer/createConsultation").
//					contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().
//							writeValueAsString(condto))).andExpect(status().isNotFound());}
//		
//		}
//
//
