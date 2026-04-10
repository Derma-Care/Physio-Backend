package com.dermaCare.customerService.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dermaCare.customerService.dto.BookingRequset;
import com.dermaCare.customerService.dto.BookingResponse;
import com.dermaCare.customerService.dto.ConsultationDTO;
import com.dermaCare.customerService.dto.CustomerDTO;
import com.dermaCare.customerService.dto.CustomerLoginDTO;
import com.dermaCare.customerService.dto.CustomerRatingDomain;
import com.dermaCare.customerService.dto.FavouriteDoctorsDTO;
import com.dermaCare.customerService.dto.LoginDTO;
import com.dermaCare.customerService.dto.NotificationToCustomer;
import com.dermaCare.customerService.dto.TempBlockingSlot;
import com.dermaCare.customerService.service.CustomerService;
import com.dermaCare.customerService.util.OtpUtil;
import com.dermaCare.customerService.util.ResBody;
import com.dermaCare.customerService.util.Response;
import com.dermaCare.customerService.util.ResponseStructure;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;


@RestController
@RequestMapping("/customer")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class CustomerController {

	@Autowired
	private CustomerService customerService;
	
	private static final Logger log = LoggerFactory.getLogger(CustomerController.class);
		
	
	@PostMapping("/VerifyUserCredentialsAndGenerateAndSendOtp")
	public ResponseEntity<Response> verifyUserCredentialsAndGenerateAndSendOtp(@RequestBody LoginDTO loginDTO) {
		log.debug("credentials verified successfully", loginDTO.getMobileNumber() );
		return customerService.verifyUserCredentialsAndGenerateAndSendOtp(loginDTO);
	}

	
	@PostMapping("/verifyOtp")
	public ResponseEntity<Response> verifyOtp(@RequestBody LoginDTO loginDTO) {
		return customerService.verifyOtp(loginDTO);
	}

	
	@PostMapping("/resendOtp")
	public ResponseEntity<Response> resendOtp(@RequestBody LoginDTO loginDTO) {
		return customerService.resendOtp(loginDTO);
	}

	
	//CUSTOMER CRUD APIS
	
	@PostMapping("/saveBasicDetails")
	public ResponseEntity<Response> saveCustomerBasicDetails(@RequestBody @Valid CustomerDTO customerDTO ){
		Response response =  customerService.saveCustomerBasicDetails(customerDTO);
		if(response != null && response.getStatus() != 0) {
			 return ResponseEntity.status(response.getStatus()).body(response);
		 }else {
				return null;
		}
	}
	
	@GetMapping("/getBasicDetails/{mobileNumber}")
	public ResponseEntity<Response> getCustomerBasicDetails(@PathVariable String mobileNumber ){
		Response response =  customerService.getCustomerByMobileNumber(mobileNumber);
		if(response != null && response.getStatus() != 0) {
			 return ResponseEntity.status(response.getStatus()).body(response);
		 }else {
				return null;
		}
	}
		
	@GetMapping("/getAllCustomers")
	public ResponseEntity<Response> getAllCustomers(){
		Response response =  customerService.getAllCustomers();
		if(response != null && response.getStatus() != 0) {
			 return ResponseEntity.status(response.getStatus()).body(response);
		 }else {
				return null;
		}
	}
	
	@PutMapping("/updateCustomerBasicDetails/{mobileNumber}")
	public ResponseEntity<Response> updateCustomerBasicDetails(@RequestBody CustomerDTO 
		customerDTO,@PathVariable String mobileNumber ){
		Response response =  customerService.updateCustomerBasicDetails(customerDTO, mobileNumber);
		if(response != null && response.getStatus() != 0) {
			 return ResponseEntity.status(response.getStatus()).body(response);
		 }else {
				return null;
		}
	}
	
	
	@DeleteMapping("/deleteCustomerBasicDetails/{mobileNumber}")
	public ResponseEntity<Response> deleteCustomerBasicDetails(@PathVariable String mobileNumber ){
		Response response =  customerService.deleteCustomerByMobileNumber(mobileNumber);
		if(response != null && response.getStatus() != 0) {
			 return ResponseEntity.status(response.getStatus()).body(response);
		 }else {
				return null;
		}
	}
	
  	@GetMapping("/getCustomerByInput/{input}")
  	public ResponseEntity<?> getCustomerByUsernameMobileEmail(@PathVariable String input) {
  		if (input == null || input.equals("") || input.equals(" ")) {
  			return new ResponseEntity<String>("Please Enter a valid input ", HttpStatus.BAD_REQUEST);
  		} else {
  			if (OtpUtil.isMobileNumber(input)) {
  				String mobilenumber;
  				try {
  					mobilenumber = input; 
  				} catch (Exception e) {
  					return new ResponseEntity<String>("Please Enter a MobileNumber in Range ", HttpStatus.BAD_REQUEST);
  				}
  				CustomerDTO optCustomer =  customerService.getCustomerDetailsByMobileNumber(mobilenumber);
  				if (optCustomer != null) {
  					return new ResponseEntity<CustomerDTO>(optCustomer, HttpStatus.OK);
  				} else {
  					return new ResponseEntity<String>("Customer Data not found", HttpStatus.OK);
  				}
  			} else {
  				if (OtpUtil.isEmail(input)) {
  					CustomerDTO optCustomer =  customerService.getCustomerDetailsByEmail(input);
  					if (optCustomer != null) {
  					
  						return new ResponseEntity<CustomerDTO>(optCustomer, HttpStatus.OK);
  					} else {
  						return new ResponseEntity<String>("Customer Data not found Please enter a valid email",
  								HttpStatus.OK);
  					}
  				} else {
  					List<CustomerDTO> listCustomers =  customerService.getCustomerByfullName(input);
  					if (listCustomers.isEmpty()) {
  						return new ResponseEntity<String>("Customers Data Not found", HttpStatus.OK);
  					} else {
  						return new ResponseEntity<List<CustomerDTO>>(listCustomers, HttpStatus.OK);
  					}
  				}
  			}
  		}
  	}

  	
  	//consultation
  	
  	 // POST: Create a new consultation
    @PostMapping("/createConsultation")
    public ResponseEntity<Response> createConsultation(@RequestBody ConsultationDTO entity) {
        Response response = customerService.saveConsultation(entity);
        if(response != null && response.getStatus() != 0) {
   		 return ResponseEntity.status(response.getStatus()).body(response);
   	 }else {
   			return null;
		}
    }
   

    // GET: Retrieve all consultations
    @GetMapping("/getAllConsultations")
    public ResponseEntity<Response> getAllConsultations() {
        Response response = customerService.getAllConsultations();
        if(response != null && response.getStatus() != 0) {
   		 return ResponseEntity.status(response.getStatus()).body(response);
   	 }else {
   			return null;
		}
}
    
    //  DOCTOR APIS
    
   @GetMapping("/getDoctorsByServiceId/{hospitalid}/{serviceId}")
    public ResponseEntity<Response> getDoctorsByServiceId(@PathVariable String hospitalid, @PathVariable String serviceId){
    	Response response = customerService.getDoctors(hospitalid, serviceId);
    	if(response != null && response.getStatus() != 0) {
   		 return ResponseEntity.status(response.getStatus()).body(response);
   	 }else {
   			return null;
		}
    	
    }
    
   @PostMapping("/saveFavouriteDoctor")
    public ResponseEntity<Response> saveFavouriteDoctor(@RequestBody FavouriteDoctorsDTO favouriteDoctorsDTO){
    	ResponseEntity<Response> response = customerService.saveFavouriteDoctors(favouriteDoctorsDTO);
    	if(response.hasBody()) {
   		 return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
   	 }else {
   			return null;
		}
    }
   
   @GetMapping("/getAllSavedFavouriteDoctors")
   public ResponseEntity<Response> getAllSavedFavouriteDoctors(){
   	Response response = customerService.getAllSavedFavouriteDoctors();
   	if(response != null && response.getStatus() != 0) {
  		 return ResponseEntity.status(response.getStatus()).body(response);
  	 }else {
  			return null;
		}
   	
   }
   
   @GetMapping("/getDoctorSlots/{hospitalId}/{branchId}/{doctorId}")
   public ResponseEntity<Response> getDoctorSlots(@PathVariable String hospitalId,@PathVariable String branchId,@PathVariable String doctorId){
   	Response response = customerService.getDoctorsSlots(hospitalId,branchId,doctorId);
   	if(response != null && response.getStatus() != 0) {
  		 return ResponseEntity.status(response.getStatus()).body(response);
  	 }else {
  			return null;
	}}
   
   
   @GetMapping("/getReports/{customerId}")
   public ResponseEntity<Response> getReports(@PathVariable String customerId){
   	Response response = customerService.getReportsAndDoctorSaveDetails(customerId);
   	if(response != null && response.getStatus() != 0) {
  		 return ResponseEntity.status(response.getStatus()).body(response);
  	 }else {
  			return null;
	}}
   	
   	
    
// BOOKING APIS

@PostMapping("/bookService")
public ResponseEntity<Object> bookService(@RequestBody BookingRequset req)throws JsonProcessingException  {
	Response response = customerService.bookService(req);
	if(response != null && response.getData() == null) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else if(response != null && response.getData() != null) {
		 return ResponseEntity.status(response.getStatus()).body(response.getData());}
		 else {
			 return null;
		 }
	}

@DeleteMapping("/deleteService/{id}")
public ResponseEntity<Object> deleteBookedService(@PathVariable String id){
	Response response = customerService.deleteBookedService(id);
	if(response != null && response.getData() == null) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else if(response != null && response.getData() != null) {
		 return ResponseEntity.status(response.getStatus()).body(response.getData());}
		 else {
			 return null;
		 }
}


@GetMapping("/getBookedService/{id}")
public ResponseEntity<Object> getBookedService(@PathVariable String id){
	Response response = customerService.getBookedService(id);
	if(response != null && response.getData() == null) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else if(response != null && response.getData() != null) {
		 return ResponseEntity.status(response.getStatus()).body(response.getData());}
		 else {
			 return null;
		 }
}

@GetMapping("/getBookedServices/{mobileNumber}")
public ResponseEntity<Object> getCustomerBookedServices(
		@PathVariable String mobileNumber){
	Response response = customerService.getCustomerBookedServices(mobileNumber);
	if(response != null && response.getData() == null) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else if(response != null && response.getData() != null) {
		 return ResponseEntity.status(response.getStatus()).body(response.getData());}
		 else {
			 return null;
		 }
}

@GetMapping("/getAllBookedServices")
public ResponseEntity<ResponseStructure<List<BookingResponse>>> getAllBookedServices() {
    ResponseStructure<List<BookingResponse>> response =
    		customerService.getAllBookedServices();
    if(response != null) {
		 return ResponseEntity.status(response.getHttpStatus().value()).body(response);}
	else {
	     return null;}
}

@GetMapping("/getBookingByDoctorId/{doctorId}")
public ResponseEntity<Object> getBookingByDoctorId(@PathVariable String doctorId){
	Response response = customerService.getBookingByDoctorId(doctorId);
	if(response != null && response.getData() == null) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else if(response != null && response.getData() != null) {
		 return ResponseEntity.status(response.getStatus()).body(response.getData());}
		 else {
			 return null;
		 }
}

@GetMapping("/getAllBookedServicesBySId/{serviceId}")
public ResponseEntity<Object> getBookingByServiceId(@PathVariable String serviceId){
	Response response = customerService.getBookingByServiceId(serviceId);
	if(response != null && response.getData() == null) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else if(response != null && response.getData() != null) {
		 return ResponseEntity.status(response.getStatus()).body(response.getData());}
		 else {
			 return null;
		 }}

@GetMapping("/getAllBookedServicesByClinicId/{clinicId}")
public ResponseEntity<Object> getBookingByClinicId(@PathVariable String clinicId){
	Response response = customerService.getBookingByClinicId(clinicId);
	if(response != null && response.getData() == null) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else if(response != null && response.getData() != null) {
		 return ResponseEntity.status(response.getStatus()).body(response.getData());}
		 else {
			 return null;
		 }}



//// RATINGS APIS

@PostMapping("/submitCustomerRating")
public ResponseEntity<Response> submitCustomerRating(@RequestBody CustomerRatingDomain ratingRequest) {
    Response response = customerService.submitCustomerRating(ratingRequest);
    if(response != null && response.getStatus() != 0) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else {
			return null;
}
}

@GetMapping("/getRatingInfo/{branchlId}/{doctorId}")
public ResponseEntity<Response> getRatingInfo(@PathVariable String branchlId, @PathVariable String doctorId) {
	 Response response = customerService.getRatingForService( branchlId,doctorId);
	 if(response != null && response.getStatus() != 0) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else {
			return null;
		}
}

@GetMapping("/getRatingInfoByDoctorId/{doctorId}")
public ResponseEntity<Response> getRatingInfoByDoctorId(@PathVariable String doctorId) {
	 Response response = customerService.getRatingForServiceBydoctorId(doctorId);
	 if(response != null && response.getStatus() != 0) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else {
			return null;
		}
}

@GetMapping("/getAverageRating/{branchId}/{doctorId}")
public ResponseEntity<Response> getRatingAverageRating(@PathVariable String branchId, @PathVariable String doctorId) {
	 Response response = customerService.getAverageRating(branchId,doctorId);
	 if(response != null && response.getStatus() != 0) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else {
			return null;
		}
}

@GetMapping("/getAverageRatingByDoctorId/{doctorId}")
public ResponseEntity<Response> getAverageRatingByDoctorId( @PathVariable String doctorId) {
	 Response response = customerService.getAverageRatingByDoctorId(doctorId);
	 if(response != null && response.getStatus() != 0) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else {
			return null;
		}
}

  
   //DOCTORSINFO
   
   @GetMapping("/getDoctorsAndClinicDetails/{hospitalId}/{subServiceId}")
   public ResponseEntity<Object> getDoctorsAndClinicDetails(@PathVariable String hospitalId,
                                                                 @PathVariable String subServiceId)throws JsonProcessingException{
	  Response response= customerService.getDoctorsandHospitalDetails(hospitalId, subServiceId);
	  if(response != null) {
			 return ResponseEntity.status(response.getStatus()).body(response.getData());}
			 else {
				 return null;
			 }
	   
   }
   
   @GetMapping("/getDoctorsAndClinicDetailsByBranchId/{hospitalId}/{branchId}/{subServiceId}")
   public ResponseEntity<Object> getDoctorsByHospitalBranchAndSubService(@PathVariable String hospitalId,@PathVariable String branchId,
                                                                 @PathVariable String subServiceId)throws JsonProcessingException{
	  Response response= customerService.getDoctorsByHospitalBranchAndSubService(hospitalId, branchId, subServiceId);
	  if(response != null) {
			 return ResponseEntity.status(response.getStatus()).body(response.getData());}
			 else {
				 return null;
			 }
	   
   }
   @GetMapping("/getDoctorsAndClinicDetailsByBranchId/{hospitalId}/{branchId}/{subServiceId}/{consultationType}")
   public ResponseEntity<Object> getDoctorsByHospitalBranchAndSubService(
           @PathVariable String hospitalId,
           @PathVariable String branchId,
           @PathVariable String subServiceId,
           @PathVariable int consultationType) throws JsonProcessingException {

       // Call service method with consultationType
       Response response = customerService.getDoctorsByHospitalBranchAndSubService(
               hospitalId, branchId, subServiceId, consultationType);
           return ResponseEntity.status(response.getStatus()).body(response.getData());
      
   }


   
   //DOCTORANDHOSPITALDETAILSBYSUBSERVICEID
   
   @GetMapping("/getDoctorAndHospitalDetailsBySubServiceId/{subServiceId}")
   public ResponseEntity<Object> getDetailsBySubServiceIdAndConsultationType(@PathVariable String subServiceId){
	  Response response= customerService.getHospitalsAndDoctorsDetailsBySubServiceId(subServiceId);
	  if(response != null) {
			 return ResponseEntity.status(response.getStatus()).body(response.getData());}
			 else {
				 return null;
			 }
	    }
   
   
   //CATEGORYANDSERVICES
   
   @GetMapping("/getServiceById/{categoryId}")
   public ResponseEntity<Object> getServiceById(@PathVariable String categoryId) {
   	Response response = customerService.getServiceById(categoryId);
   	if(response != null && response.getData() == null) {
   		 return ResponseEntity.status(response.getStatus()).body(response);
   	 }else if(response != null && response.getData() != null ) {
   		 return ResponseEntity.status(response.getStatus()).body(response.getData());
   	 }
   	else {
   			return null;}
   }

   
   @GetMapping("/getSubServicesByServiceId/{serviceId}")
   public ResponseEntity<?> getSubServicesByServiceId(@PathVariable String serviceId){
   	Response response = customerService.getSubServicesByServiceId(serviceId);
   	 if(response != null && response.getStatus() != 0) {
   		 return ResponseEntity.status(response.getStatus()).body(response);
   	 }else {
   			return null;}
       }
  

   @GetMapping("/getSubServiceInfo/{subServiceId}")
   public ResponseEntity<Object> getSubServiceInfoBySubServiceId(@PathVariable String subServiceId)throws JsonProcessingException{
	   Response response = customerService.getSubServiceInfoBySubServiceId(subServiceId);
		if(response != null) {
			 return ResponseEntity.status(response.getStatus()).body(response);
			 }else{
				 return null;
			 }
   }
   
    
   
   @GetMapping("/getBranchesInfoBySubServiceId/{clinicId}/{subServiceId}/{latitude}/{longtitude}")
   public ResponseEntity<Object> getBranchesInfoBySubServiceId(@PathVariable String clinicId,@PathVariable String subServiceId,@PathVariable String latitude,@PathVariable String longtitude)throws JsonProcessingException{
	   Response response = customerService.getBranchesInfoBySubServiceId(clinicId, subServiceId,latitude,longtitude);
		if(response != null) {
			 return ResponseEntity.status(response.getStatus()).body(response);
			 }else{
				 return null;
			 }
   }
   
   
   @GetMapping("/getAllCategories")
  	public ResponseEntity<?> getAllCategory() {
      	Response response = customerService.getAllCategory();
      	if(response != null && response.getData() == null) {
  			 return ResponseEntity.status(response.getStatus()).body(response);
  		 }else if(response != null && response.getData() != null ) {
  			 return ResponseEntity.status(response.getStatus()).body(response.getData());
  		 }
  		else {
  				return null;}}
   
   
   //NOTIFICATION
   
   @GetMapping("/customerNotification/{customerMobileNumber}")
   public ResponseEntity<ResBody<List<NotificationToCustomer>>> notificationToCustomer(
			 @PathVariable String customerMobileNumber){
	   return customerService.notificationToCustomer(customerMobileNumber);
   }
   
   //booking api
   @GetMapping("/inprogressAppointments/{mobileNumber}")
   public ResponseEntity<?> getInProgressAppointments (
			 @PathVariable String mobileNumber){
	   return customerService.getInProgressAppointments(mobileNumber);
 }
   
   @PostMapping("/customerHospitalLogin")
   public ResponseEntity<?> customerLogin(@RequestBody CustomerLoginDTO dto){
	   return customerService.customerLogin(dto);
   }
   
   @GetMapping("/getRecommendedClinicsAndOnDoctors/{keyPoints}")
   public ResponseEntity<?> getRecommendedClinicsAndOnDoctors(
			 @PathVariable String keyPoints){
	   return customerService.getRecommendedClinicsAndOnDoctors(keyPoints);
 }
   
   
   @GetMapping("/getBookingsByBranchId/{branchId}")
   public ResponseEntity<?> getBookingsByBranchId(
			 @PathVariable String branchId){
	   return customerService.getBookingsByBranchId(branchId);
 }

   @GetMapping("/getBookingsByClinicIdWithBranchId/{clinicId}/{branchId}")
   public ResponseEntity<?> getBookingsByClinicIdWithBranchId(
           @PathVariable String clinicId,
           @PathVariable String branchId) {
       return customerService.getBookingsByClinicIdWithBranchId(clinicId, branchId);
   }
   
   @GetMapping("/bookings/customerId/{customerId}")
   public ResponseEntity<?> getBookingsByCustomerId(
			 @PathVariable String customerId){
	   return customerService.getBookingsByCustomerId(customerId);
 }
   
   
   @GetMapping("/bookings/Inprogress/customerId/{customerId}")
   public ResponseEntity<?> getInprogressBookingsByCustomerId(
			 @PathVariable String customerId){
	   return customerService.getInprogressBookingsByCustomerId(customerId);
 }
   
   @GetMapping("/bookings/Inprogress/patientId/{patientId}/{clinicId}")
   public ResponseEntity<?> getInprogressAppointmentsByPatientId(
			 @PathVariable String patientId, @PathVariable String clinicId){
	   return customerService.getInprogressBookingsByPatientId(patientId,clinicId);
 }
   
   
   @GetMapping("/bookings/byRelation/{customerId}")
   public ResponseEntity<?> getInprogressBookingsByRelation(
			 @PathVariable String customerId){
	   return customerService.retrieveAppointnmentsByRelation(customerId);
 }
   
   @GetMapping("/bookings/byPatientId/{patientId}")
   public ResponseEntity<?> getInprogressBookingsByPatientId(
			 @PathVariable String patientId){
	   return customerService.retrieveAppointnmentsByPatientId(patientId);
 }
   
 
   @PostMapping("/block/slot/whileBooking")
   public boolean blockSlot(
		  @RequestBody TempBlockingSlot tempBlockingSlot){
	   return customerService.blockSlot(tempBlockingSlot);
 }
   
   @GetMapping("/gcmToken/{token}")
   public CustomerDTO getCustomerByToken(
			 @PathVariable String token ){
	   return customerService.getCustomerByToken(token);
 }

}
