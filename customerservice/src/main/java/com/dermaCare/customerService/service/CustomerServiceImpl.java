package com.dermaCare.customerService.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.dermaCare.customerService.dto.BookingRequset;
import com.dermaCare.customerService.dto.BookingResponse;
import com.dermaCare.customerService.dto.BranchDTO;
import com.dermaCare.customerService.dto.BranchInfo;
import com.dermaCare.customerService.dto.CategoryDto;
import com.dermaCare.customerService.dto.ClinicAndDoctorsResponse;
import com.dermaCare.customerService.dto.ClinicDTO;
import com.dermaCare.customerService.dto.ConsultationDTO;
import com.dermaCare.customerService.dto.CustomerDTO;
import com.dermaCare.customerService.dto.CustomerLoginDTO;
import com.dermaCare.customerService.dto.CustomerRatingDomain;
import com.dermaCare.customerService.dto.DoctorSaveDetailsDTO;
import com.dermaCare.customerService.dto.DoctorsDTO;
import com.dermaCare.customerService.dto.FavouriteDoctorsDTO;
import com.dermaCare.customerService.dto.LoginDTO;
import com.dermaCare.customerService.dto.NotificationToCustomer;
import com.dermaCare.customerService.dto.QuestionsDTO;
import com.dermaCare.customerService.dto.ReportsAndDoctorSaveDetailsDto;
import com.dermaCare.customerService.dto.ReportsDtoList;
import com.dermaCare.customerService.dto.ServicesDto;
import com.dermaCare.customerService.dto.SubServicesDetailsDto;
import com.dermaCare.customerService.dto.SubServicesDto;
import com.dermaCare.customerService.dto.TempBlockingSlot;
import com.dermaCare.customerService.dto.TheraphyAnswersDTO;
import com.dermaCare.customerService.entity.ConsultationEntity;
import com.dermaCare.customerService.entity.Customer;
import com.dermaCare.customerService.entity.CustomerRating;
import com.dermaCare.customerService.entity.FavouriteDoctorsEntity;
import com.dermaCare.customerService.entity.QuestionsByPartEntity;
import com.dermaCare.customerService.entity.QuestionsEntity;
import com.dermaCare.customerService.feignClient.AdminFeign;
import com.dermaCare.customerService.feignClient.BookingFeign;
import com.dermaCare.customerService.feignClient.CategoryServicesFeign;
import com.dermaCare.customerService.feignClient.ClinicAdminFeign;
import com.dermaCare.customerService.feignClient.DoctorServiceFeign;
import com.dermaCare.customerService.feignClient.NotificationFeign;
import com.dermaCare.customerService.repository.ConsultationRep;
import com.dermaCare.customerService.repository.CustomerFavouriteDoctors;
import com.dermaCare.customerService.repository.CustomerRatingRepository;
import com.dermaCare.customerService.repository.CustomerRepository;
import com.dermaCare.customerService.repository.PhysiotherapyRepo;
import com.dermaCare.customerService.util.ExtractFeignMessage;
import com.dermaCare.customerService.util.GetByKey;
import com.dermaCare.customerService.util.HelperForConversion;
import com.dermaCare.customerService.util.ResBody;
import com.dermaCare.customerService.util.Response;
import com.dermaCare.customerService.util.ResponseStructure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import feign.FeignException;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    public CustomerRepository customerRepository;

    @Autowired 
    private CustomerRatingRepository customerRatingRepository; 
    
    @Autowired
	private ConsultationRep consultationRep;
    
    @Autowired
    private BookingFeign bookingFeign;
    
    @Autowired
    private PhysiotherapyRepo physiotherapyRepo;
    private GetByKey getByKey;
    
    @Autowired
    private ClinicAdminFeign clinicAdminFeign;
    
    @Autowired
    private  CustomerFavouriteDoctors customerFavouriteDoctors;
    
    @Autowired 
    private CategoryServicesFeign categoryServicesFeign;
    
    @Autowired 
    private AdminFeign adminFeign;
    
    @Autowired
    private FirebaseMessagingService firebaseMessagingService;
    
    @Autowired
    private NotificationFeign notificationFeign;
    
    @Autowired
    private DoctorServiceFeign doctorServiceFeign;
    
    
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);
    
    
    private Map<String, String> generatedOtps = new HashMap<>();
    private Map<String, Long> session = new HashMap<>();
    private static final long OTP_EXPIRY_MILLIS = 1 * 60 * 1000;
   

   @Override
    public ResponseEntity<Response> verifyUserCredentialsAndGenerateAndSendOtp(LoginDTO loginDTO) {
	   Response response = new Response();
     try {
    	 if(!isIndianMobileNumber(loginDTO.getMobileNumber())) {
    		 response.setMessage("Please Enter Valid MobileNumber");
 	    	response.setStatus(400);
 	    	response.setSuccess(false);}
   	    Optional<Customer> custmer = customerRepository.findByMobileNumber(loginDTO.getMobileNumber());
   	    log.info("customer found with mobilenumber",custmer.get().getMobileNumber());
	    if(custmer.isPresent()) {
	    	custmer.get().setDeviceId(loginDTO.getDeviceId());
	    	customerRepository.save(custmer.get());}
	    	String otp = randomNumber();
	    	if(loginDTO.getDeviceId() != null) {
	    		firebaseMessagingService.sendPushNotification(
	    			    loginDTO.getDeviceId(),
	    			    "🔐 Hello,Here’s your OTP!",
	    			    "Use " + otp + " to verify your login. Expires in 1 minute.",
	    			    "OTP",
	    			    "OTPVerificationScreen",
	    			    "default"
	    			);
	    	log.info("OTP sent successfully {}", loginDTO.getMobileNumber());
	    	generatedOtps.put(loginDTO.getMobileNumber(),otp);
	    	session.put(loginDTO.getMobileNumber(),System.currentTimeMillis());
	    	log.info("OTP successfully stored locally {}", loginDTO.getMobileNumber());
	    	response.setMessage("OTP Sent Successfully");
	    	response.setStatus(200);
	    	response.setSuccess(true);}
	    	else {
	    		response.setMessage("Please Provide DeviceId");
		    	response.setStatus(400);
		    	response.setSuccess(false);
		    	log.warn("Device ID not found {}", loginDTO.getMobileNumber());
		    	}
	    }catch(Exception e) {
	    	response.setMessage(e.getMessage());
	    	response.setStatus(500);
	    	response.setSuccess(false);
	    	log.error("Exception occured {}", e.getMessage());}
     return ResponseEntity.status(response.getStatus()).body(response);
}
   
   
     
    private boolean isIndianMobileNumber(String mobileNumber) {
        mobileNumber = mobileNumber.replaceAll("[\\s\\-()]", "");
        String regex = "^(\\+91|91|0)?[6-9]\\d{9}$";
        return mobileNumber.matches(regex);
    }
 
     private String randomNumber() {
         Random random = new Random();    
         int sixDigitNumber = 100000 + random.nextInt(900000); // Generates number from 100000 to 999999
         return String.valueOf(sixDigitNumber);
     }
     
     
    
   public ResponseEntity<Response> verifyOtp(LoginDTO loginDTO){
	   Response response = new Response();
	   try {
		   String otp = generatedOtps.get(loginDTO.getMobileNumber());
           log.info("OTP found",loginDTO.getMobileNumber() );
		   long createdTime = session.get(loginDTO.getMobileNumber());
		   log.info("otpCreatedAt time successfully found",loginDTO.getMobileNumber() );
		   if(!isExpired(createdTime)) {
			   if(loginDTO.getOtp().equals(otp)) {
			   response.setMessage("OTP Successfully Verified");
			   response.setStatus(200);
				response.setSuccess(true);
			   return ResponseEntity.status(response.getStatus()).body(response);
			   }else {
				   response.setMessage("Invalid OTP Please Enter Correct OTP");
				   response.setStatus(400);
				   return ResponseEntity.status(response.getStatus()).body(response);}
		   }else {
			   response.setMessage("OTP Expired Please Click On Resend OTP");
			   response.setStatus(410);
			   return ResponseEntity.status(response.getStatus()).body(response);
		   }}catch(Exception e) {
			   response.setMessage(e.getMessage());
			   response.setStatus(500);
			   log.error("Exception occured", e.getMessage());
			   return ResponseEntity.status(response.getStatus()).body(response);}
  }

   
   private boolean isExpired(long createdAt) {
       return System.currentTimeMillis() - createdAt > OTP_EXPIRY_MILLIS;
   }
   
 
   
   public  ResponseEntity<Response> resendOtp(LoginDTO loginDTO){
	   Response response = new Response();
	   try {
		   if(!isIndianMobileNumber(loginDTO.getMobileNumber())) {
	    		response.setMessage("Please Enter Valid MobileNumber");
	 	    	response.setStatus(400);
	 	    	response.setSuccess(false);
	 	    	return ResponseEntity.status(response.getStatus()).body(response);}		   
		    String otp = randomNumber();
		    log.info("OTP generated successfully", loginDTO.getMobileNumber());
		    if(loginDTO.getDeviceId() != null) {
		    	firebaseMessagingService.sendPushNotification(
	    			    loginDTO.getDeviceId(),
	    			    "🔐 Hello,Here’s your ResendOTP!",
	    			    "Use " + otp + " to verify your login. Expires in 1 minute.",
	    			    "OTP",
	    			    "OTPVerificationScreen",
	    			    "default"
	    			);
		    	log.info("notification for OTP sent",  loginDTO.getMobileNumber());
	    	generatedOtps.put(loginDTO.getMobileNumber(),otp);
	    	session.put(loginDTO.getMobileNumber(),System.currentTimeMillis());
	    	response.setMessage("OTP Sent Successfully");
			response.setStatus(200);
			response.setSuccess(true);
	    	}else{
		    	response.setMessage("Please Provide DeviceId");
				response.setStatus(400);}
	        }catch(Exception e) {
		    response.setMessage(e.getMessage());
		    response.setStatus(500);}
	        return ResponseEntity.status(response.getStatus()).body(response);
   }
   
   

   public Response saveCustomerBasicDetails(CustomerDTO customerDTO) {
	   Response response =  new Response();
	   try {
	   if(customerDTO != null) {
		Optional<Customer> cstmr = customerRepository.findByMobileNumber(customerDTO.getMobileNumber());
		Optional<Customer> cstmrEmail = customerRepository.findByEmailId(customerDTO.getEmailId());
		   if(cstmr.isPresent()) {
			   response.setMessage("MobileNumber Already Exist");
			   response.setStatus(409);
			   response.setSuccess(false);
			   return response;}
		   
		   if(cstmrEmail.isPresent()) {
			   response.setMessage("EmailId Already Exist");
			   response.setStatus(409);
			   response.setSuccess(false);
			   return response;}
		   	   
		   if(!isValidDate(customerDTO.getDateOfBirth())) {
			   response.setMessage("DateOfBirth Should Be In DD-MM-YYYY Format");
			   response.setStatus(400);
			   response.setSuccess(false);
			   return response;}
	   }
		   Customer customer = HelperForConversion.convertToEntity(customerDTO);
		   customer.setCustomerId(generateCustomerId());
		   Customer cusmr = customerRepository.save(customer);
		   log.info("customer object successfully stored in database", cusmr.getMobileNumber());
		   if(cusmr != null) {
			   CustomerDTO ctmrDTO = HelperForConversion.convertToDTO(customer);
			   response.setData(ctmrDTO);
			   response.setMessage("Details saved successfullly");
			   response.setStatus(200);
			   response.setSuccess(true);
			   return response;
		   }
		   else {
			   response.setMessage("Unable to save details");
			   response.setStatus(400);
			   response.setSuccess(false);
			   return response;
		   }}catch(Exception e) {
			   response.setMessage(e.getMessage());
			   response.setStatus(500);
			   response.setSuccess(false);
			   log.error("Exception occured", e.getMessage());
			  }
	   return response;
	   }
   
   public boolean isValidDate(String date) {
       boolean check;
       String date1 = "^(0?[1-9]|[12][0-9]|3[01])-(0?[1-9]|1[012])-([12][0-9]{3})$";
       check = date.matches(date1);
       return check;
   }
   
   
   public String generateCustomerId() {
	    // Fetch the last customer document ordered by ID in descending order
	    Customer lastCustomer = customerRepository.findFirstByOrderByIdDesc();

	    // If no customer exists in the database, return the first customer ID "CR_1"
	    if (lastCustomer == null) {
	        return "CR_1";
	    }
	    // Get the last customer ID
	    String lastCustomerId = lastCustomer.getCustomerId();

	    // Define the regex pattern to match "CR_" followed by a number (e.g., CR_123)
	    Pattern pattern = Pattern.compile("CR_(\\d+)");
	    Matcher matcher = pattern.matcher(lastCustomerId);

	    // If the last ID is in the expected format (CR_1, CR_2, etc.)
	    if (matcher.matches()) {
	        // Extract the numeric part
	        int currentNumber = Integer.parseInt(matcher.group(1));

	        // Generate the next number
	        int nextNumber = currentNumber + 1;

	        // Return the new ID
	        return "CR_" + nextNumber;
	    } else {
	        // If the ID format is invalid, start from CR_1
	        return "CR_1";
	    }
	}

   
   public Response getCustomerByMobileNumber(String mblnumber) {
	   Response  response =  new Response();
	   try {
		   Optional<Customer> cusmr = customerRepository.findByMobileNumber(mblnumber);
		   log.info("customer found with mobileNumber", cusmr.get().getMobileNumber());
		   if(cusmr.isPresent()) {
			   Customer c = cusmr.get();
			   CustomerDTO ctmrDTO = HelperForConversion.convertToDTO(c);
			   response.setData(ctmrDTO);
			   response.setMessage("Details Fetched Successfullly");
			   response.setStatus(200);
			   response.setSuccess(true);
			   return response;
		   }
		   else {
			   response.setMessage("No Customer Found With Given MobileNumber");
			   response.setStatus(200);
			   response.setSuccess(true);
			   return response;
		   }}catch(Exception e) {
			   response.setMessage(e.getMessage());
			   response.setStatus(500);
			   response.setSuccess(false);
			   log.error(e.getMessage());
			   return response;
			  }
	  }
	   
	
  public Response getAllCustomers() {
	 Response  response =  new Response();
	   try {
		   List<Customer> cusmr = customerRepository.findAll();
		   log.info("successfully retrieved all customers", cusmr.size());
		   List< CustomerDTO> dto = new ArrayList<>();
		   if(cusmr != null && !cusmr.isEmpty()) {
			   for(Customer c : cusmr) {
			   CustomerDTO ctmrDTO = HelperForConversion.convertToDTO(c);
			   dto.add(ctmrDTO);}
			   response.setData(dto);
			   response.setMessage("details fetched successfullly");
			   response.setStatus(200);
			   response.setSuccess(true);
			   return response; }
		   else {
			   response.setMessage("details not fetched successfullly");
			   response.setStatus(200);
			   response.setSuccess(true);
			   return response;
		   } }catch(Exception e) {
			   response.setMessage(e.getMessage());
			   response.setStatus(500);
			   response.setSuccess(false);
			   log.info("Exception occured", e.getMessage());
			   return response;
			  }
	  }

    
public Response updateCustomerBasicDetails( CustomerDTO customerDTO ,String mobileNumber) {
	   Response  response =  new Response();
	   try {
		   Optional<Customer> cusmr = customerRepository.findByMobileNumber(mobileNumber);
		   log.info("customer found with mobileNumber{}",mobileNumber );
		   if(cusmr.isPresent()) {
			   Customer c = cusmr.get();
			   c.setDateOfBirth(customerDTO.getDateOfBirth());
			   c.setEmailId(customerDTO.getEmailId());
			   c.setFullName(customerDTO.getFullName());
			   c.setGender(customerDTO.getGender());
			   c.setMobileNumber(customerDTO.getMobileNumber());
			   Customer obj = customerRepository.save(c);
			   if(obj != null) {
			   CustomerDTO ctmrDTO = HelperForConversion.convertToDTO(obj);
			   response.setData(ctmrDTO);
			   response.setMessage("details updated successfullly");
			   response.setStatus(200);
			   response.setSuccess(true);
			   return response;
		   }}
		   else {
			   response.setMessage("customer not found with given mobileNumber");
			   response.setStatus(404);
			   response.setSuccess(false);
			   log.warn("customer not found with given mobileNumber",mobileNumber );
			   return response;
		   }}catch(Exception e) {
			   response.setMessage(e.getMessage());
			   response.setStatus(500);
			   response.setSuccess(false);
			   log.error("Exception occured", e.getMessage());
			  }
	   return response;
	   }
	   
   
   public Response deleteCustomerByMobileNumber(String mobileNumber) {
	   Response  response =  new Response();
	   try {
		   Optional<Customer> c = customerRepository.findByMobileNumber(mobileNumber);
		   log.info("customer found with mobilenumber", mobileNumber);
		   if(c.isPresent()){
		   Customer obj = customerRepository.deleteByMobileNumber(mobileNumber);
		   log.debug("customer deleted successfully", obj.getMobileNumber());
		   if(obj != null) {
		   response.setData(null);
		   response.setMessage("data deleted successfullly");
		   response.setStatus(200);
		   response.setSuccess(true);
		   return response;
		   }}else{
			   response.setMessage("No customer found with given mobileNumber");
			   response.setStatus(404);
			   response.setSuccess(false);
			   log.warn("customer not found with mobilenumber",mobileNumber);			  
			   return response;
	   }}catch(Exception e) {
		   response.setMessage(e.getMessage());
		   response.setStatus(500);
		   response.setSuccess(false);
		   log.error("Exception occured", e.getMessage());}
   return response;
   }
   
	   
	@Override
	public CustomerDTO getCustomerDetailsByMobileNumber(String mobileNumber) {

	    // Retrieve customer from repository
	    Optional<Customer> customerOptional = customerRepository.findByMobileNumber(mobileNumber);
        
	    // Check if customer is present, return null or handle accordingly if not found
	    if (customerOptional.isEmpty()) {
	        log.warn("Customer not found for mobile number: " + mobileNumber);
	        return null; // Or throw a custom exception based on your requirement
	    }
	    Customer customerObject = customerOptional.get();
	    // Create and return CustomerDTO with null-safe handling
	    CustomerDTO customerDTO = new CustomerDTO(Optional.ofNullable(customerObject.getCustomerId()).orElse(null),  
	            Optional.ofNullable(customerObject.getFullName()).orElse(null),           // Full name
	            String.valueOf(customerObject.getMobileNumber()), null,                      // Mobile number
	            Optional.ofNullable(customerObject.getGender()).orElse(null),            // Gender         // Age
	            Optional.ofNullable(customerObject.getEmailId()).orElse(null),           // Email ID
	             null,
	             Optional.ofNullable(customerObject.getDateOfBirth()).orElse(null)
	    );
          log.info("Customer not found for mobile number: " + mobileNumber);
	    return customerDTO;
	}


	@Override
	public CustomerDTO getCustomerDetailsByEmail(String email) {

	    // Retrieve customer from repository
	    Optional<Customer> customerOptional = customerRepository.findByEmailId(email);

	    // Check if customer is present, return null or handle accordingly if not found
	    if (customerOptional.isEmpty()) {
	        log.warn("Customer not found for email: " + email);
	        return null; // Or throw a custom exception if preferred
	    }
	    Customer customerObject = customerOptional.get();
	    // Create and return CustomerDTO with null-safe handling
	    CustomerDTO customerDTO = new CustomerDTO(Optional.ofNullable(customerObject.getCustomerId()).orElse(null), 
	            Optional.ofNullable(customerObject.getFullName()).orElse(null),           // Full name
	            String.valueOf(customerObject.getMobileNumber()), null,                      // Mobile number
	            Optional.ofNullable(customerObject.getGender()).orElse(null),            // Gender         // Age
	            Optional.ofNullable(customerObject.getEmailId()).orElse(null),           // Email ID
	             null,
	             Optional.ofNullable(customerObject.getDateOfBirth()).orElse(null)
	    );
            log.info("Customer not found for email: " + email);
	    return customerDTO;
	}

	@Override
	public List<CustomerDTO> getCustomerByfullName(String fullName) {
	    // Retrieve customers by full name
	    List<Customer> customers = customerRepository.findByfullName(fullName);
	    List<CustomerDTO> customerDTOs = new ArrayList<>();
	    for (Customer customerObject : customers) {
	   // Create CustomerDTO with null-safe handling
	        CustomerDTO customerDTO = new CustomerDTO(Optional.ofNullable(customerObject.getCustomerId()).orElse(null),
	                Optional.ofNullable(customerObject.getFullName()).orElse(null),           // Full name
	                String.valueOf(customerObject.getMobileNumber()), null,                       // Mobile number
	                Optional.ofNullable(customerObject.getGender()).orElse(null),            // Gender
	                Optional.ofNullable(customerObject.getEmailId()).orElse(null), 
	                null,
	                Optional.ofNullable(customerObject.getDateOfBirth()).orElse(null));

	        customerDTOs.add(customerDTO);
	    }
	    if(!customerDTOs.isEmpty()) {
	    log.info("customers found with fullname "+ fullName );
	    return customerDTOs;
	    }else{
	    	log.warn("customers Not found with fullname "+ fullName );
	    	return Collections.emptyList();
	    }
	}

	
	//consultation
	
	public Response saveConsultation(ConsultationDTO dto) {
	    log.info("SAVE_CONSULTATION :: START :: consultationId={}, consultationType={}",
	            dto.getConsultationId(), dto.getConsultationType());

	    Response response = new Response();
	    ConsultationEntity consultation = new ConsultationEntity();

	    try {
	    	  ConsultationEntity duplicateConsult = consultationRep.findByConsultationType(dto.getConsultationType());
	    	if(duplicateConsult != null || !dto.getConsultationType().equals(" ")) {	    		
	        consultation.setConsultationType(dto.getConsultationType());
	        consultation.setConsultationId(dto.getConsultationId());

	        log.debug("SAVE_CONSULTATION :: SAVING_TO_DB :: consultationId={}", dto.getConsultationId());

	        consultation = consultationRep.save(consultation);
	    	}else {
		         response.setStatus(200);
		         response.setMessage("already consultation available or consultation is empty");
		         response.setSuccess(true);
	    	}
	        if (consultation != null) {
	            log.info("SAVE_CONSULTATION :: SUCCESS :: consultationId={}",
	            		consultation.getConsultationId());

	            ConsultationDTO dtoObj = new ConsultationDTO();
	            dtoObj.setConsultationType(consultation.getConsultationType());
	            dtoObj.setConsultationId(consultation.getConsultationId());

	            response.setData(dtoObj);
	            response.setStatus(200);
	            response.setMessage("saved consultation successfully");
	            response.setSuccess(true);
	            return response;
	        } else {
	            log.warn("SAVE_CONSULTATION :: FAILED_TO_SAVE :: consultationId={}", dto.getConsultationId());

	            response.setStatus(404);
	            response.setMessage("unable to save consultation");
	            response.setSuccess(false);
	            return response;
	        }
	    } catch (Exception e) {
	        log.error("SAVE_CONSULTATION :: ERROR :: consultationId={}", dto.getConsultationId(), e);

	        response.setStatus(500);
	        response.setMessage(e.getMessage());
	        response.setSuccess(false);
	        return response;
	    }
	}
	 
	public Response getAllConsultations() {
	    log.info("GET_ALL_CONSULTATIONS :: START");

	    Response response = new Response();
	    List<ConsultationDTO> dto = new ArrayList<>();

	    try {
	        List<ConsultationEntity> list = consultationRep.findAll();

	        log.debug("GET_ALL_CONSULTATIONS :: DB_COUNT :: {}", list.size());

	        if (!list.isEmpty()) {
	            dto = list.stream().map(n -> {
	                ConsultationDTO consultationDTO = new ConsultationDTO();
	                consultationDTO.setConsultationType(n.getConsultationType());
	                consultationDTO.setConsultationId(n.getConsultationId());
	                return consultationDTO;
	            }).collect(Collectors.toList());

	            log.info("GET_ALL_CONSULTATIONS :: SUCCESS :: count={}", dto.size());

	            response.setStatus(200);
	            response.setData(dto);
	            response.setMessage("retrieved consultations successfully");
	            response.setSuccess(true);
	            return response;
	        } else {
	            log.warn("GET_ALL_CONSULTATIONS :: EMPTY_RESULT");

	            response.setStatus(200);
	            response.setMessage("consultations not found");
	            response.setSuccess(true);
	            return response;
	        }
	    } catch (Exception e) {
	        log.error("GET_ALL_CONSULTATIONS :: ERROR", e);

	        response.setStatus(500);
	        response.setMessage(e.getMessage());
	        response.setSuccess(false);
	        return response;
	    }
	}
	
	    
	    // USER SELECT FAVOURITE DOCTOR
	    
	public Response getDoctors(String cid, String serviceId) {
	    log.info("GET_DOCTORS :: START :: clinicId={}, serviceId={}", cid, serviceId);

	    Response response = new Response();

	    try {
	        ResponseEntity<Response> res = clinicAdminFeign.getDoctorByService(cid, serviceId);

	        if (res.getBody() != null) {
	            log.info("GET_DOCTORS :: SUCCESS :: clinicId={}, serviceId={}", cid, serviceId);

	            response.setData(res.getBody());
	            response.setStatus(200);
	            response.setMessage("fetched doctors successfully");
	            response.setSuccess(true);
	            return response;
	        } else {
	            log.warn("GET_DOCTORS :: NO_DOCTORS_FOUND :: clinicId={}, serviceId={}", cid, serviceId);

	            response.setStatus(200);
	            response.setMessage("No doctors found");
	            response.setSuccess(true);
	            return response;
	        }
	    } catch (FeignException e) {
	        log.error("GET_DOCTORS :: FEIGN_ERROR :: clinicId={}, serviceId={}",
	                cid, serviceId, e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }
	}
	    
   
	public ResponseEntity<Response> saveFavouriteDoctors(FavouriteDoctorsDTO favouriteDoctorsDTO) {

	    log.info("SAVE_FAVOURITE_DOCTOR :: START :: doctorId={}, hospitalId={}",
	            favouriteDoctorsDTO.getDoctorId(), favouriteDoctorsDTO.getHospitalId());

	    Response response = new Response();

	    try {
	        FavouriteDoctorsEntity favouriteDoctorsEntity = new FavouriteDoctorsEntity();
	        favouriteDoctorsEntity.setDoctorId(favouriteDoctorsDTO.getDoctorId());
	        favouriteDoctorsEntity.setHospitalId(favouriteDoctorsDTO.getHospitalId());
	        favouriteDoctorsEntity.setFavourite(true);

	        FavouriteDoctorsEntity f = customerFavouriteDoctors.save(favouriteDoctorsEntity);

	        if (f != null) {
	            log.info("SAVE_FAVOURITE_DOCTOR :: SUCCESS :: doctorId={}", f.getDoctorId());

	            response.setData(f);
	            response.setStatus(200);
	            response.setMessage("saved favourite doctor successfully");
	            response.setSuccess(true);
	            return ResponseEntity.status(200).body(response);
	        } else {
	            log.warn("SAVE_FAVOURITE_DOCTOR :: FAILED :: doctorId={}",
	                    favouriteDoctorsDTO.getDoctorId());

	            response.setStatus(404);
	            response.setMessage("Unable to Save Favourite Doctor");
	            response.setSuccess(false);
	            return ResponseEntity.status(404).body(response);
	        }
	    } catch (Exception e) {
	        log.error("SAVE_FAVOURITE_DOCTOR :: ERROR :: doctorId={}",
	                favouriteDoctorsDTO.getDoctorId(), e);

	        response.setStatus(500);
	        response.setMessage(e.getMessage());
	        response.setSuccess(false);
	        return ResponseEntity.status(500).body(response);
	    }
	}

	public Response getAllSavedFavouriteDoctors() {

	    log.info("GET_ALL_FAVOURITE_DOCTORS :: START");

	    Response response = new Response();
	    List<FavouriteDoctorsDTO> dto = new ArrayList<>();

	    try {
	        List<FavouriteDoctorsEntity> list = customerFavouriteDoctors.findAll();

	        log.debug("GET_ALL_FAVOURITE_DOCTORS :: DB_COUNT :: {}", list.size());

	        if (list != null) {
	            for (FavouriteDoctorsEntity f : list) {
	                FavouriteDoctorsDTO favouriteDoctors = new FavouriteDoctorsDTO();
	                favouriteDoctors.setDoctorId(f.getDoctorId());
	                favouriteDoctors.setHospitalId(f.getHospitalId());
	                favouriteDoctors.setFavourite(f.isFavourite());
	                dto.add(favouriteDoctors);
	            }

	            log.info("GET_ALL_FAVOURITE_DOCTORS :: SUCCESS :: count={}", dto.size());

	            response.setData(dto);
	            response.setStatus(200);
	            response.setMessage("saved favourite doctor successfully");
	            response.setSuccess(true);
	            return response;
	        } else {
	            log.warn("GET_ALL_FAVOURITE_DOCTORS :: EMPTY");

	            response.setStatus(200);
	            response.setMessage("Unable to Save Favourite Doctor");
	            response.setSuccess(true);
	            return response;
	        }
	    } catch (Exception e) {
	        log.error("GET_ALL_FAVOURITE_DOCTORS :: ERROR", e);

	        response.setStatus(500);
	        response.setMessage(e.getMessage());
	        response.setSuccess(false);
	        return response;
	    }
	}

	public Response getDoctorsSlots(String hid, String branchId, String doctorId) {

	    log.info("GET_DOCTOR_SLOTS :: START :: hospitalId={}, branchId={}, doctorId={}",
	            hid, branchId, doctorId);

	    Response response = new Response();

	    try {
	        ResponseEntity<Response> res = clinicAdminFeign.getDoctorSlot(hid, branchId, doctorId);

	        log.info("GET_DOCTOR_SLOTS :: SUCCESS :: hospitalId={}, branchId={}, doctorId={}",
	                hid, branchId, doctorId);

	        return res.getBody();

	    } catch (FeignException e) {
	        log.error("GET_DOCTOR_SLOTS :: FEIGN_ERROR :: hospitalId={}, branchId={}, doctorId={}",
	                hid, branchId, doctorId, e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }
	}

	public Response getReportsAndDoctorSaveDetails(String customerId) {

	    log.info("GET_REPORTS_AND_DOCTOR_DETAILS :: START :: customerId={}", customerId);

	    Response response = new Response();

	    try {
	        log.debug("GET_REPORTS_AND_DOCTOR_DETAILS :: CALLING_CLINIC_ADMIN :: customerId={}", customerId);

	        Response res = clinicAdminFeign.getReportsBycustomerId(customerId).getBody();

	        log.debug("GET_REPORTS_AND_DOCTOR_DETAILS :: REPORTS_RESPONSE_RECEIVED :: customerId={}", customerId);

	        List<ReportsDtoList> repots =
	                new ObjectMapper().convertValue(res.getData(), new TypeReference<List<ReportsDtoList>>() {});

	        log.info("GET_REPORTS_AND_DOCTOR_DETAILS :: REPORTS_COUNT :: customerId={}, count={}",
	                customerId, repots != null ? repots.size() : 0);

	        log.debug("GET_REPORTS_AND_DOCTOR_DETAILS :: CALLING_DOCTOR_SERVICE :: customerId={}", customerId);

	        Response rs = doctorServiceFeign.getDoctorSaveDetailsByCustomerId(customerId).getBody();

	        log.debug("GET_REPORTS_AND_DOCTOR_DETAILS :: DOCTOR_DETAILS_RESPONSE_RECEIVED :: customerId={}", customerId);

	        ObjectMapper mapper = new ObjectMapper();
	        mapper.registerModule(new JavaTimeModule());
	        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	        List<DoctorSaveDetailsDTO> doctorSaveDetailsDTO =
	                mapper.convertValue(rs.getData(), new TypeReference<List<DoctorSaveDetailsDTO>>() {});

	        log.info("GET_REPORTS_AND_DOCTOR_DETAILS :: DOCTOR_DETAILS_COUNT :: customerId={}, count={}",
	                customerId, doctorSaveDetailsDTO != null ? doctorSaveDetailsDTO.size() : 0);

	        ReportsAndDoctorSaveDetailsDto rd = new ReportsAndDoctorSaveDetailsDto();

	        if (repots != null && !repots.isEmpty()) {
	            log.debug("GET_REPORTS_AND_DOCTOR_DETAILS :: SETTING_REPORTS_DATA :: customerId={}", customerId);
	            rd.setReportsDtoList(repots);
	        }

	        if (doctorSaveDetailsDTO != null && !doctorSaveDetailsDTO.isEmpty()) {
	            log.debug("GET_REPORTS_AND_DOCTOR_DETAILS :: SETTING_DOCTOR_DETAILS_DATA :: customerId={}", customerId);
	            rd.setDoctorSaveDetailsDTO(doctorSaveDetailsDTO);
	        }

	        response.setStatus(200);
	        response.setMessage("Data fetched Successfully");
	        response.setSuccess(true);
	        response.setData(rd);

	        log.info("GET_REPORTS_AND_DOCTOR_DETAILS :: SUCCESS :: customerId={}", customerId);

	        return response;

	    } catch (FeignException e) {

	        log.error("GET_REPORTS_AND_DOCTOR_DETAILS :: FEIGN_ERROR :: customerId={}, status={}",
	                customerId, e.status(), e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }
	}

// BOOKING MANAGEMENT
	    
	public Response bookService(BookingRequset req) throws JsonProcessingException {

	    log.info("BOOK_SERVICE :: START :: customerMobile={}, serviceId={}, doctorId={}",
	            req.getMobileNumber(), req.getSubServiceId(), req.getDoctorId());

	    Response response = new Response();
	    BookingResponse bookingResponse = null;
	    ResponseEntity<ResponseStructure<BookingResponse>> res = null;
	    try {
	        log.debug("BOOK_SERVICE :: CALLING_BOOKING_SERVICE");
//	        if(req.getBodyPartId() != null) {
//	        for(QuestionsDTO dto:req.getQuestions()) {
//	        //Optional<QuestionsEntity> entity = physiotherapyRepo.findByQuestionId(dto.getQuestionId());
//	        if(entity.isPresent()) {
//	        dto.setQuestion(entity.get().getQuestion());}}
	        res = bookingFeign.bookService(req);
	        bookingResponse = res.getBody().getData();
//	        }else {
//	        res = bookingFeign.bookService(req);
//	        bookingResponse = res.getBody().getData();}
	        if(req.getTheraphyAnswers()!= null) {
	        
	        	if (req.getTheraphyAnswers() != null && !req.getTheraphyAnswers().isEmpty()) {

	        	    Map<String, List<TheraphyAnswersDTO>> map = req.getTheraphyAnswers();

	        	    for (Map.Entry<String, List<TheraphyAnswersDTO>> entry : map.entrySet()) {

	        	        String key = entry.getKey(); // e.g., "back"
	        	        List<TheraphyAnswersDTO> answersList = entry.getValue();

	        	        // 🔍 Fetch DB data based on key
	        	        QuestionsByPartEntity entity = getByKey.getByKey(key);

	        	        if (entity == null || entity.getQuestionsByPart() == null) {
	        	            continue;
	        	        }

	        	        List<QuestionsEntity> questionsList = entity.getQuestionsByPart().get(key);

	        	        if (questionsList == null || questionsList.isEmpty()  ) {
	        	            continue;
	        	        }

	        	        // 🔁 Match questionId and set question
	        	        for (TheraphyAnswersDTO dto : answersList) {

	        	            for (QuestionsEntity q : questionsList) {

	        	                if (q.getQuestionId() == dto.getQuestionId()) {
	        	                    dto.setQuestion(q.getQuestion());
	        	                    break; // stop once matched
	        	                }
	        	            }
	        	        }
	        	    }
	        	}
	        res = bookingFeign.bookService(req);
	        bookingResponse = res.getBody().getData();
	        }else {
	        res = bookingFeign.bookService(req);
	        bookingResponse = res.getBody().getData();}

	        
	        if (bookingResponse != null) {

	            log.info("BOOK_SERVICE :: BOOKING_SUCCESS :: bookingId={}",
	                    bookingResponse.getBookingId());

	            log.debug("BOOK_SERVICE :: UPDATING_DOCTOR_SLOT :: doctorId={}, branchId={}",
	                    bookingResponse.getDoctorId(), bookingResponse.getBranchId());

	            clinicAdminFeign.updateDoctorSlotWhileBooking(
	                    bookingResponse.getDoctorId(),
	                    bookingResponse.getBranchId(),
	                    bookingResponse.getServiceDate(),
	                    bookingResponse.getServicetime()
	            );

	            response.setData(res.getBody());
	            response.setStatus(res.getBody().getStatusCode());

	        } else {
	            log.warn("BOOK_SERVICE :: BOOKING_FAILED :: NULL_RESPONSE");

	            response.setStatus(res.getBody().getHttpStatus().value());
	            response.setData(res.getBody());
	        }

	    } catch (FeignException e) {
	        log.error("BOOK_SERVICE :: FEIGN_ERROR :: status={}", e.status(), e);

	        response.setStatus(e.status());
	        response.setMessage(e.getMessage());
	        response.setSuccess(false);
	    }

	    return response;
	}

	  	   
	public Response deleteBookedService(String id) {

	    log.info("DELETE_BOOKED_SERVICE :: START :: bookingId={}", id);

	    Response response = new Response();

	    try {
	        ResponseEntity<ResponseStructure<BookingResponse>> res =
	                bookingFeign.deleteBookedService(id);

	        ResponseStructure<BookingResponse> bookingResponse = res.getBody();

	        if (bookingResponse != null) {

	            log.info("DELETE_BOOKED_SERVICE :: SUCCESS :: bookingId={}", id);

	            response.setData(res.getBody());
	            response.setStatus(res.getBody().getStatusCode());
	            return response;

	        } else {
	            log.warn("DELETE_BOOKED_SERVICE :: NOT_FOUND :: bookingId={}", id);

	            response.setStatus(404);
	            response.setMessage("Unable To Delete Bookedservice");
	            response.setSuccess(false);
	            return response;
	        }

	    } catch (FeignException e) {
	        log.error("DELETE_BOOKED_SERVICE :: FEIGN_ERROR :: bookingId={}", id, e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }
	}
	    

	public Response getBookedService(String id) {

	    log.info("GET_BOOKED_SERVICE :: START :: bookingId={}", id);

	    Response response = new Response();

	    try {
	        ResponseEntity<ResponseStructure<BookingResponse>> res =
	                bookingFeign.getBookedService(id);

	        ResponseStructure<BookingResponse> bookingResponse = res.getBody();

	        if (bookingResponse != null) {

	            log.info("GET_BOOKED_SERVICE :: FOUND :: bookingId={}", id);

	            response.setData(res.getBody());
	            response.setStatus(res.getBody().getStatusCode());
	            return response;

	        } else {
	            log.warn("GET_BOOKED_SERVICE :: NOT_FOUND :: bookingId={}", id);

	            response.setStatus(200);
	            response.setMessage("Unable Get Bookings");
	            response.setSuccess(true);
	            return response;
	        }

	    } catch (FeignException e) {
	        log.error("GET_BOOKED_SERVICE :: FEIGN_ERROR :: bookingId={}", id, e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }
	}
	 
	public Response getCustomerBookedServices(String mobileNumber) {

	    log.info("GET_CUSTOMER_BOOKINGS :: START :: mobileNumber={}", mobileNumber);

	    Response response = new Response();

	    try {
	        ResponseEntity<ResponseStructure<List<BookingResponse>>> res =
	                bookingFeign.getCustomerBookedServices(mobileNumber);

	        ResponseStructure<List<BookingResponse>> respnse = res.getBody();

	        if (respnse != null) {

	            log.info("GET_CUSTOMER_BOOKINGS :: SUCCESS :: mobileNumber={}", mobileNumber);

	            response.setData(respnse);
	            response.setStatus(res.getBody().getStatusCode());
	            return response;

	        } else {
	            log.warn("GET_CUSTOMER_BOOKINGS :: NO_BOOKINGS :: mobileNumber={}", mobileNumber);

	            response.setStatus(200);
	            response.setMessage("Bookedservices Not Found ");
	            response.setSuccess(true);
	            return response;
	        }

	    } catch (FeignException e) {
	        log.error("GET_CUSTOMER_BOOKINGS :: FEIGN_ERROR :: mobileNumber={}", mobileNumber, e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }
	}
	    
	public ResponseStructure<List<BookingResponse>> getAllBookedServices() {

	    log.info("GET_ALL_BOOKINGS :: START");

	    try {
	        ResponseEntity<ResponseStructure<List<BookingResponse>>> responseEntity =
	                bookingFeign.getAllBookedService();

	        ResponseStructure<List<BookingResponse>> res = responseEntity.getBody();

	        if (res.getData() != null && !res.getData().isEmpty()) {

	            log.info("GET_ALL_BOOKINGS :: SUCCESS :: count={}", res.getData().size());

	            return new ResponseStructure<>(
	                    res.getData(),
	                    res.getMessage(),
	                    res.getHttpStatus(),
	                    res.getStatusCode()
	            );
	        } else {

	            log.warn("GET_ALL_BOOKINGS :: EMPTY");

	            return new ResponseStructure<>(
	                    null,
	                    "Bookings Not Found",
	                    res.getHttpStatus(),
	                    res.getStatusCode()
	            );
	        }

	    } catch (FeignException e) {
	        log.error("GET_ALL_BOOKINGS :: FEIGN_ERROR", e);
     System.out.println(e.getMessage());
	        return new ResponseStructure<>(
	                null,
	                ExtractFeignMessage.clearMessage(e),
	                HttpStatus.INTERNAL_SERVER_ERROR,
	                e.status()
	        );
	    }
	}
	    
	public Response getBookingByDoctorId(String doctorId) {

	    log.info("GET_BOOKINGS_BY_DOCTOR :: START :: doctorId={}", doctorId);

	    Response response = new Response();

	    try {
	        ResponseEntity<ResponseStructure<List<BookingResponse>>> res =
	                bookingFeign.getBookingByDoctorId(doctorId);

	        if (res.getBody() != null) {

	            log.info("GET_BOOKINGS_BY_DOCTOR :: FOUND :: doctorId={}", doctorId);

	            response.setData(res.getBody());
	            response.setStatus(res.getBody().getStatusCode());

	        } else {
	            log.warn("GET_BOOKINGS_BY_DOCTOR :: NOT_FOUND :: doctorId={}", doctorId);

	            response.setStatus(200);
	            response.setMessage("No Bookedservices Found For This DoctorId");
	            response.setSuccess(true);
	        }

	    } catch (FeignException e) {
	        log.error("GET_BOOKINGS_BY_DOCTOR :: FEIGN_ERROR :: doctorId={}", doctorId, e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }

	    return response;
	}
	   
	public Response getBookingByServiceId(String serviceId) {

	    log.info("GET_BOOKINGS_BY_SERVICE :: START :: serviceId={}", serviceId);

	    Response response = new Response();

	    try {
	        ResponseEntity<ResponseStructure<List<BookingResponse>>> res =
	                bookingFeign.getBookingByServiceId(serviceId);

	        if (res.getBody() != null) {

	            log.info("GET_BOOKINGS_BY_SERVICE :: FOUND :: serviceId={}", serviceId);

	            response.setStatus(res.getBody().getStatusCode());
	            response.setData(res.getBody());
	            return response;

	        } else {
	            log.warn("GET_BOOKINGS_BY_SERVICE :: NOT_FOUND :: serviceId={}", serviceId);

	            response.setStatus(200);
	            response.setMessage("Bookedservices Not Found");
	            response.setSuccess(true);
	            return response;
	        }

	    } catch (FeignException e) {
	        log.error("GET_BOOKINGS_BY_SERVICE :: FEIGN_ERROR :: serviceId={}", serviceId, e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }
	}
	   
	public Response getBookingByClinicId(String clinicId) {

	    log.info("GET_BOOKINGS_BY_CLINIC :: START :: clinicId={}", clinicId);

	    Response response = new Response();

	    try {
	        ResponseEntity<ResponseStructure<List<BookingResponse>>> res =
	                bookingFeign.getBookingByClinicId(clinicId);

	        if (res.getBody() != null) {

	            log.info("GET_BOOKINGS_BY_CLINIC :: FOUND :: clinicId={}", clinicId);

	            response.setStatus(res.getBody().getStatusCode());
	            response.setData(res.getBody());
	            return response;

	        } else {
	            log.warn("GET_BOOKINGS_BY_CLINIC :: NOT_FOUND :: clinicId={}", clinicId);

	            response.setStatus(200);
	            response.setMessage("Bookedservices Not Found");
	            response.setSuccess(true);
	            return response;
	        }

	    } catch (FeignException e) {
	        log.error("GET_BOOKINGS_BY_CLINIC :: FEIGN_ERROR :: clinicId={}", clinicId, e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }
	}

	    //RATING MANAGEMENT    
	public Response submitCustomerRating(CustomerRatingDomain ratingRequest) {

	    log.info("SUBMIT_RATING :: START :: branchId={}, doctorId={}, appointmentId={}",
	            ratingRequest.getBranchId(),
	            ratingRequest.getDoctorId(),
	            ratingRequest.getAppointmentId());

	    Response response = new Response();
	    ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");
	    String formattedTime = istTime.format(formatter);

	    try {
	        log.debug("SUBMIT_RATING :: CHECK_EXISTING_RATING :: branchId={}, doctorId={}, appointmentId={}",
	                ratingRequest.getBranchId(),
	                ratingRequest.getDoctorId(),
	                ratingRequest.getAppointmentId());

	        CustomerRating customerRating =
	                customerRatingRepository.findByBranchIdAndDoctorIdAndAppointmentId(
	                        ratingRequest.getBranchId(),
	                        ratingRequest.getDoctorId(),
	                        ratingRequest.getAppointmentId()
	                );

	        if (customerRating == null) {

	            log.info("SUBMIT_RATING :: NEW_RATING :: doctorRating={}, branchRating={}",
	                    ratingRequest.getDoctorRating(),
	                    ratingRequest.getBranchRating());

	            CustomerRating cRating = new CustomerRating(
	                    null,
	                    ratingRequest.getDoctorRating(),
	                    ratingRequest.getBranchRating(),
	                    ratingRequest.getFeedback(),
	                    ratingRequest.getHospitalId(),
	                    ratingRequest.getBranchId(),
	                    ratingRequest.getDoctorId(),
	                    ratingRequest.getCustomerMobileNumber(),
	                    ratingRequest.getPatientId(),
	                    ratingRequest.getPatientName(),
	                    ratingRequest.getAppointmentId(),
	                    true,
	                    formattedTime
	            );

	            customerRatingRepository.save(cRating);

	            response.setStatus(200);
	            response.setMessage("Successfully Submitted Rating");
	            response.setSuccess(true);

	        } else {
	            log.warn("SUBMIT_RATING :: ALREADY_RATED :: branchId={}, doctorId={}, appointmentId={}",
	                    ratingRequest.getBranchId(),
	                    ratingRequest.getDoctorId(),
	                    ratingRequest.getAppointmentId());

	            response.setStatus(409);
	            response.setMessage("Already Rated");
	            response.setSuccess(false);
	        }

	        log.debug("SUBMIT_RATING :: UPDATING_AVERAGE_RATINGS :: branchId={}, doctorId={}",
	                ratingRequest.getBranchId(),
	                ratingRequest.getDoctorId());

	        updateAvgRatingInClinicAndDoctorObject(
	                ratingRequest.getBranchId(),
	                ratingRequest.getDoctorId()
	        );

	        log.info("SUBMIT_RATING :: COMPLETED :: branchId={}, doctorId={}",
	                ratingRequest.getBranchId(),
	                ratingRequest.getDoctorId());

	        return response;

	    } catch (Exception e) {
	        log.error("SUBMIT_RATING :: ERROR :: branchId={}, doctorId={}",
	                ratingRequest.getBranchId(),
	                ratingRequest.getDoctorId(),
	                e);

	        response.setStatus(500);
	        response.setMessage(e.getMessage());
	        response.setSuccess(false);
	        return response;
	    }
	}
	   
	public void updateAvgRatingInClinicAndDoctorObject(String bId, String doctorId) {

	    log.info("UPDATE_AVG_RATING :: START :: branchId={}, doctorId={}", bId, doctorId);

	    try {
	        List<CustomerRating> clinicRatings =
	                customerRatingRepository.findByBranchId(bId);

	        List<CustomerRating> doctorRatings =
	                customerRatingRepository.findByDoctorId(doctorId);

	        log.debug("UPDATE_AVG_RATING :: RATINGS_COUNT :: branchRatings={}, doctorRatings={}",
	                clinicRatings.size(),
	                doctorRatings.size());

	        double avgClinicRating = clinicRatings.stream()
	                .mapToDouble(CustomerRating::getBranchRating)
	                .average()
	                .orElse(0.0);

	        double avgDoctorRating = doctorRatings.stream()
	                .mapToDouble(CustomerRating::getDoctorRating)
	                .average()
	                .orElse(0.0);

	        log.info("UPDATE_AVG_RATING :: CALCULATED :: branchAvg={}, doctorAvg={}",
	                avgClinicRating,
	                avgDoctorRating);

	        Response res = adminFeign.getBranchById(bId).getBody();
	        BranchDTO dto = new ObjectMapper().convertValue(res.getData(), BranchDTO.class);
	        dto.setBranchOverallRating(avgClinicRating);
	        adminFeign.updateBranch(bId, dto);

	        log.debug("UPDATE_AVG_RATING :: BRANCH_UPDATED :: branchId={}", bId);

	        ResponseEntity<Response> doctorsDTO =
	                clinicAdminFeign.getDoctorById(doctorId);

	        DoctorsDTO dctDto =
	                new ObjectMapper().convertValue(doctorsDTO.getBody().getData(), DoctorsDTO.class);
	        dctDto.setDoctorAverageRating(avgDoctorRating);
	        clinicAdminFeign.updateDoctorById(doctorId, dctDto);

	        log.info("UPDATE_AVG_RATING :: DOCTOR_UPDATED :: doctorId={}", doctorId);

	    } catch (FeignException e) {
	        log.error("UPDATE_AVG_RATING :: FEIGN_ERROR :: branchId={}, doctorId={}",
	                bId, doctorId, e);
	    }
	}
	   

	public Response getRatingForService(String bId, String doctorId) {

	    log.info("GET_RATING_BY_SERVICE :: START :: branchId={}, doctorId={}", bId, doctorId);

	    Response response = new Response();

	    try {
	        List<CustomerRatingDomain> listDto = new ArrayList<>();
	        List<CustomerRating> ratings =
	                customerRatingRepository.findByBranchIdAndDoctorId(bId, doctorId);

	        log.debug("GET_RATING_BY_SERVICE :: COUNT :: {}", ratings.size());

	        if (ratings.isEmpty()) {
	            log.warn("GET_RATING_BY_SERVICE :: NOT_FOUND :: branchId={}, doctorId={}", bId, doctorId);

	            response.setStatus(200);
	            response.setMessage("Rating Not Found");
	            response.setSuccess(true);
	            return response;
	        }

	        for (CustomerRating rating : ratings) {
	            CustomerRatingDomain c = new CustomerRatingDomain(
	                    rating.getDoctorRating(),
	                    rating.getBranchRating(),
	                    rating.getFeedback(),
	                    rating.getHospitalId(),
	                    rating.getBranchId(),
	                    rating.getDoctorId(),
	                    rating.getCustomerMobileNumber(),
	                    rating.getPatientId(),
	                    rating.getPatientName(),
	                    rating.getAppointmentId(),
	                    rating.getRated(),
	                    rating.getDateAndTimeAtRating()
	            );
	            listDto.add(c);
	        }

	        log.info("GET_RATING_BY_SERVICE :: SUCCESS :: branchId={}, doctorId={}", bId, doctorId);

	        response.setStatus(200);
	        response.setData(listDto);
	        response.setMessage("Rating fetched successfully");
	        response.setSuccess(true);
	        return response;

	    } catch (Exception e) {
	        log.error("GET_RATING_BY_SERVICE :: ERROR :: branchId={}, doctorId={}", bId, doctorId, e);

	        response.setStatus(500);
	        response.setMessage(e.getMessage());
	        response.setSuccess(false);
	        return response;
	    }
	}
	   
	public Response getRatingForServiceBydoctorId(String doctorId) {

	    log.info("GET_RATING_BY_DOCTOR :: START :: doctorId={}", doctorId);

	    Response response = new Response();

	    try {
	        List<CustomerRatingDomain> listDto = new ArrayList<>();
	        List<CustomerRating> ratings =
	                customerRatingRepository.findByDoctorId(doctorId);

	        log.debug("GET_RATING_BY_DOCTOR :: COUNT :: {}", ratings.size());

	        if (ratings.isEmpty()) {
	            log.warn("GET_RATING_BY_DOCTOR :: NOT_FOUND :: doctorId={}", doctorId);

	            response.setStatus(200);
	            response.setMessage("Rating Not Found");
	            response.setSuccess(true);
	            return response;
	        }

	        for (CustomerRating rating : ratings) {
	            CustomerRatingDomain c = new CustomerRatingDomain(
	                    rating.getDoctorRating(),
	                    rating.getBranchRating(),
	                    rating.getFeedback(),
	                    rating.getHospitalId(),
	                    rating.getBranchId(),
	                    rating.getDoctorId(),
	                    rating.getCustomerMobileNumber(),
	                    rating.getPatientId(),
	                    rating.getPatientName(),
	                    rating.getAppointmentId(),
	                    rating.getRated(),
	                    rating.getDateAndTimeAtRating()
	            );
	            listDto.add(c);
	        }

	        log.info("GET_RATING_BY_DOCTOR :: SUCCESS :: doctorId={}", doctorId);

	        response.setStatus(200);
	        response.setData(listDto);
	        response.setMessage("Rating fetched successfully");
	        response.setSuccess(true);
	        return response;

	    } catch (Exception e) {
	        log.error("GET_RATING_BY_DOCTOR :: ERROR :: doctorId={}", doctorId, e);

	        response.setStatus(500);
	        response.setMessage(e.getMessage());
	        response.setSuccess(false);
	        return response;
	    }
	}
	   
	     
	public Response getAverageRating(String branchId, String doctorId) {

	    log.info("GET_AVERAGE_RATING :: START :: branchId={}, doctorId={}", branchId, doctorId);

	    Response response = new Response();

	    try {
	        ResponseEntity<Response> ratings =
	                clinicAdminFeign.getAverageRatings(branchId, doctorId);

	        if (!ratings.hasBody()) {
	            log.warn("GET_AVERAGE_RATING :: NOT_FOUND :: branchId={}, doctorId={}", branchId, doctorId);

	            response.setStatus(200);
	            response.setMessage("Rating Not Found");
	            response.setSuccess(true);
	            return response;
	        } else {
	            log.info("GET_AVERAGE_RATING :: SUCCESS :: branchId={}, doctorId={}", branchId, doctorId);
	            return ratings.getBody();
	        }

	    } catch (FeignException e) {
	        log.error("GET_AVERAGE_RATING :: FEIGN_ERROR :: branchId={}, doctorId={}",
	                branchId, doctorId, e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }
	}
	
	public Response getAverageRatingByDoctorId(String doctorId) {

	    log.info("GET_AVERAGE_RATING_BY_DOCTOR :: START :: doctorId={}", doctorId);

	    Response response = new Response();

	    try {
	        ResponseEntity<Response> ratings =
	                clinicAdminFeign.getAverageRatingsByDoctorId(doctorId);

	        if (!ratings.hasBody()) {
	            log.warn("GET_AVERAGE_RATING_BY_DOCTOR :: NOT_FOUND :: doctorId={}", doctorId);

	            response.setStatus(200);
	            response.setMessage("Rating Not Found");
	            response.setSuccess(true);
	            return response;
	        } else {
	            log.info("GET_AVERAGE_RATING_BY_DOCTOR :: SUCCESS :: doctorId={}", doctorId);
	            return ratings.getBody();
	        }

	    } catch (FeignException e) {
	        log.error("GET_AVERAGE_RATING_BY_DOCTOR :: FEIGN_ERROR :: doctorId={}", doctorId, e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }
	}


    //GETDOCTORSBYSUBSERVICEID

	@Override
	public Response getDoctorsandHospitalDetails(String hospitalId, String subServiceId)
	        throws JsonProcessingException {

	    log.info("GET_DOCTORS_AND_HOSPITAL :: START :: hospitalId={}, subServiceId={}",
	            hospitalId, subServiceId);

	    Response response = new Response();

	    try {
	        log.debug("GET_DOCTORS_AND_HOSPITAL :: FETCHING_HOSPITAL :: hospitalId={}", hospitalId);

	        Response hospitalResponse = adminFeign.getClinicById(hospitalId);

	        if (hospitalResponse.getData() != null) {

	            log.debug("GET_DOCTORS_AND_HOSPITAL :: FETCHING_DOCTORS :: hospitalId={}, subServiceId={}",
	                    hospitalId, subServiceId);

	            ResponseEntity<Response> doctorsResponse =
	                    clinicAdminFeign.getDoctorsBySubServiceId(hospitalId, subServiceId);

	            Object obj = doctorsResponse.getBody().getData();

	            List<DoctorsDTO> doctors =
	                    new ObjectMapper().convertValue(obj, new TypeReference<List<DoctorsDTO>>() {});

	            log.info("GET_DOCTORS_AND_HOSPITAL :: DOCTORS_COUNT :: {}",
	                    doctors != null ? doctors.size() : 0);

	            if (doctors != null && !doctors.isEmpty()) {

	                ClinicDTO hospital =
	                        new ObjectMapper().convertValue(hospitalResponse.getData(), ClinicDTO.class);

	                ClinicAndDoctorsResponse combinedData =
	                        new ClinicAndDoctorsResponse(hospital, doctors);

	                response.setSuccess(true);
	                response.setData(combinedData);
	                response.setMessage("Hospital and doctors fetched successfully");
	                response.setStatus(200);

	                log.info("GET_DOCTORS_AND_HOSPITAL :: SUCCESS :: hospitalId={}", hospitalId);

	            } else {
	                log.warn("GET_DOCTORS_AND_HOSPITAL :: NO_DOCTORS_FOUND :: hospitalId={}, subServiceId={}",
	                        hospitalId, subServiceId);

	                response.setData(doctorsResponse.getBody());
	                response.setStatus(doctorsResponse.getBody().getStatus());
	            }

	        } else {
	            log.warn("GET_DOCTORS_AND_HOSPITAL :: HOSPITAL_NOT_FOUND :: hospitalId={}", hospitalId);

	            response.setData(hospitalResponse);
	            response.setStatus(hospitalResponse.getStatus());
	        }

	    } catch (FeignException e) {
	        log.error("GET_DOCTORS_AND_HOSPITAL :: FEIGN_ERROR :: hospitalId={}, subServiceId={}",
	                hospitalId, subServiceId, e);

	        response.setSuccess(false);
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setStatus(500);
	    }

	    return response;
	}

// GETHOSPITALANDDOCTORINFORMSTION

	@Override
	public Response getHospitalsAndDoctorsDetailsBySubServiceId(String subServiceId) {

	    log.info("GET_HOSPITALS_AND_DOCTORS_BY_SUBSERVICE :: START :: subServiceId={}", subServiceId);

	    Response response = new Response();

	    try {
	        ResponseEntity<Response> doctorsResponse =
	                clinicAdminFeign.getHospitalAndDoctorUsingSubServiceId(subServiceId);

	        Response res = doctorsResponse.getBody();

	        if (res != null) {

	            log.info("GET_HOSPITALS_AND_DOCTORS_BY_SUBSERVICE :: SUCCESS :: subServiceId={}", subServiceId);

	            response.setData(res);
	            response.setStatus(res.getStatus());

	        } else {
	            log.warn("GET_HOSPITALS_AND_DOCTORS_BY_SUBSERVICE :: NO_DATA :: subServiceId={}", subServiceId);

	            response.setSuccess(true);
	            response.setMessage("Details Not Found");
	            response.setStatus(200);
	        }

	    } catch (FeignException e) {
	        log.error("GET_HOSPITALS_AND_DOCTORS_BY_SUBSERVICE :: FEIGN_ERROR :: subServiceId={}",
	                subServiceId, e);

	        response.setSuccess(false);
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setStatus(500);
	    }

	    return response;
	}


///CATEGORYANDSERVICES


	@Override
	public Response getAllCategory() {

	    log.info("GET_ALL_CATEGORY :: START");

	    Response response = new Response();

	    try {
	        ResponseEntity<ResponseStructure<List<CategoryDto>>> res =
	                categoryServicesFeign.getAllCategory();

	        if (res.hasBody()) {
	            ResponseStructure<List<CategoryDto>> rs = res.getBody();

	            log.info("GET_ALL_CATEGORY :: SUCCESS :: count={}",
	                    rs.getData() != null ? rs.getData().size() : 0);

	            response.setData(rs);
	            response.setStatus(rs.getHttpStatus().value());
	        }

	    } catch (FeignException e) {
	        log.error("GET_ALL_CATEGORY :: FEIGN_ERROR", e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	    }

	    return response;
	}


	@Override
	public Response getServiceById(String categoryId) {

	    log.info("GET_SERVICE_BY_CATEGORY :: START :: categoryId={}", categoryId);

	    Response response = new Response();

	    try {
	        ResponseEntity<ResponseStructure<List<ServicesDto>>> res =
	                categoryServicesFeign.getServiceById(categoryId);

	        if (res.getBody() != null) {
	            ResponseStructure<List<ServicesDto>> rs = res.getBody();

	            log.info("GET_SERVICE_BY_CATEGORY :: SUCCESS :: categoryId={}, count={}",
	                    categoryId,
	                    rs.getData() != null ? rs.getData().size() : 0);

	            response.setData(rs);
	            response.setStatus(rs.getHttpStatus().value());
	        }

	    } catch (FeignException e) {
	        log.error("GET_SERVICE_BY_CATEGORY :: FEIGN_ERROR :: categoryId={}", categoryId, e);

	        response.setStatus(e.status());
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	    }

	    return response;
	}
	
	@Override
	public Response getSubServicesByServiceId(String serviceId) {

	    log.info("GET_SUBSERVICES_BY_SERVICE :: START :: serviceId={}", serviceId);

	    Response response = new Response();

	    try {
	        ResponseEntity<Response> res =
	                categoryServicesFeign.getSubServicesByServiceId(serviceId);

	        log.info("GET_SUBSERVICES_BY_SERVICE :: SUCCESS :: serviceId={}", serviceId);

	        return res.getBody();

	    } catch (FeignException e) {
	        log.error("GET_SUBSERVICES_BY_SERVICE :: FEIGN_ERROR :: serviceId={}", serviceId, e);

	        response.setStatus(500);
	        response.setMessage(ExtractFeignMessage.clearMessage(e));
	        response.setSuccess(false);
	        return response;
	    }
	}


	public Response getSubServiceInfoBySubServiceId(String subServiceId)
	        throws JsonProcessingException {

	    log.info("GET_SUBSERVICE_INFO :: START :: subServiceId={}", subServiceId);

	    Response responseObj = new Response();

	    try {
	        ResponseEntity<ResponseStructure<List<SubServicesDto>>> res =
	                categoryServicesFeign.retrieveSubServicesBySubServiceId(subServiceId);

	        List<SubServicesDetailsDto> hospitalAndSubServiceInfo = new ArrayList<>();

	        if (res.getBody().getData() != null && !res.getBody().getData().isEmpty()) {

	            log.debug("GET_SUBSERVICE_INFO :: SUBSERVICE_COUNT :: {}",
	                    res.getBody().getData().size());

	            for (SubServicesDto subsrvice : res.getBody().getData()) {

	                if (subsrvice.getSubServiceId().equals(subServiceId)) {

	                    Response respnse =
	                            adminFeign.getClinicById(subsrvice.getHospitalId());

	                    ClinicDTO clncDto =
	                            new ObjectMapper().convertValue(respnse.getData(), ClinicDTO.class);

	                    if (clncDto != null) {

	                        SubServicesDetailsDto subServicesDetailsDto =
	                                new SubServicesDetailsDto();

	                        subServicesDetailsDto.setServiceName(subsrvice.getServiceName());
	                        subServicesDetailsDto.setSubServiceName(subsrvice.getSubServiceName());
	                        subServicesDetailsDto.setSubServicePrice(subsrvice.getFinalCost());
	                        subServicesDetailsDto.setDiscountedCost(subsrvice.getDiscountedCost());
	                        subServicesDetailsDto.setDiscountPercentage(subsrvice.getDiscountPercentage());
	                        subServicesDetailsDto.setPrice(subsrvice.getPrice());
	                        subServicesDetailsDto.setTaxAmount(subsrvice.getTaxAmount());
	                        subServicesDetailsDto.setConsultationFee(subsrvice.getConsultationFee());

	                        Response response =
	                                adminFeign.getClinicById(subsrvice.getHospitalId());

	                        if (response.getData() != null) {
	                            ClinicDTO clinicDto =
	                                    new ObjectMapper().convertValue(response.getData(), ClinicDTO.class);

	                            subServicesDetailsDto.setHospitalId(clinicDto.getHospitalId());
	                            subServicesDetailsDto.setHospitalName(clinicDto.getName());
	                            subServicesDetailsDto.setHospitalLogo(clinicDto.getHospitalLogo());
	                            subServicesDetailsDto.setRecommanded(clinicDto.isRecommended());
	                            subServicesDetailsDto.setHospitalOverallRating(
	                                    clinicDto.getHospitalOverallRating());
	                            subServicesDetailsDto.setWebsite(clinicDto.getWebsite());
	                            subServicesDetailsDto.setWalkthrough(clinicDto.getWalkthrough());
	                            subServicesDetailsDto.setCity(clinicDto.getCity());
	                        }

	                        hospitalAndSubServiceInfo.add(subServicesDetailsDto);
	                    }
	                }
	            }

	            if (!hospitalAndSubServiceInfo.isEmpty()) {
	                log.info("GET_SUBSERVICE_INFO :: SUCCESS :: subServiceId={}", subServiceId);

	                responseObj.setData(hospitalAndSubServiceInfo);
	                responseObj.setStatus(200);
	                responseObj.setSuccess(true);

	            } else {
	                log.warn("GET_SUBSERVICE_INFO :: NO_DATA_AFTER_FILTER :: subServiceId={}", subServiceId);

	                responseObj.setMessage("SubServices Data Not Found ");
	                responseObj.setStatus(200);
	            }

	        } else {
	            log.warn("GET_SUBSERVICE_INFO :: NO_SUBSERVICE_DATA :: subServiceId={}", subServiceId);

	            responseObj.setMessage("No SubService Data Found ");
	            responseObj.setStatus(200);
	        }

	    } catch (FeignException e) {
	        log.error("GET_SUBSERVICE_INFO :: FEIGN_ERROR :: subServiceId={}", subServiceId, e);

	        responseObj.setMessage(ExtractFeignMessage.clearMessage(e));
	        responseObj.setStatus(e.status());
	        responseObj.setSuccess(false);
	    }

	    return responseObj;
	}


	

private static final double EARTH_RADIUS_KM = 6371.0;
private double haversine(double lat1, double lon1, double lat2, double lon2) {
    // Convert degrees to radians
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    
    lat1 = Math.toRadians(lat1);
    lat2 = Math.toRadians(lat2);

    // Haversine formula
    double a = Math.pow(Math.sin(dLat / 2), 2)
             + Math.cos(lat1) * Math.cos(lat2)
             * Math.pow(Math.sin(dLon / 2), 2);

    double c = 2 * Math.asin(Math.sqrt(a));

    return EARTH_RADIUS_KM * c; // Distance in KM
}


//CUSTOMERNOTIFICATION

public Response getBranchesInfoBySubServiceId(String clinicId,String subServiceId,
        String latitude,String longtitude) throws JsonProcessingException {

    log.info("GET_BRANCHES_BY_SUBSERVICE :: START :: clinicId={}, subServiceId={}, lat={}, lon={}",
            clinicId, subServiceId, latitude, longtitude);

    Response responseObj = new Response();

    try {
        ResponseEntity<ResponseStructure<SubServicesDto>> res =
                categoryServicesFeign.getSubServiceBySubServiceId(clinicId, subServiceId);

        log.debug("GET_BRANCHES_BY_SUBSERVICE :: SUBSERVICE_RESPONSE_RECEIVED");

        BranchInfo hospitalAndSubServiceInfo = new BranchInfo();

        if (res.getBody().getData() != null) {

            SubServicesDto subsrvice = res.getBody().getData();

            log.info("GET_BRANCHES_BY_SUBSERVICE :: SUBSERVICE_FOUND :: hospitalId={}",
                    subsrvice.getHospitalId());

            Response rs = adminFeign.getClinicById(subsrvice.getHospitalId());
            ClinicDTO cDto = new ObjectMapper().convertValue(rs.getData(), ClinicDTO.class);
           
            if (cDto != null) {

                log.debug("GET_BRANCHES_BY_SUBSERVICE :: CLINIC_FOUND :: hospitalId={}",
                        subsrvice.getHospitalId());

                hospitalAndSubServiceInfo.setServiceName(subsrvice.getServiceName());
                hospitalAndSubServiceInfo.setSubServiceName(subsrvice.getSubServiceName());
                hospitalAndSubServiceInfo.setSubServicePrice(subsrvice.getFinalCost());
                hospitalAndSubServiceInfo.setDiscountedCost(subsrvice.getDiscountedCost());
                hospitalAndSubServiceInfo.setDiscountPercentage(subsrvice.getDiscountPercentage());
                hospitalAndSubServiceInfo.setPrice(subsrvice.getPrice());
                hospitalAndSubServiceInfo.setTaxAmount(subsrvice.getTaxAmount());
                hospitalAndSubServiceInfo.setConsultationFee(subsrvice.getConsultationFee());

                Response response =
                        adminFeign.getBranchByClinicId(subsrvice.getHospitalId()).getBody();

//                Response respnse =
//                        adminFeign.getClinicById(subsrvice.getHospitalId());

//                if (response.getData() != null) {
//
//                    ClinicDTO clinicDto =
//                            new ObjectMapper().convertValue(respnse.getData(), ClinicDTO.class);

                    hospitalAndSubServiceInfo.setHospitalId(cDto.getHospitalId());
                    hospitalAndSubServiceInfo.setHospitalName(cDto.getName());
                    hospitalAndSubServiceInfo.setHospitalLogo(cDto.getHospitalLogo());
                    hospitalAndSubServiceInfo.setRecommanded(cDto.isRecommended());
                    hospitalAndSubServiceInfo.setHospitalOverallRating(
                    		cDto.getHospitalOverallRating());
                    hospitalAndSubServiceInfo.setWebsite(cDto.getWebsite());
                    hospitalAndSubServiceInfo.setWalkthrough(cDto.getWalkthrough());
                    hospitalAndSubServiceInfo.setCity(cDto.getCity());
                
                List<BranchDTO> branchDto =
                        new ObjectMapper().convertValue(response.getData(),
                                new TypeReference<List<BranchDTO>>() {});

                log.info("GET_BRANCHES_BY_SUBSERVICE :: BRANCH_COUNT :: {}",
                        branchDto != null ? branchDto.size() : 0);

                List<BranchDTO> branchDtoWithKms =
                        branchDto.stream().map(n -> {
                            double d = haversine(
                                    Double.valueOf(latitude),
                                    Double.valueOf(longtitude),
                                    Double.valueOf(n.getLatitude()),
                                    Double.valueOf(n.getLongitude())
                            );
                            n.setDistance(d);
                            n.setKms(String.format("%.1f", d) + " km");
                            return n;
                        }).toList();

                List<BranchDTO> branchDtoWithKmsAsndng =
                        branchDtoWithKms.stream()
                                .sorted(Comparator.comparingDouble(BranchDTO::getDistance))
                                .toList();

                hospitalAndSubServiceInfo.setBranches(branchDtoWithKmsAsndng);

            } else {
                log.warn("GET_BRANCHES_BY_SUBSERVICE :: CLINIC_NOT_FOUND :: hospitalId={}",
                        subsrvice.getHospitalId());

                responseObj.setMessage("Hospital Not Found ");
                responseObj.setStatus(200);
            }

            if (hospitalAndSubServiceInfo != null) {
                log.info("GET_BRANCHES_BY_SUBSERVICE :: SUCCESS :: subServiceId={}", subServiceId);

                responseObj.setData(hospitalAndSubServiceInfo);
                responseObj.setStatus(200);
                responseObj.setSuccess(true);
            } else {
                log.warn("GET_BRANCHES_BY_SUBSERVICE :: SUBSERVICE_NOT_FOUND :: subServiceId={}",
                        subServiceId);

                responseObj.setMessage("SubServices Not Found ");
                responseObj.setStatus(200);
            }

        } else {
            log.warn("GET_BRANCHES_BY_SUBSERVICE :: NO_SUBSERVICE_DATA :: subServiceId={}",
                    subServiceId);

            responseObj.setMessage("No SubService Found ");
            responseObj.setStatus(200);
        }

    } catch (FeignException e) {
        log.error("GET_BRANCHES_BY_SUBSERVICE :: FEIGN_ERROR :: clinicId={}, subServiceId={}",
                clinicId, subServiceId, e);

        responseObj.setMessage(e.getMessage());
        responseObj.setStatus(e.status());
        responseObj.setSuccess(false);
    }

    return responseObj;
}

public ResponseEntity<?> getInProgressAppointments(String mnumber) {

    log.info("GET_INPROGRESS_APPOINTMENTS :: START :: mobileNumber={}", mnumber);

    try {
        log.info("GET_INPROGRESS_APPOINTMENTS :: SUCCESS :: mobileNumber={}", mnumber);
        return bookingFeign.inProgressAppointments(mnumber);

    } catch (FeignException e) {
        log.error("GET_INPROGRESS_APPOINTMENTS :: FEIGN_ERROR :: mobileNumber={}", mnumber, e);

        ResBody<List<NotificationToCustomer>> res =
                new ResBody<>(ExtractFeignMessage.clearMessage(e), e.status(), null);

        return ResponseEntity.status(e.status()).body(res);
    }
}


public ResponseEntity<?> customerLogin(CustomerLoginDTO dto) {

    log.info("CUSTOMER_LOGIN :: START :: username={}", dto.getUserName());

    try {
        log.info("CUSTOMER_LOGIN :: SUCCESS :: username={}", dto.getUserName());
        return clinicAdminFeign.login(dto);

    } catch (FeignException e) {
        log.error("CUSTOMER_LOGIN :: FEIGN_ERROR :: username={}",
        		dto.getUserName(), e);

        Response res = new Response();
        res.setMessage(ExtractFeignMessage.clearMessage(e));
        res.setStatus(e.status());
        res.setSuccess(false);

        return ResponseEntity.status(e.status()).body(res);
    }
}



@Override
public Response getDoctorsByHospitalBranchAndSubService(
        String hospitalId,
        String branchId,
        String subServiceId) throws JsonProcessingException {

    log.info("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE :: START :: hospitalId={}, branchId={}, subServiceId={}",
            hospitalId, branchId, subServiceId);

    Response response = new Response();

    try {
        Response hospitalResponse = adminFeign.getClinicById(hospitalId);

        if (hospitalResponse.getData() != null) {

            log.debug("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE :: HOSPITAL_FOUND :: hospitalId={}",
                    hospitalId);

            ResponseEntity<Response> doctorsResponse =
                    clinicAdminFeign.getDoctorsByHospitalBranchAndSubService(
                            hospitalId, branchId, subServiceId);

            Object obj = doctorsResponse.getBody().getData();

            List<DoctorsDTO> doctors =
                    new ObjectMapper().convertValue(obj, new TypeReference<List<DoctorsDTO>>() {});

            log.info("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE :: DOCTOR_COUNT :: {}",
                    doctors != null ? doctors.size() : 0);

            if (doctors != null && !doctors.isEmpty()) {

                ClinicDTO hospital =
                        new ObjectMapper().convertValue(hospitalResponse.getData(), ClinicDTO.class);

                ClinicAndDoctorsResponse combinedData =
                        new ClinicAndDoctorsResponse(hospital, doctors);

                response.setSuccess(true);
                response.setData(combinedData);
                response.setMessage("Hospital and doctors fetched successfully");
                response.setStatus(200);

                log.info("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE :: SUCCESS");

            } else {
                log.warn("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE :: NO_DOCTORS_FOUND");

                response.setData(doctorsResponse.getBody());
                response.setStatus(doctorsResponse.getBody().getStatus());
            }

        } else {
            log.warn("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE :: HOSPITAL_NOT_FOUND :: hospitalId={}",
                    hospitalId);

            response.setData(hospitalResponse);
            response.setStatus(hospitalResponse.getStatus());
        }

    } catch (FeignException e) {
        log.error("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE :: FEIGN_ERROR :: hospitalId={}, branchId={}, subServiceId={}",
                hospitalId, branchId, subServiceId, e);

        response.setSuccess(false);
        response.setMessage(ExtractFeignMessage.clearMessage(e));
        response.setStatus(500);
    }

    return response;
}

@Override
public Response getDoctorsByHospitalBranchAndSubService(
        String hospitalId,
        String branchId,
        String subServiceId,
        int consultationType) throws JsonProcessingException {

    log.info("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE_CT :: START :: hospitalId={}, branchId={}, subServiceId={}, consultationType={}",
            hospitalId, branchId, subServiceId, consultationType);

    Response response = new Response();

    try {
        Response hospitalResponse = adminFeign.getClinicById(hospitalId);

        if (hospitalResponse.getData() != null) {

            ResponseEntity<Response> doctorsResponse =
                    clinicAdminFeign.getDoctorsByHospitalBranchAndSubService(
                            hospitalId, branchId, subServiceId);

            Object obj = doctorsResponse.getBody().getData();

            List<DoctorsDTO> doctors =
                    new ObjectMapper().convertValue(obj, new TypeReference<List<DoctorsDTO>>() {});

            log.debug("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE_CT :: TOTAL_DOCTORS :: {}",
                    doctors != null ? doctors.size() : 0);

            if (!doctors.isEmpty()) {

                List<DoctorsDTO> filteredDoctors = doctors.stream()
                        .filter(dto -> {
                            if (dto.getConsultation() == null) return false;
                            switch (consultationType) {
//                                case 1:
//                                    return dto.getConsultation().getInClinic() == 1;
//                                case 2:
//                                    return dto.getConsultation().getVideoOrOnline() == 2;
//                                case 3:
//                                    return dto.getConsultation().getServiceAndTreatments() == 3;
//
                            case 1:
                                return Optional.ofNullable(dto.getConsultation().getInClinic()).orElse(0) == 1;

                            case 2:
                                return Optional.ofNullable(dto.getConsultation().getVideoOrOnline()).orElse(0) == 2;

                            case 3:
                                return Optional.ofNullable(dto.getConsultation().getServiceAndTreatments()).orElse(0) == 3;
                            default:
                            
                                    return false;
                            }
                        })
                        .collect(Collectors.toList());

                log.info("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE_CT :: FILTERED_DOCTORS :: {}",
                        filteredDoctors.size());

                ClinicDTO hospital =
                        new ObjectMapper().convertValue(hospitalResponse.getData(), ClinicDTO.class);

                ClinicAndDoctorsResponse combinedData =
                        new ClinicAndDoctorsResponse(hospital, filteredDoctors);

                response.setSuccess(true);
                response.setData(combinedData);
                response.setMessage("Hospital and doctors fetched successfully");
                response.setStatus(HttpStatus.OK.value());

            } else {
                log.warn("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE_CT :: NO_DOCTORS_FOUND");

                response.setData(doctorsResponse.getBody());
                response.setStatus(doctorsResponse.getBody().getStatus());
            }

        } else {
            log.warn("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE_CT :: HOSPITAL_NOT_FOUND");

            response.setData(hospitalResponse);
            response.setStatus(hospitalResponse.getStatus());
        }

    } catch (FeignException e) {
        log.error("GET_DOCTORS_BY_HOSPITAL_BRANCH_SUBSERVICE_CT :: FEIGN_ERROR",
                e);

        response.setSuccess(false);
        response.setMessage(ExtractFeignMessage.clearMessage(e));
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    return response;
}

public ResponseEntity<Response> getRecommendedClinicsAndOnDoctors(String keyPoints) {

    log.info("GET_RECOMMENDED_CLINICS_DOCTORS :: START :: keyPoints={}", keyPoints);

    try {
        log.info("GET_RECOMMENDED_CLINICS_DOCTORS :: SUCCESS");
        return clinicAdminFeign.getRecommendedClinicsAndOnDoctors(keyPoints);

    } catch (FeignException e) {
        log.error("GET_RECOMMENDED_CLINICS_DOCTORS :: FEIGN_ERROR :: keyPoints={}", keyPoints, e);

        Response res = new Response();
        res.setMessage(ExtractFeignMessage.clearMessage(e));
        res.setStatus(e.status());
        res.setSuccess(false);

        return ResponseEntity.status(e.status()).body(res);
    }
}

public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingsByBranchId(String branchId) {

    log.info("GET_BOOKINGS_BY_BRANCH :: START :: branchId={}", branchId);

    ResponseStructure<List<BookingResponse>> res =
            new ResponseStructure<>();

    try {
        log.info("GET_BOOKINGS_BY_BRANCH :: SUCCESS :: branchId={}", branchId);
        return bookingFeign.getAllBookedServicesByBranchId(branchId);

    } catch (FeignException e) {
        log.error("GET_BOOKINGS_BY_BRANCH :: FEIGN_ERROR :: branchId={}", branchId, e);

        res = new ResponseStructure<>(
                null,
                ExtractFeignMessage.clearMessage(e),
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.status()
        );
    }

    return ResponseEntity.status(res.getStatusCode()).body(res);
}

@Override
public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingsByClinicIdWithBranchId(
        String clinicId, String branchId) {

    log.info("GET_BOOKINGS_BY_CLINIC_BRANCH :: START :: clinicId={}, branchId={}",
            clinicId, branchId);

    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

    try {
        log.info("GET_BOOKINGS_BY_CLINIC_BRANCH :: SUCCESS");
        return bookingFeign.getBookedServicesByClinicIdWithBranchId(clinicId, branchId);

    } catch (FeignException e) {
        log.error("GET_BOOKINGS_BY_CLINIC_BRANCH :: FEIGN_ERROR", e);

        res = new ResponseStructure<>(
                null,
                ExtractFeignMessage.clearMessage(e),
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.status()
        );
        return ResponseEntity.status(res.getStatusCode()).body(res);
    }
}


@Override
public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingsByCustomerId(String customerId) {

    log.info("GET_BOOKINGS_BY_CUSTOMER :: START :: customerId={}", customerId);

    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

    try {
        ResponseEntity<ResponseStructure<List<BookingResponse>>> response =
                bookingFeign.getBookingByCustomerId(customerId);

        log.info("GET_BOOKINGS_BY_CUSTOMER :: SUCCESS :: customerId={}", customerId);

        return response;

    } catch (FeignException e) {

        log.error("GET_BOOKINGS_BY_CUSTOMER :: FEIGN_ERROR :: customerId={}",
                customerId, e);

        res = new ResponseStructure<>(
                null,
                ExtractFeignMessage.clearMessage(e),
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.status()
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }
}

@Override
public ResponseEntity<?> getInprogressBookingsByCustomerId(String customerId) {

    log.info("GET_INPROGRESS_BOOKINGS_BY_CUSTOMER :: START :: customerId={}", customerId);

    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

    try {
        ResponseEntity<?> response =
                bookingFeign.getInprogressAppointmentsByCustomerId(customerId);

        log.info("GET_INPROGRESS_BOOKINGS_BY_CUSTOMER :: SUCCESS :: customerId={}", customerId);

        return response;

    } catch (FeignException e) {

        log.error("GET_INPROGRESS_BOOKINGS_BY_CUSTOMER :: FEIGN_ERROR :: customerId={}",
                customerId, e);

        res = new ResponseStructure<>(
                null,
                ExtractFeignMessage.clearMessage(e),
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.status()
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }
}

@Override
public ResponseEntity<?> getInprogressBookingsByPatientId(String patientId, String clinicId) {

    log.info("GET_INPROGRESS_BOOKINGS_BY_PATIENT :: START :: patientId={}, clinicId={}",
            patientId, clinicId);

    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

    try {
        ResponseEntity<?> response =
                bookingFeign.getInprogressAppointmentsByPatientId(patientId, clinicId);

        log.info("GET_INPROGRESS_BOOKINGS_BY_PATIENT :: SUCCESS :: patientId={}", patientId);

        return response;

    } catch (FeignException e) {

        log.error("GET_INPROGRESS_BOOKINGS_BY_PATIENT :: FEIGN_ERROR :: patientId={}, clinicId={}",
                patientId, clinicId, e);

        res = new ResponseStructure<>(
                null,
                ExtractFeignMessage.clearMessage(e),
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.status()
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }
}

@Override
public ResponseEntity<?> retrieveAppointnmentsByRelation(String customerId) {

    log.info("RETRIEVE_APPOINTMENTS_BY_RELATION :: START :: customerId={}", customerId);

    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

    try {
        ResponseEntity<?> response =
                bookingFeign.retrieveAppointnmentsByRelation(customerId);

        log.info("RETRIEVE_APPOINTMENTS_BY_RELATION :: SUCCESS :: customerId={}", customerId);

        return response;

    } catch (FeignException e) {

        log.error("RETRIEVE_APPOINTMENTS_BY_RELATION :: FEIGN_ERROR :: customerId={}",
                customerId, e);

        res = new ResponseStructure<>(
                null,
                ExtractFeignMessage.clearMessage(e),
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.status()
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }
}

@Override
public ResponseEntity<?> retrieveAppointnmentsByPatientId(String patientId) {

    log.info("RETRIEVE_APPOINTMENTS_BY_PATIENT :: START :: patientId={}", patientId);

    ResponseStructure<List<BookingResponse>> res = new ResponseStructure<>();

    try {
        ResponseEntity<?> response =
                bookingFeign.getBookingByPatientId(patientId);

        log.info("RETRIEVE_APPOINTMENTS_BY_PATIENT :: SUCCESS :: patientId={}", patientId);

        return response;

    } catch (FeignException e) {

        log.error("RETRIEVE_APPOINTMENTS_BY_PATIENT :: FEIGN_ERROR :: patientId={}",
                patientId, e);

        res = new ResponseStructure<>(
                null,
                ExtractFeignMessage.clearMessage(e),
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.status()
        );

        return ResponseEntity.status(res.getStatusCode()).body(res);
    }
}

@Override
public boolean blockSlot(TempBlockingSlot tempBlockingSlot) {

    log.info("BLOCK_SLOT :: START :: doctorId={}, branchId={}",
            tempBlockingSlot.getDoctorId(),
            tempBlockingSlot.getBranchId());

    try {
        boolean result = clinicAdminFeign.blockSlot(tempBlockingSlot);

        log.info("BLOCK_SLOT :: SUCCESS :: result={}", result);
        return result;

    } catch (FeignException e) {

        log.error("BLOCK_SLOT :: FEIGN_ERROR :: doctorId={}, branchId={}",
                tempBlockingSlot.getDoctorId(),
                tempBlockingSlot.getBranchId(),
                e);

        return false;
    }
}


public CustomerDTO getCustomerByToken(String token) {

    log.info("GET_CUSTOMER_BY_TOKEN :: START");

    try {
        Customer cstmr = customerRepository.findByDeviceId(token);

        if (cstmr != null) {

            log.info("GET_CUSTOMER_BY_TOKEN :: FOUND :: customerId={}",
                    cstmr.getCustomerId());

            CustomerDTO cusmrdto =
                    new ObjectMapper().convertValue(cstmr, CustomerDTO.class);

            return cusmrdto;

        } else {
            log.warn("GET_CUSTOMER_BY_TOKEN :: NOT_FOUND");
            return null;
        }

    } catch (FeignException e) {

        log.error("GET_CUSTOMER_BY_TOKEN :: FEIGN_ERROR", e);
        return null;
    }
}

public ResponseEntity<ResBody<List<NotificationToCustomer>>> notificationToCustomer(
        String customerMobileNumber) {

    log.info("CUSTOMER_NOTIFICATION :: START :: mobileNumber={}", customerMobileNumber);

    try {
        log.info("CUSTOMER_NOTIFICATION :: SUCCESS :: mobileNumber={}", customerMobileNumber);
        return notificationFeign.customerNotification(customerMobileNumber);

    } catch (FeignException e) {
        log.error("CUSTOMER_NOTIFICATION :: FEIGN_ERROR :: mobileNumber={}",
                customerMobileNumber, e);

        ResBody<List<NotificationToCustomer>> res =
                new ResBody<>(ExtractFeignMessage.clearMessage(e), e.status(), null);

        return ResponseEntity.status(e.status()).body(res);
    }
}

}