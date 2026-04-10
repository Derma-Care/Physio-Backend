package com.sureCare.CustomerService.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.dermaCare.customerService.controller.CustomerController;
import com.dermaCare.customerService.dto.ConsultationDTO;
import com.dermaCare.customerService.dto.CustomerDTO;
import com.dermaCare.customerService.entity.ConsultationEntity;
import com.dermaCare.customerService.service.CustomerService;
import com.dermaCare.customerService.util.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(CustomerController.class)
public class CustomerControllerTest {
	
	@Autowired
	private MockMvc mockMvc;
		
	@MockitoBean
	private CustomerService customerService;
	
	private CustomerDTO	customerDTO;
	private CustomerDTO	customerDTO_1;
	private Response responseSuccess;
	private Response listResponse;
	private Response resFailure;
	private Response deleteRes;
	List<CustomerDTO> c = new ArrayList<>();
	List<CustomerDTO> empty = new ArrayList<>();
	private ConsultationDTO condto;
	private ConsultationEntity conentity;
	private Response consulResponsesuccess;
	private Response consulResponseFailure;
	@BeforeEach
	public void setUp() {
	 customerDTO = new CustomerDTO(
			    "CR_1",                          // customerId
			    "Shravan",                         // fullName
			    "9876543210",                       // mobileNumber
			    "male",                             // gender
			    "fcm_token_123",                    // fcm
			    "shravan@example.com",            // emailId
			    "REF123",                           // referCode
			    "1990-05-15"                        // dateOfBirth
			);
	 customerDTO_1 = new CustomerDTO(
			    "CR_2",                          // customerId
			    "rajesh",                         // fullName
			    "9876543210",                       // mobileNumber
			    "male",                             // gender
			    "fcm_token_123",                    // fcm
			    "rajesh@example.com",            // emailId
			    "REF11",                           // referCode
			    "1990-05-19"                        // dateOfBirth
			);
	 responseSuccess = new Response("Updated successfully",200,true,customerDTO);
	 resFailure = new Response("No Customer Found With Given MobileNumber",404,false,null);
	 deleteRes = new Response("Deleted successfully",200,true,customerDTO);
	 c.add(customerDTO);
	 c.add(customerDTO_1);
	 listResponse = new Response("fetched successfully",200,true,c);
	 condto = new ConsultationDTO("68285b3a42da7c18e16f7a39","video consultation");
	 conentity = new ConsultationEntity(null,"68285b3a42da7c18e16f7a39","video consultation");
	 consulResponsesuccess = new Response("consultation saved successfully",200,true, condto );
	 consulResponseFailure = new Response("Unable save consultation",404,false,null );
	}
	
	@AfterEach
	public void tearDown() {
		customerDTO = null;
		 responseSuccess = null;
		 resFailure = null;
		 
	}
		
	@Test
	public void saveCustomerBasicDetailsSuccess() throws Exception {
		when(customerService.saveCustomerBasicDetails(customerDTO)).thenReturn( responseSuccess);
		mockMvc.perform(post("/customer/saveBasicDetails").
				contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().
						writeValueAsString(customerDTO))).andExpect(status().isOk());}
	
		@Test
		public void getCustomerBasicDetailsSuccess() throws Exception  {
			when(customerService.getCustomerByMobileNumber("9876543210")).thenReturn( responseSuccess);
			mockMvc.perform(get("/customer/getBasicDetails/{mobileNumber}","9876543210").
					contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(jsonPath("$.data").value(customerDTO));
		}
		
		@Test
		public void getCustomerBasicDetailsFailure() throws Exception  {
			when(customerService.getCustomerByMobileNumber("9876543210")).thenReturn( resFailure);
			mockMvc.perform(get("/customer/getBasicDetails/{mobileNumber}","9876543210").
					contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("No Customer Found With Given MobileNumber"));
		}
		

		@Test
		public void getAllCustomersSuccess() throws Exception  {
			when(customerService.getAllCustomers()).thenReturn(listResponse);
			MvcResult mvcResult = mockMvc.perform(get("/customer/getAllCustomers").
					contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("fetched successfully")).andExpect(jsonPath("$.status").
					value(200)).andReturn();
			String stringRes = mvcResult.getResponse().getContentAsString();
			Response res = new ObjectMapper().readValue(stringRes,Response.class);
			assertEquals(res.getData().getClass(),listResponse.getData().getClass());
							
		}
		
		@Test
		public void updateCustomerBasicDetailsSuccess() throws Exception  {
			when(customerService.updateCustomerBasicDetails(customerDTO, "9876543210")).thenReturn(responseSuccess);
			mockMvc.perform(put("/customer/updateCustomerBasicDetails/{mobileNumber}","9876543210").
					contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().
							writeValueAsString(customerDTO))).andExpect(status().isOk()).
			andExpect(jsonPath("$.message").value("Updated successfully"));
		}
		
		
		@Test
		public void updateCustomerBasicDetailsFailure() throws Exception  {
			when(customerService.updateCustomerBasicDetails(customerDTO, "9876543210")).thenReturn(resFailure);
			mockMvc.perform(put("/customer/updateCustomerBasicDetails/{mobileNumber}","9876543210").
					contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().
							writeValueAsString(customerDTO))).andExpect(status().isNotFound()).
			andExpect(jsonPath("$.message").value("No Customer Found With Given MobileNumber"));
		}	
		
		@Test
		public void deleteCustomerBasicDetailsSuccess() throws Exception  {
			when(customerService.deleteCustomerByMobileNumber("9876543210")).thenReturn(deleteRes);
			mockMvc.perform(delete("/customer/deleteCustomerBasicDetails/{mobileNumber}","9876543210").
					contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).
			andExpect(jsonPath("$.message").value("Deleted successfully")).
			andExpect(jsonPath("$.data").value(customerDTO));
		}	
		
		@Test
		public void deleteCustomerBasicDetailsFailure() throws Exception  {
			when(customerService.deleteCustomerByMobileNumber("9876543210")).thenReturn(resFailure);
			mockMvc.perform(delete("/customer/deleteCustomerBasicDetails/{mobileNumber}","9876543210").
					contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound()).
			andExpect(jsonPath("$.message").value("No Customer Found With Given MobileNumber"));}	
		

		@Test
		public void getCustomerBasicDetailsByMobileNumberSuccess() throws Exception  {
			when(customerService.getCustomerDetailsByMobileNumber("9876543210")).thenReturn(customerDTO);
			mockMvc.perform(get("/customer/getCustomerByInput/{input}","9876543210").
					contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(jsonPath("$.customerId").value("CR_1"));
		}				
		
		@Test
		public void getCustomerBasicDetailsByFullNameSuccess() throws Exception  {
		when(customerService.getCustomerByfullName("Shravan")).thenReturn(c);
			mockMvc.perform(get("/customer/getCustomerByInput/{input}","Shravan").
					contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].customerId").value("CR_1"));
		}	
		
		@Test
		public void getCustomerBasicDetailsByEmailSuccess() throws Exception  {
		when(customerService.getCustomerDetailsByEmail("shravan@gmail.com")).thenReturn(customerDTO);
			mockMvc.perform(get("/customer/getCustomerByInput/{input}","shravan@gmail.com").
					contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(jsonPath("$.customerId").value("CR_1"));
		}
		
		
		@Test
		public void getCustomerBasicDetailsByMobileNumberFailure() throws Exception  {
			when(customerService.getCustomerDetailsByMobileNumber("9876543210")).thenReturn(null);
			mockMvc.perform(get("/customer/getCustomerByInput/{input}","9876543210").
					contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
			
			
		}				
		
		@Test
		public void getCustomerBasicDetailsByFullNameFailure() throws Exception  {
		when(customerService.getCustomerByfullName("Shravan")).thenReturn(empty);
			mockMvc.perform(get("/customer/getCustomerByInput/{input}","Shravan").
					contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
			
		}	
		
		@Test
		public void getCustomerBasicDetailsByEmailFailure() throws Exception  {
		when(customerService.getCustomerDetailsByEmail("shravan@gmail.com")).thenReturn(null);
			mockMvc.perform(get("/customer/getCustomerByInput/{input}","shravan@gmail.com").
					contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
			
		}
		
		
		@Test
		public void saveFavouriteDoctorSuccess() throws Exception {
			when(customerService.saveConsultation(condto)).thenReturn(consulResponsesuccess);
			mockMvc.perform(post("/customer/createConsultation").
					contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().
							writeValueAsString(condto))).andExpect(status().isOk());}
		
		
		@Test
		public void saveFavouriteDoctorFailure() throws Exception {
			when(customerService.saveConsultation(condto)).thenReturn(consulResponseFailure);
			mockMvc.perform(post("/customer/createConsultation").
					contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().
							writeValueAsString(condto))).andExpect(status().isNotFound());}
		
		}


