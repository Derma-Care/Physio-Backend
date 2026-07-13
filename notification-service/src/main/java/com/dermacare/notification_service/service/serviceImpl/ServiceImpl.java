package com.dermacare.notification_service.service.serviceImpl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.dermacare.notification_service.dto.BookingResponse;
import com.dermacare.notification_service.dto.CustomerInfo;
import com.dermacare.notification_service.dto.CustomerOnbordingDTO;
import com.dermacare.notification_service.dto.DoctorSaveDetails;
import com.dermacare.notification_service.dto.ExerciseInfo;
import com.dermacare.notification_service.dto.Medicines;
import com.dermacare.notification_service.dto.NotificationDTO;
import com.dermacare.notification_service.dto.NotificationResponse;
import com.dermacare.notification_service.dto.NotificationToCustomer;
import com.dermacare.notification_service.dto.PriceDropAlertDto;
import com.dermacare.notification_service.dto.ResBody;
import com.dermacare.notification_service.dto.Response;
import com.dermacare.notification_service.dto.ResponseStructure;
import com.dermacare.notification_service.entity.Booking;
import com.dermacare.notification_service.entity.NotificationEntity;
import com.dermacare.notification_service.entity.PriceDropAlertEntity;
import com.dermacare.notification_service.feign.BookServiceFeign;
import com.dermacare.notification_service.feign.CllinicFeign;
import com.dermacare.notification_service.notificationFactory.SendAppNotification;
import com.dermacare.notification_service.repository.NotificationRepository;
import com.dermacare.notification_service.repository.PriceDropAlertNotifications;
import com.dermacare.notification_service.service.ServiceInterface;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import feign.FeignException;


@Service
public class ServiceImpl implements ServiceInterface{

	@Autowired
    private NotificationRepository repository;
	
	@Autowired
	private SendAppNotification appNotification;
	
	@Autowired
	private  BookServiceFeign  bookServiceFeign;	
	
	@Autowired
	private CllinicFeign cllinicFeign;
    
    @Autowired
    private PriceDropAlertNotifications priceDropAlertNotifications;
    
//    @Autowired
//    private FirbaseConfig firbaseConfig;

	//private Set<String> bookings = new LinkedHashSet<>();
	
//	 private BookingResponse bookingResponse;	 
//	 private boolean isCalledAlready;	 
	 private String imag = null;
	 private String customerDeviceId = null;
	 private String Id = null;
	 private String deviceId = null; 
	 private String patient_Id = null;
			 
		@Override
		public ResponseEntity<Response> createNotification(BookingResponse bookingDTO) {
			Response res = new Response();
			try {
			convertToNotification(bookingDTO);	  
			String title=buildTitle(bookingDTO);
			String body =buildBody(bookingDTO);
			///System.out.println(bookingDTO);
			customerDeviceId = cllinicFeign.customerDeviceId(bookingDTO.getCustomerId());
			Id = cllinicFeign.getDeviceId(bookingDTO.getClinicId(),bookingDTO.getBranchId());
			try {
			if(customerDeviceId != null) {
			appNotification.sendPushNotification(customerDeviceId,title,body, "BOOKING",
				    "BookingScreen","default","dashboard");}
			if(Id != null) {
				String content = 
						"AppointmentId:"+bookingDTO.getBookingId()+ "\n\n"
						+ "Doctor:"+bookingDTO.getDoctorName()+ "\n\n"
						+ "Branch: "+ bookingDTO.getBranchname() +"\n\n"
						+ "Date:"+ bookingDTO.getServiceDate() +"\n\n"
						+ "Time:"+ bookingDTO.getServicetime();
						
				appNotification.sendPushNotification(Id,"An appointment has been successfully confirmed for "+bookingDTO.getName()+".\n\n",content, "BOOKING",
					    "BookingScreen","default","dashboard");}	
			
	        res.setMessage("notification sent");
	        res.setStatus(200);
	        res.setSuccess(true);
			}catch(Exception e) {
				res.setMessage(e.getMessage());
		        res.setStatus(404);
		        res.setSuccess(false);	
			}}catch (Exception e) {
				res.setMessage(e.getMessage());
		        res.setStatus(500);
		        res.setSuccess(false);
			}
			//System.out.println(res);
			return ResponseEntity.status(res.getStatus()).body(res);
		}
			
		
  public void sendNotificationToTherapist(Map<String, String> data) {	  
	  try {
		  //System.out.println(data);
		  String deviceId = cllinicFeign.retrivetherapistDeviceId(data.get("therapistId"));
		 // System.out.println(deviceId);
		  if(deviceId != null) {
				String content = 
						String.format(
						        "Hello %s, a new therapy session has been assigned to you for patient %s. The session is scheduled to start on %s. Please review the session details and prepare accordingly.",
						        data.get("therapistName"),
						        data.get("patientname"),
						        data.get("sessionStartDate"));
						
				appNotification.sendPushNotification(deviceId,"New Therapy Session Assigned",content, "Assign",
					    "BookingScreen","default","therapist");}	
	  }catch (Exception e) {
		//System.out.println(e.getMessage());
	}
	  
  }
  
  
  public void sendOverallFeedbackNotificationToTherapist(Map<String, String> data) {	  
	  try {
		  String deviceId = cllinicFeign.retrivetherapistDeviceId(data.get("therapistId"));
			
		  if(deviceId != null) {
			  
				String content = 
						 String.format(
							        "Hello Thrapist, patient %s has submitted feedback for your therapy session.%n" +
							        "Rating: %s/5%n" +
							        "Feedback: \"%s\"",							       
							        data.get("patientName"),
							        data.get("rating"),
							        data.get("feedbackText")
							);
						
				appNotification.sendPushNotification(deviceId,"New Patient Feedback Received",content, "Feedback",
					    "BookingScreen","default","therapist-feedback");}	
	  }catch (Exception e) {
		
	}
	  
  }
	
  public void sendSessionFeedbackNotificationToTherapist(Map<String, String> data) {	  
	  try {
		  String deviceId = cllinicFeign.retrivetherapistDeviceId(data.get("therapistId"));
			
		  if(deviceId != null) {		  
				String content = 
						String.format(
						        "Hello Therapist, you have received new feedback for your therapy session with patient %s.\n\n" +
						        "⭐ Rating: %s/5\n" +
						        "✅ What Went Well: %s\n" +
						        "📝 Improvements Suggested: %s",				       
						        data.get("patientName"),
						        data.get("rating"),
						        data.get("whatWentWell"),
						        data.get("improvements")
						);
				appNotification.sendPushNotification(deviceId,"New Patient session Feedback Received",content, "sessionFeedback",
					    "BookingScreen","default","therapist-feedback");}	
	  }catch (Exception e) {
		
	}
	  
  }
  
  
  public void sendSessionReassignNotificationToTherapist(Map<String, String> data) {	  
	  try {
		 // System.out.println(data);
		  String deviceId = cllinicFeign.retrivetherapistDeviceId(data.get("reassignedTherapistId"));		
		  //System.out.println(deviceId);
		  if(deviceId != null) {		  
				String content = 
						String.format(
						        "%s session has been reassigned to you by Therapist %s. Please review the appointment details.\n\n"
								+ "therapistRecordId: %s",			       
						        data.get("reassignedTherapistname"),
						        data.get("therapistname"),
						        data.get("therapistRecordId")
						);
				appNotification.sendPushNotification(deviceId,"Session Reassignment",content, "Reassignment",
					    "BookingScreen","default","therapist");}	
	  }catch (Exception e) { System.out.println(e.getMessage());
		
	}
	  
  }
  
  public void sendSessionWithdrawNotificationToTherapist(Map<String, String> data) {	  
	  try {
		  //System.out.println(data);
		  String deviceId = cllinicFeign.retrivetherapistDeviceId(data.get("reassignedTherapistId"));		
		  //System.out.println(deviceId);
		  if(deviceId != null) {		  
				String content = 
						String.format(
						        "Therapist %s has withdrawn the reassignment request for the session.\n\n"				       
						        + "therapistRecordId %s",
						        data.get("reassignedTherapistname"),
						        data.get("therapistRecordId")	
						);
				appNotification.sendPushNotification(deviceId,"Assignment Withdrawn",content, "Withdrawn",
					    "BookingScreen","default","therapist");}	
	  }catch (Exception e) { System.out.println(e.getMessage());
		
	}
	  
  }
  
			
	private void convertToNotification(BookingResponse booking) {	
			NotificationEntity notificationEntity = new NotificationEntity();
//			notificationEntity.setMessage("New Service Appointment Request For: " + booking.getSubServiceName());
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			String currentDate = LocalDate.now().format(dateFormatter);	
			notificationEntity.setDate(currentDate);
			ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
		    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
		    String formattedTime = istTime.format(formatter);
		    notificationEntity.setTime(formattedTime);
		    notificationEntity.setData(new ObjectMapper().convertValue(booking,Booking.class));
			notificationEntity.setActions(new String[]{"Accept", "Reject"});
			repository.save(notificationEntity);}
	
	
	private String buildTitle(BookingResponse booking) {
	    return "Appointment Confirmed";
	}
	
	
	private String buildBody(BookingResponse booking) {
	    return "Dear " + booking.getName() + ",\n\n" +
	           "Your appointment has been successfully booked.\n\n" +
	           "Doctor: " + booking.getDoctorName() + "\n" +
	           "Branch: " + booking.getBranchname() + "\n" +
	           "Date: " + booking.getServiceDate() + "\n" +
	           "Time: " + booking.getServicetime() + "\n\n" +
	           "We look forward to seeing you.";
	}
	
	
	
	public ResBody<List<NotificationDTO>> notificationtodoctor( String hospitalId,
			 String doctorId){
		ResBody<List<NotificationDTO>> res = new ResBody<List<NotificationDTO>>();
		List<NotificationDTO> eligibleNotifications = new ArrayList<>();
		List<NotificationDTO> reversedEligibleNotifications = new ArrayList<>();
		try {
		List<NotificationEntity> entity = repository.findByDataClinicIdAndDataDoctorId(hospitalId, doctorId);
		List<NotificationDTO> dto = new ObjectMapper().convertValue(entity, new TypeReference<List<NotificationDTO>>() {});
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String currentDate = LocalDate.now().format(dateFormatter);
		if(dto != null) {
		for(NotificationDTO n : dto) {	
			if(n.getData().getStatus().equalsIgnoreCase("Confirmed") && n.getDate().equals(currentDate)){
				eligibleNotifications.add(n);}}}
		for(int i=eligibleNotifications.size()-1;i>=0;i--) {
			reversedEligibleNotifications.add(eligibleNotifications.get(i));
		}
		if(eligibleNotifications!=null && !eligibleNotifications.isEmpty() ) {
		res = new ResBody<List<NotificationDTO>>("Notification sent Successfully",200,reversedEligibleNotifications);	
		
		}else {
			res = new ResBody<List<NotificationDTO>>("NotificationInfo Not Found",200,null);
			}}catch(Exception e) {
		res = new ResBody<List<NotificationDTO>>(e.getMessage(),500,null);
	}
		return res;
	}
				

	
	public ResBody<List<NotificationDTO>> sendNotificationToClinic(String clinicId) {
		ResBody<List<NotificationDTO>> r = new ResBody<List<NotificationDTO>>();
		List<NotificationDTO> list = new ArrayList<>();
		List<NotificationDTO> reversedList = new ArrayList<>();
		try {
			List<NotificationEntity> entity = repository.findAll();
			List<NotificationDTO> dto = new ObjectMapper().convertValue(entity, new TypeReference<List<NotificationDTO>>() {});	
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			String currentDate = LocalDate.now().format(dateFormatter);	
			if(dto != null) {
			for(NotificationDTO n : dto) {												
				if(n.getData().getStatus().equalsIgnoreCase("Pending") && 
				n.getData().getClinicId().equals(clinicId) && n.getDate().equals(currentDate)){					
					list.add(n);}}}
			for(int i = list.size()-1; i>=0; i-- ) {
				reversedList.add(list.get(i));
			}
		    if( list != null && ! list.isEmpty()) {
		    	r = new ResBody<List<NotificationDTO>>("Notifications Are sent to the admin",200,reversedList);
		    }else {
		    r = new ResBody<List<NotificationDTO>>("Notifications Are Not Found",200,null); }  
		}catch(Exception e) {
			r = new ResBody<List<NotificationDTO>>(e.getMessage(),500,null);
		}
		return r;	
	}
	
	
	
//	 private boolean timeDifference(String notificationTime) {			
//		   try {
//			 
//			   SimpleDateFormat inputFormat = new SimpleDateFormat("hh:mm a"); 
//			   
//			   ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
//		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
//		        String formattedTimeByZone = istTime.format(formatter);			   
//		        Date formattedCurrentTime = inputFormat.parse(formattedTimeByZone);		        
//		       SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
//		       String modifiedcurrentTime = simpleDateFormat.format(formattedCurrentTime);
//		       		      
//		      Date date = inputFormat.parse(notificationTime);		      
//		      SimpleDateFormat simpleDateFormatForNotificationTime = new SimpleDateFormat("HH:mm");
//		      String modifiednotificationTime = simpleDateFormatForNotificationTime.format(date);
//		      
//		       Date nTime = simpleDateFormat.parse(modifiednotificationTime);
//		       Date cTime = simpleDateFormat.parse(modifiedcurrentTime);
//		       
//		       System.out.println(nTime);
//		       System.out.println(cTime);
//		       
//		       long differenceInMilliSeconds
//		           = cTime.getTime() - nTime.getTime();     
//		           		      
//		       long differenceInMinutes
//		           = differenceInMilliSeconds / (60 * 1000);///it wont ignores hours 
//		       
//		       System.out.println(differenceInMinutes);
//		       if(differenceInMinutes != 0 && differenceInMinutes >= 5 ) {
//		    	   return true;
//		    	 }else{
//		    	    return false;}
//		   }catch(ParseException e) {
//			   return false;}
//		   }
	
	 public ResBody<NotificationDTO> notificationResponse(NotificationResponse notificationResponse) {
		    try {		        
		        ResponseEntity<ResponseStructure<BookingResponse>> res = bookServiceFeign.getBookedService(notificationResponse.getAppointmentId());
		        BookingResponse b = res.getBody().getData();		     
		        NotificationEntity notificationEntity = repository.findByNotificationId(notificationResponse.getNotificationId());		      
		        if (b == null) {
		            return new ResBody<>("Booking not found for given appointment ID", 404, null);
		        }
		        if (notificationEntity == null) {
		            return new ResBody<>("Notification not found for given notification ID", 404, null);
		        }		       
		        if (b.getDoctorId().equalsIgnoreCase(notificationResponse.getDoctorId()) &&
		            b.getClinicId().equalsIgnoreCase(notificationResponse.getHospitalId()) &&
		            b.getBookingId().equalsIgnoreCase(notificationResponse.getAppointmentId()) &&
		            b.getSubServiceId().equalsIgnoreCase(notificationResponse.getSubServiceId())) {		          
		            String status = notificationResponse.getStatus();
		            switch (status) {
		                case "Accepted":
		                    b.setStatus("Confirmed");
		                    notificationEntity.getData().setStatus("Confirmed");
		                    repository.save(notificationEntity);
		                    try {
		                        if (b.getCustomerDeviceId() != null) {
		                            appNotification.sendPushNotification(
		                                b.getCustomerDeviceId(),
		                                " Hello " + b.getName(),
		                                b.getDoctorName() + " Accepted Your Appointment For " +
		                                b.getSubServiceName() + " on " + b.getServiceDate() + " at " + b.getServicetime(),
		                                "BOOKING SUCCESS",
		                			    "BookingVerificationScreen","default","dashboard" );}
		                        
		                        if (b.getDoctorDeviceId() != null) {
		                            appNotification.sendPushNotification(
		                                b.getCustomerDeviceId(),
		                                " Hello " + b.getDoctorName()," You Have A New "+b.getConsultationType() +" Appointment For " +
		                                b.getSubServiceName() + " on " + b.getServiceDate() + " at " + b.getServicetime(),
		                                "BOOKING SUCCESS",
		                			    "BookingVerificationScreen","default","dashboard" );}
		                        
		                        if(b.getDoctorWebDeviceId() != null) {
		                            appNotification.sendPushNotification(
		                                b.getCustomerDeviceId(),
		                                " Hello " + b.getDoctorName()," You Have A New "+b.getConsultationType() +" Appointment For " +
		                                b.getSubServiceName() + " on " + b.getServiceDate() + " at " + b.getServicetime(),
		                                "BOOKING SUCCESS",
		                			    "BookingVerificationScreen","default","dashboard" );}
		                    } catch (Exception ex) {}
		                      break;

		                case "Rejected":
		                    b.setStatus("Rejected");
		                    b.setReasonForCancel(notificationResponse.getReasonForCancel());
		                    cllinicFeign.makingFalseDoctorSlot(b.getDoctorId(),b.getBranchId() ,b.getServiceDate(), b.getServicetime());
		                    notificationEntity.getData().setStatus("Rejected");
		                    repository.save(notificationEntity);
		                    try {
		                        if (b.getCustomerDeviceId() != null) {
		                            appNotification.sendPushNotification(
		                                b.getCustomerDeviceId(),
		                                " Hello " + b.getName(),
		                                b.getDoctorName() + " Rejected Your Appointment For " +
		                                b.getSubServiceName() + " on " + b.getServiceDate() + " at " + b.getServicetime(),
		                                "BOOKING REJECT",
		                			    "BookingVerificationScreen","default","dashboard"
		                            );
		                        }
		                    } catch (Exception ex) {}		                
		                    break;
		                default:		                  
		                    b.setStatus("Pending");
		                    notificationEntity.getData().setStatus("Pending");
		                    repository.save(notificationEntity);
		                    break;}		           
		            ResponseEntity<?> book = bookServiceFeign.updateAppointment(b);
		            if (book != null) {
		                return new ResBody<>("Appointment And Notification Status updated", 200, null);		                
		            } else {
		                return new ResBody<>("Appointment Status Not updated", 200, null);
		            }

		        } else {
		            return new ResBody<>("Status Not updated, please check provided details", 200, null);
		        }

		    } catch (FeignException e) {
		        return new ResBody<>(e.getMessage(), 500, null);
		    }
		}

		

	
	
	
//	private void removeCompletedNotifications() {
//   	List<NotificationEntity> entity = repository.findAll();
//   	if(entity!=null && !entity.isEmpty()) {
//   		for(NotificationEntity e : entity) {
//   			if(e.getData().getStatus().equals("Completed")) {
//   				if(bookings.contains(e.getId())) {
//   					bookings.remove(e.getId());}
//   			repository.delete(e);	    			
//   		}}}}

	
//	 @Scheduled(fixedRate = 1 * 60 * 1000)
//	 public void sendAlertNotifications() {		 
//		 try {
//			 //System.out.println("sendAlertNotifications method invoked");
//			 List<NotificationEntity> notifications = repository.findAll();
//			 DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//				String currentDate = LocalDate.now().format(dateFormatter);
//		        for (NotificationEntity notification : notifications) {
//		            if ("Confirmed".equalsIgnoreCase(notification.getData().getStatus()) && notification.isAlerted() == false 
//		             && notification.getData().getServiceDate().equals(currentDate) && calculateTimeDifferenceForAlertNotification(notification.getData().getServicetime())) {
//		            if(notification.getData().getConsultationType().equalsIgnoreCase("online consultation") ||
//		            notification.getData().getConsultationType().equalsIgnoreCase("video consultation")) {
//		            sendAlertPushNotification(notification.getData().getBookingId());
//		            notification.setAlerted(true);
//		            repository.save(notification);}}}
//		 }catch(Exception e) {}
// }
	
	 
	 
	 private boolean calculateTimeDifferenceForAlertNotification(String serviceTime) {	
		 
		   try {		   
             SimpleDateFormat inputFormat = new SimpleDateFormat("hh:mm a"); ////used for convert string to date object
			   
			   ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
		        String formattedTimeByZone = istTime.format(formatter);	////current asia time generated.present in form of string		   
		        Date formattedCurrentTime = inputFormat.parse(formattedTimeByZone);	////converting String to date object	        
		       SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");////converting form 12 hrs to 24 hrs
		       String modifiedcurrentTime = simpleDateFormat.format(formattedCurrentTime);
		       		      
		      Date serviceTimeStringToDteObject = inputFormat.parse(serviceTime);		      
		      SimpleDateFormat simpleDateFormatForNotificationTime = new SimpleDateFormat("HH:mm");
		      String modifiedServiceTime = simpleDateFormatForNotificationTime.format(serviceTimeStringToDteObject);
		      
		       Date sTime = simpleDateFormat.parse(modifiedServiceTime );
		       Date cTime = simpleDateFormat.parse(modifiedcurrentTime);
//		       
//		       System.out.println(sTime);
//		       System.out.println(cTime);
		       long differenceInMilliSeconds
		           =  sTime.getTime() - cTime.getTime();     
		       		      
		       long differenceInMinutes
		           = differenceInMilliSeconds / (60 * 1000);///it wont ignores hours convert then into minutes
		       System.out.println(differenceInMinutes);
		       
		       if(differenceInMinutes != 0 && differenceInMinutes >= 1 &&  differenceInMinutes <= 5  ) {
		    	   return true;
		    	 }else{
		    	    return false;}
		   }catch(ParseException e) {
			   return false;}
		   }
	
		 
	 private void sendAlertPushNotification(String appointmentId) {
		 try {
			 ResponseEntity<ResponseStructure<BookingResponse>> res = bookServiceFeign.getBookedService(appointmentId);
		        BookingResponse b = res.getBody().getData();
		        //System.out.println(b.getCustomerDeviceId());
		       // System.out.println(b.getDoctorDeviceId());
		        if (b != null) {
		        	 try {
	                        if(customerDeviceId != null && b.getDoctorDeviceId() != null) {
	                            appNotification.sendPushNotification(
	                            		customerDeviceId,
	                                " Hello " + b.getName()+ "," ,
	                                b.getDoctorName() + " Connect With You Through Video Call within 5 Minutes ", "Alert",
	                			    "AlertScreen","default","dashboard");
	                            
	                            appNotification.sendPushNotification(
	                                b.getDoctorDeviceId(),
	                                " Hello " +b.getDoctorName()+ "," , " You Have a Video Consultation within 5 Minutes With " +
	                                b.getName(), "Alert",
	                			    "AlertScreen","default","dashboard");}
	                            
	                            if(b.getDoctorWebDeviceId() != null) {
	                            	 appNotification.sendPushNotification(
	     	                                b.getDoctorWebDeviceId(),
	     	                                " Hello " +b.getDoctorName()+ "," , " You Have a Video Consultation within 5 Minutes With " +
	     	                                b.getName(), "Alert",
	     	                			    "AlertScreen","default","dashboard");}
	                            
	                            if(b.getClinicDeviceId() != null) {
	                            	 appNotification.sendPushNotification(
	     	                                b.getClinicDeviceId(),
	     	                                " Hello ClinicAdmin", b.getDoctorName()+ " Have a Video Consultation within 5 Minutes With " +
	     	                                b.getName(), "Alert",
	     	                			    "AlertScreen","default","dashboard");}
	                            
	                            //System.out.println("Notification sent to doctor and customer");
	                    }catch (Exception ex) {}}
			 }catch(Exception e) {}
		  }
	 
	 
	
	@Override
	public NotificationDTO getNotificationByBookingId(String bookingId) {
		NotificationEntity notification = repository.findByDataBookingId(bookingId);
		 NotificationDTO dto = new ObjectMapper().convertValue(notification, NotificationDTO.class );
		 return dto;
	}
	

	@Override
	public NotificationDTO updateNotification( NotificationDTO  notificationDTO) {
		 NotificationEntity entity = new ObjectMapper().convertValue(notificationDTO, NotificationEntity.class );
		NotificationEntity notification = repository.save(entity);
		 NotificationDTO dto = new ObjectMapper().convertValue(notification, NotificationDTO.class );
		 return dto;
	}
	
	
	@Override
	public ResponseEntity<ResBody<List<NotificationToCustomer>>> notificationToCustomer(
			 String customerMobileNumber){
		ResBody<List<NotificationToCustomer>> res = new ResBody<List<NotificationToCustomer>>();
		List<NotificationToCustomer> eligibleNotifications = new ArrayList<>();
		try {
		List<NotificationEntity> entity = repository.findByDataMobileNumber(customerMobileNumber );
		List<NotificationDTO> dto = new ObjectMapper().convertValue(entity, new TypeReference<List<NotificationDTO>>() {});
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String currentDate = LocalDate.now().format(dateFormatter);	
		if(dto != null) {
		for(NotificationDTO n : dto) {					
			if(n.getData().getStatus().equalsIgnoreCase("Confirmed") && n.getDate().equals(currentDate)) {
				NotificationToCustomer notification = new NotificationToCustomer();
				notification.setMessage("Appointment Accepted");
				notification.setHospitalName(n.getData().getClinicName());
				notification.setDoctorName(n.getData().getDoctorName());
				//notification.setServiceName(n.getData().getSubServiceName());
				notification.setServiceDate(n.getData().getServiceDate());
				notification.setServiceTime(n.getData().getServicetime());
				notification.setServiceFee(n.getData().getTotalFee());
				notification.setConsultationType(n.getData().getConsultationType());
				notification.setConsultationFee(n.getData().getConsultationFee());
				eligibleNotifications.add(notification);}}}
		if(eligibleNotifications!=null && !eligibleNotifications.isEmpty() ) {
		res = new ResBody<List<NotificationToCustomer>>("Notification sent Successfully",200,eligibleNotifications);
		}else {
			res = new ResBody<List<NotificationToCustomer>>("Notifications Not Found",200,null);
			}}catch(FeignException e) {
		res = new ResBody<List<NotificationToCustomer>>(e.getMessage(),500,null);
	}
		return ResponseEntity.status(res.getStatus()).body(res);
		}	
	

//	@Scheduled(cron = "0 30 8 * * ?")
//	public void remindMorningMedicines() {
//		 try {
//		        // Fetch doctor details
//		        Response obj = doctorFeign.getAllDoctorSaveDetails().getBody();     
//		        ObjectMapper mapper = new ObjectMapper();
//		        mapper.registerModule(new JavaTimeModule());
//		        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//		        List<DoctorSaveDetails> doctorSaveDetailsDTOs =
//		        		mapper.convertValue(obj.getData(), new TypeReference<List<DoctorSaveDetails>>() {});
//
//		       // System.out.println("Fetched doctors: " + doctorSaveDetailsDTOs.size());
//
//		        for (DoctorSaveDetails doctorSaveDetailsDTO : doctorSaveDetailsDTOs) {
//
//		            LocalDateTime visitedDate = doctorSaveDetailsDTO.getVisitDateTime();
//
//		            // Fetch booking details
//		            // Iterate over prescribed medicines
//		            for (Medicines m : doctorSaveDetailsDTO.getPrescription().getMedicines()) {
//		                long duration = convertDurationToDays(m.getDuration(),m.getDurationUnit()); // already long?
//		                LocalDateTime plusDays = visitedDate.plusDays(duration);
//	                   // System.out.println(plusDays);
//		                LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
//	                    ///System.out.println(now);
//		                // Check if today is within duration
//		                if (!now.isBefore(visitedDate) && !now.isAfter(plusDays)) {
//		                    boolean isMorning = Arrays.stream(m.getRemindWhen().split(" "))
//		                            .anyMatch(time -> time.equalsIgnoreCase("Morning"));
//		                    	 if(isMorning){
//		     	                    if(bookingResponse == null) {
//		     	                    	isCalledAlready = true;
//		     	                    }else{
//		     	                    if(!bookingResponse.getBookingId().equalsIgnoreCase(doctorSaveDetailsDTO.getBookingId())) {
//		     	                    	isCalledAlready = true;
//		     	                    }else {
//		     	                    	isCalledAlready = false;	
//		     	                    }}
//		     	                    if(isCalledAlready){
//		                    try{
//		    		            ResponseEntity<ResponseStructure<BookingResponse>> res =
//		    		                    bookServiceFeign.getBookedService(doctorSaveDetailsDTO.getBookingId());
//		    		            bookingResponse = res.getBody().getData();
//
//		    		            if (bookingResponse == null) {
//		    		                //System.out.println("No booking found for ID: " + doctorSaveDetailsDTO.getBookingId());
//		    		                continue;
//		    		            }
//
//		    		            //System.out.println("Booking: " + bookingResponse);
//		    		            }catch(Exception e) {
//		    		            	 System.out.println(e.getMessage());
//		    		            }}
//		                    // System.out.println(isAfternoon);
//		                    if (bookingResponse != null && bookingResponse.getCustomerDeviceId() != null) {
//		appNotification.sendPushNotification(
//				bookingResponse.getCustomerDeviceId(),
//	            "🌞 Good morning!",
//	           "Time to take your prescribed "+m.getName()+","+m.getDose()+" with water.",
//	            "MEDICINE REMINDER",
//			    "reminderScreen","default","dashboard"
//	        );	
//		    }}}}}}catch (Exception e) {e.printStackTrace();}}
//
//
//
//
//	@Scheduled(cron = "0 30 13 * * ?")
//	public void remindAfternoonMedicines() {
//	   // System.out.println("invoke");
//	    try {
//	        // Fetch doctor details
//	        Response obj = doctorFeign.getAllDoctorSaveDetails().getBody();     
//	        ObjectMapper mapper = new ObjectMapper();
//	        mapper.registerModule(new JavaTimeModule());
//	        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//	        List<DoctorSaveDetails> doctorSaveDetailsDTOs =
//	        		mapper.convertValue(obj.getData(), new TypeReference<List<DoctorSaveDetails>>() {});
//
//	      // System.out.println("Fetched doctors: " + doctorSaveDetailsDTOs.size());
//
//	        for (DoctorSaveDetails doctorSaveDetailsDTO : doctorSaveDetailsDTOs) {
//
//	            LocalDateTime visitedDate = doctorSaveDetailsDTO.getVisitDateTime();
//
//	            // Fetch booking details
//	            // Iterate over prescribed medicines
//	            for (Medicines m : doctorSaveDetailsDTO.getPrescription().getMedicines()) {
//	                long duration = convertDurationToDays(m.getDuration(),m.getDurationUnit()); // already long?
//	                LocalDateTime plusDays = visitedDate.plusDays(duration);
//                   // System.out.println(plusDays);
//	                LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
//                   // System.out.println(now);
//	                // Check if today is within duration
//	                if (!now.isBefore(visitedDate) && !now.isAfter(plusDays)) {
//	                    boolean isAfternoon = Arrays.stream(m.getRemindWhen().split(" "))
//	                            .anyMatch(time -> time.equalsIgnoreCase("Afternoon"));
//	                    if(isAfternoon){
//	                    if(bookingResponse == null) {
//	                    	isCalledAlready = true;
//	                    }else{
//	                    if(!bookingResponse.getBookingId().equalsIgnoreCase(doctorSaveDetailsDTO.getBookingId())) {
//	                    	isCalledAlready = true;
//	                    }else {
//	                    	isCalledAlready = false;	
//	                    }}
//	                    if(isCalledAlready){
//	                    try{
//	        	            ResponseEntity<ResponseStructure<BookingResponse>> res =
//	        	                    bookServiceFeign.getBookedService(doctorSaveDetailsDTO.getBookingId());
//	        	            bookingResponse = res.getBody().getData();
//
//	        	            if (bookingResponse == null) {
//	        	               // System.out.println("No booking found for ID: " + doctorSaveDetailsDTO.getBookingId());
//	        	                continue;
//	        	            }
//
//	        	          //  System.out.println("Booking: " + bookingResponse);
//	        	            }catch(Exception e) {
//	        	            	 System.out.println(e.getMessage());
//	        	            }}
//	                    // System.out.println(isAfternoon);
//	                    if (bookingResponse != null && bookingResponse.getCustomerDeviceId() != null) {
//	                    //System.out.println(bookingResponse.getCustomerDeviceId());	
//	                  //  System.out.println("not invoke");
//	                        appNotification.sendPushNotification(
//	                                bookingResponse.getCustomerDeviceId(),
//	                                "🌤️ Good afternoon!",
//	                                "Time to take your prescribed " + m.getName() + ", " + m.getDose() + " with water.",
//	                                "MEDICINE REMINDER",
//	                                "reminderScreen",
//	                                "default","dashboard"
//	                        );
//	                       // System.out.println("Notification sent for " + m.getName());
//	                    } 
//	                }
//	            }
//	        }
//	        }} catch (Exception e) {
//	        e.printStackTrace(); // log properly instead of hiding errors
//	    }
//	}
//
//
//	@Scheduled(cron = "0 01 17 * * ?")
//	public void remindEveningMedicines() {
//		     try {
//		        // Fetch doctor details
//		        Response obj = doctorFeign.getAllDoctorSaveDetails().getBody();     
//		        ObjectMapper mapper = new ObjectMapper();
//		        mapper.registerModule(new JavaTimeModule());
//		        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//		        List<DoctorSaveDetails> doctorSaveDetailsDTOs =
//		        		mapper.convertValue(obj.getData(), new TypeReference<List<DoctorSaveDetails>>() {});
//
//		       // System.out.println("Fetched doctors: " + doctorSaveDetailsDTOs.size());
//
//		        for (DoctorSaveDetails doctorSaveDetailsDTO : doctorSaveDetailsDTOs) {
//
//		            LocalDateTime visitedDate = doctorSaveDetailsDTO.getVisitDateTime();
//
//		            // Fetch booking details
//		            // Iterate over prescribed medicines
//		            for (Medicines m : doctorSaveDetailsDTO.getPrescription().getMedicines()) {
//		                long duration = convertDurationToDays(m.getDuration(),m.getDurationUnit()); // already long?
//		               // System.out.println(duration);
//		                LocalDateTime plusDays = visitedDate.plusDays(duration);
//	                   // System.out.println(plusDays);
//		                LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
//	                    //System.out.println(now);
//	                    //System.out.println(visitedDate);
//		                // Check if today is within duration
//		                if (!now.isBefore(visitedDate) && !now.isAfter(plusDays)) {
//		                	//System.out.println("invoked for times");
//		                    boolean isEvening = Arrays.stream(m.getRemindWhen().split(" "))
//		                            .anyMatch(time -> time.equalsIgnoreCase("Evening"));
//		                    if(isEvening){
//			                    if(bookingResponse == null) {
//			                    	isCalledAlready = true;
//			                    }else{
//			                    if(!bookingResponse.getBookingId().equalsIgnoreCase(doctorSaveDetailsDTO.getBookingId())) {
//			                    	isCalledAlready = true;
//			                    }else {
//			                    	isCalledAlready = false;	
//			                    }}
//			                    if(isCalledAlready){
//		                    try{
//		    		            ResponseEntity<ResponseStructure<BookingResponse>> res =
//		    		                    bookServiceFeign.getBookedService(doctorSaveDetailsDTO.getBookingId());
//		    		            bookingResponse = res.getBody().getData();
//
//		    		            if (bookingResponse == null) {
//		    		                //System.out.println("No booking found for ID: " + doctorSaveDetailsDTO.getBookingId());
//		    		                continue;
//		    		            }
//
//		    		            //System.out.println("Booking: " + bookingResponse);
//		    		            }catch(Exception e) {
//		    		            	 System.out.println(e.getMessage());
//		    		            }}
//		                    //System.out.println(isAfternoon);
//		                    if (bookingResponse != null && bookingResponse.getCustomerDeviceId() != null) {
//		                   // System.out.println(bookingResponse.getCustomerDeviceId());	
//		                   // System.out.println("not invoke");
//			 
//			 	        appNotification.sendPushNotification(
//			 			bookingResponse.getCustomerDeviceId(),
//			             "🌆 Good evening!",
//			            "Time to take your prescribed "+m.getName()+","+m.getDose()+" with water.",
//			             "MEDICINE REMINDER",
//			 		    "reminderScreen","default","dashboard"
//			         );	
//		             }}}}}}catch (Exception e) {e.printStackTrace();}}
//
//
//	@Scheduled(cron = "0 30 20 * * ?")
//	public void remindNightMedicines() {
//		      try {
//		        // Fetch doctor details
//		        Response obj = doctorFeign.getAllDoctorSaveDetails().getBody();     
//		        ObjectMapper mapper = new ObjectMapper();
//		        mapper.registerModule(new JavaTimeModule());
//		        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//		        List<DoctorSaveDetails> doctorSaveDetailsDTOs =
//		        		mapper.convertValue(obj.getData(), new TypeReference<List<DoctorSaveDetails>>() {});
//
//		       //System.out.println("Fetched doctors: " + doctorSaveDetailsDTOs.size());
//
//		        for (DoctorSaveDetails doctorSaveDetailsDTO : doctorSaveDetailsDTOs) {
//
//		            LocalDateTime visitedDate = doctorSaveDetailsDTO.getVisitDateTime();
//
//		            // Fetch booking details
//		            // Iterate over prescribed medicines
//		            for (Medicines m : doctorSaveDetailsDTO.getPrescription().getMedicines()) {
//		                long duration = convertDurationToDays(m.getDuration(),m.getDurationUnit()); // already long?
//		                LocalDateTime plusDays = visitedDate.plusDays(duration);
//	                   //System.out.println(plusDays);
//		                LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
//	                    //System.out.println(now);
//		                // Check if today is within duration
//		                if (!now.isBefore(visitedDate) && !now.isAfter(plusDays)) {
//		                    boolean isNight = Arrays.stream(m.getRemindWhen().split(" "))
//		                            .anyMatch(time -> time.equalsIgnoreCase("Night"));
//		                    if(isNight){
//			                    if(bookingResponse == null) {
//			                    	isCalledAlready = true;
//			                    }else{
//			                    if(!bookingResponse.getBookingId().equalsIgnoreCase(doctorSaveDetailsDTO.getBookingId())) {
//			                    	isCalledAlready = true;
//			                    }else {
//			                    	isCalledAlready = false;	
//			                    }}
//			                    if(isCalledAlready){
//		                        try{
//		    		            ResponseEntity<ResponseStructure<BookingResponse>> res =
//		    		                    bookServiceFeign.getBookedService(doctorSaveDetailsDTO.getBookingId());
//		    		            bookingResponse = res.getBody().getData();
//
//		    		            if (bookingResponse == null) {
//		    		               // System.out.println("No booking found for ID: " + doctorSaveDetailsDTO.getBookingId());
//		    		                continue;
//		    		            }
//
//		    		            //System.out.println("Booking: " + bookingResponse);
//		    		            }catch(Exception e) {
//		    		            	 System.out.println(e.getMessage());
//		    		            }}
//		                   //System.out.println(isNight);
//			                
//		                    if(bookingResponse != null && bookingResponse.getCustomerDeviceId() != null) {
//		                  // System.out.println(bookingResponse.getCustomerDeviceId());	
//		                   // System.out.println("not invoke");
//			 	        appNotification.sendPushNotification(
//			 			bookingResponse.getCustomerDeviceId(),
//			             "🌃 Good evening!",
//			            "Time to take your prescribed "+m.getName()+","+m.getDose()+" with water.",
//			             "MEDICINE REMINDER",
//			 		    "reminderScreen","default","dashboard"
//			         );	
//		             }}}}}}catch (Exception e) {e.printStackTrace();}}
//
//	
//	@Scheduled(cron = "0 01 9 * * ?")
//	public void remindFollowUpsBeforeTwoDays() {
//		      try {
//		        // Fetch doctor details
//		        Response obj = doctorFeign.getAllDoctorSaveDetails().getBody();     
//		        ObjectMapper mapper = new ObjectMapper();
//		        mapper.registerModule(new JavaTimeModule());
//		        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//		        List<DoctorSaveDetails> doctorSaveDetailsDTOs =
//		        		mapper.convertValue(obj.getData(), new TypeReference<List<DoctorSaveDetails>>() {});
//		       //System.out.println("Fetched doctors: " + doctorSaveDetailsDTOs.size());
//		        for (DoctorSaveDetails doctorSaveDetailsDTO : doctorSaveDetailsDTOs) {
//                      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//		              LocalDateTime visitedDate = doctorSaveDetailsDTO.getVisitDateTime();
//		              String dte = formatter.format(visitedDate);
//		             // System.out.println(dte);
//		              LocalDate formattedVisitedDate = LocalDate.parse(dte);
//		                int follup = convertDurationToDays(String.valueOf(doctorSaveDetailsDTO.getFollowUp()
//		                .getDurationValue()),doctorSaveDetailsDTO.getFollowUp().getDurationUnit());
//		                LocalDate plusDays = formattedVisitedDate.plusDays(follup);
//	                    //System.out.println(plusDays);
//		                LocalDate now = LocalDate.now();
//	                    //System.out.println(now);
//		                // Check if today is within duration
//		                LocalDate minusDay = plusDays.minusDays(2);
//		               // System.out.println(minusDay);
//		                if (minusDay.equals(now)) {
//		                        try{
//		    		            ResponseEntity<ResponseStructure<BookingResponse>> res =
//		    		                    bookServiceFeign.getBookedService(doctorSaveDetailsDTO.getBookingId());
//		    		            bookingResponse = res.getBody().getData();
//		    		            if (bookingResponse == null) {
//		    		                System.out.println("No booking found for ID: " + doctorSaveDetailsDTO.getBookingId());
//		    		                continue;
//		    		            }
//		    		           // System.out.println("Booking: " + bookingResponse);
//		    		            }catch(Exception e) {
//		    		            	 System.out.println(e.getMessage());
//		    		            }}			                
//		                    if(bookingResponse != null && bookingResponse.getCustomerDeviceId() != null &&
//		                    !bookingResponse.getStatus().equalsIgnoreCase("Completed")) {
//		                   // System.out.println(bookingResponse.getCustomerDeviceId());	
//		                   /// System.out.println("not invoke");
//			 	        appNotification.sendPushNotification(
//			 			bookingResponse.getCustomerDeviceId(),
//			             "🌞 Good morning!",
//			             "Reminder: You have a follow-up appointment on " + doctorSaveDetailsDTO.getFollowUp().getNextFollowUpDate() +
//			             " with Dr." + doctorSaveDetailsDTO.getDoctorName() + "\n" +
//			             "📍 "+doctorSaveDetailsDTO.getClinicName()+" \n" +
//			             "Please be present, and contact us if you need to reschedule.",
//			             "FollowUp REMINDER",
//			 		    "reminderScreen","default","dashboard"
//			         );	
//		             }}}catch (Exception e) {e.printStackTrace();}}
//
//	
	 private int convertDurationToDays(String duration,String durationUnit) {
	        if (duration == null || duration.isEmpty()) {
	            throw new IllegalArgumentException("Duration cannot be null or empty");
	        }

	        duration = duration.toLowerCase().trim();

	        // Split into number and unit
	        String[] parts = duration.split(" ");
	        if (parts.length < 1) {
	            throw new IllegalArgumentException("Invalid duration format: " + duration);
	        }

	        int number = Integer.parseInt(parts[0]); 
	       //// String unit = parts[1];

	        switch (durationUnit) {
	            case "day":
	            case "Day":
	            case "Days":
	            case "days":
	                return number;
	            case "week":
	            case "Week":
	            case "Weeks":
	            case "weeks":
	                return number * 7;
	            case "month":
	            case "Month":
	            case "Months":
	            case "months":
	                return number * 30; // Approximation
	            default:
	                throw new IllegalArgumentException("Unsupported duration unit: " + durationUnit);
	        }
	    }
		
	 
	 public ResponseEntity<?> sendImageNotifications(PriceDropAlertDto priceDropAlertDto){
		 Response res = new Response();
		 try {
			 if(priceDropAlertDto.getImage() != null) {
				 imag = priceDropAlertDto.getImage();
				// firbaseConfig.uploadBase64Image(imag);
			 }else {
				 imag = "";
			 }			 
			 if(priceDropAlertDto.getSendAll()) {
			 List<CustomerOnbordingDTO> cusmr =  new ObjectMapper().convertValue(cllinicFeign.getAllCustomers().getBody().getData(), new TypeReference< List<CustomerOnbordingDTO>>() {});			 
			// System.out.println(cusmr);
			 cusmr.stream().map(n->{  if(n.getDeviceId() != null) {
			 appNotification.sendPushNotificationForImage(n.getDeviceId(),priceDropAlertDto.getTitle(),priceDropAlertDto.getBody(), "Notification",
					    "NotificationScreen","default",imag);
			// System.out.println("notification sent successfully");
			 }
			 return n;}).toList();
			 }else {
				 priceDropAlertDto.getTokens().stream().map(t->{appNotification.sendPushNotificationForImage(t,priceDropAlertDto.getTitle(),priceDropAlertDto.getBody(), "Notification",
						    "NotificationScreen","default",imag);
				 return t;}).toList();}
			 PriceDropAlertEntity en = new ObjectMapper().convertValue(priceDropAlertDto, PriceDropAlertEntity.class);
			 en.setLocalDateTime(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
			 priceDropAlertNotifications.save(en); 
			 res.setStatus(200);
             res.setMessage("successfully sent notification");
             res.setSuccess(true);
		 }catch(Exception e) {
			 res.setStatus(500);
             res.setMessage(e.getMessage());
             res.setSuccess(false);
		 }
		 return ResponseEntity.status(res.getStatus()).body(res);
	 }
	 
	 
	 public ResponseEntity<?> priceDropNotifications(String clinicId,String branchId){
		 Response res = new Response();
		 try {
			List<PriceDropAlertEntity> enty = priceDropAlertNotifications.findByClinicIdAndBranchId(clinicId, branchId);
			//System.out.println(enty);
			List<PriceDropAlertDto> dto = null;
			List<PriceDropAlertDto> response = new LinkedList<>();
			 CustomerOnbordingDTO cdto = null;
				 if(enty != null) {	
				 ObjectMapper mapper = new ObjectMapper();
				 mapper.registerModule(new JavaTimeModule());
			     mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
				 dto = mapper.convertValue(enty,new TypeReference<List<PriceDropAlertDto>>(){});				
				 //System.out.println(dto);
				 for(PriceDropAlertDto obj : dto){
					// System.out.println(obj);
				 obj.setImage(null);
				 if(obj.getTokens() != null) {
				 for(String s : obj.getTokens()){
				 try {
				 cdto = cllinicFeign.getCustomerByToken(s);
				//System.out.println(cdto);
				 }catch(Exception e) {System.out.println(e.getMessage());}				
				 if(cdto != null) {	
				 Map<String,CustomerInfo> info = new HashMap<>();
				 CustomerInfo cInfo = new CustomerInfo();
				 cInfo.setMobileNumber(cdto.getMobileNumber());
				 cInfo.setCustomerId(cdto.getCustomerId());
				 cInfo.setPatientId(cdto.getPatientId());
				 info.put(cdto.getFullName(), cInfo);
				 if(obj.getCustomerData() != null) {
				 List<Map<String,CustomerInfo>> customerData =	obj.getCustomerData();
				 customerData.add(info);
				 obj.setCustomerData(customerData);
				 }else{
				 List<Map<String,CustomerInfo>> customerData = new ArrayList<>();
				 customerData.add(info);
				 obj.setCustomerData(customerData);
				 }}}}
				 obj.setTokens(null);
				 response.add(obj);}}
			 res.setData(response);
			 res.setMessage("fetched successfully");
			 res.setStatus(200);
			 res.setSuccess(true);
		 }catch(Exception e) {
			 res.setMessage(e.getMessage());
			 res.setStatus(500);
			 res.setSuccess(false);
		 }
		 return ResponseEntity.status(res.getStatus()).body(res);		 
	 }
	 	
	 
//	 @Scheduled(cron = "0 30 8 * * ?")
//	 public void sendBirthdayWishes() {
//		 try {
//			 List<CustomerOnbordingDTO> cusmr =  new ObjectMapper().convertValue(cllinicFeign.getAllCustomers().getBody().getData(), new TypeReference< List<CustomerOnbordingDTO>>() {});
//			 //System.out.println(cusmr);
//			 cusmr.stream().map(n->{ 
//				 if(n.getDateOfBirth() != null) {
//				 DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
//			        LocalDate dob = LocalDate.parse(n.getDateOfBirth(), formatter);	
//			        //System.out.println(dob);
//			        LocalDate today = LocalDate.now();	
//			       // System.out.println(today);
//			        MonthDay customerdobMonthDay = MonthDay.from(dob);
//			        //System.out.println(customerdobMonthDay);
//			        MonthDay todayMonthDay = MonthDay.from(today);	
//			        //System.out.println(customerdobMonthDay);
//			        if (customerdobMonthDay.equals(todayMonthDay)) {
//			        	//System.out.println(n);
//			        	if(n.getDeviceId() != null) {
//			        		System.out.println(n.getDeviceId());
//			 appNotification.sendPushNotification(n.getDeviceId(),"🎉 Happy Birthday, " + n.getFullName() + "!","Your health and happiness are our priority. Have a great birthday!", "birthdayGreeting",
//					    "bithdayGreetingsScreen","default","dashboard");
//			        	//System.out.println("notifications sent successfully");
//			 }}
//			 return n;}return null;}).toList();
//		 }catch(Exception e) {
//			 System.out.println(e.getMessage());
//		 }
//		 
//	 }	
	 
	 	
	 public ResponseEntity<?> updatePriceDropAlert(
	         String clinicId,
	        String branchId,
	        String id,
	        PriceDropAlertDto dto) {

	     Response res = new Response();
	     try {
	         PriceDropAlertEntity existingList = priceDropAlertNotifications.findByClinicIdAndBranchIdAndId(clinicId, branchId,id);
	         if (existingList == null) {
	             res.setMessage("No Price Drop Alert found for Clinic ID: " + clinicId + " and Branch ID: " + branchId);
	             res.setStatus(404);
	             res.setSuccess(false);
	             return ResponseEntity.status(404).body(res);
	         }
	         ObjectMapper mapper = new ObjectMapper();
	         List<PriceDropAlertEntity> updatedList = new ArrayList<>();
	             // Map DTO → temporary entity
	             PriceDropAlertEntity updatedEntity = mapper.convertValue(dto, PriceDropAlertEntity.class);	            
	             updatedEntity.setId(existingList.getId());	            
	             updatedEntity = priceDropAlertNotifications.save(updatedEntity);
	             updatedList.add(updatedEntity);	        
	         res.setData(updatedList);
	         res.setMessage("Price drop alert(s) updated successfully");
	         res.setStatus(200);
	         res.setSuccess(true);
	     } catch (Exception e) {
	         res.setMessage(e.getMessage());
	         res.setStatus(500);
	         res.setSuccess(false);
	     }
	     return ResponseEntity.status(res.getStatus()).body(res);
	 }

	 	 
	 public ResponseEntity<?> deletePriceDropAlerts(
	         String clinicId,
	        String branchId,
	        String id) {

	     Response res = new Response();
	     try {
	         // Fetch all matching records
	         PriceDropAlertEntity existingList = priceDropAlertNotifications.findByClinicIdAndBranchIdAndId(clinicId, branchId,id);
	         if (existingList == null) {
	             res.setMessage("No Price Drop Alerts found for Clinic ID: " + clinicId + " and Branch ID: " + branchId);
	             res.setStatus(404);
	             res.setSuccess(false);
	             return ResponseEntity.status(404).body(res);
	         }
	         // Delete all matching records
	         priceDropAlertNotifications.delete(existingList);
	         res.setMessage("Price drop alerts deleted successfully for Clinic ID: " + clinicId + " and Branch ID: " + branchId);
	         res.setStatus(200);
	         res.setSuccess(true);	        
	     } catch (Exception e) {
	         res.setMessage(e.getMessage());
	         res.setStatus(500);
	         res.setSuccess(false);
	     }

	     return ResponseEntity.status(res.getStatus()).body(res);
	 } 
	 
	 
//		@Scheduled(cron = "0 10 7 * * ?")
//		public void remindHomeExcercises() {
//			     try {
//			        // Fetch doctor details
//			        Response obj = doctorFeign.getAllDoctorSaveDetails().getBody();     
//			        ObjectMapper mapper = new ObjectMapper();
//			        mapper.registerModule(new JavaTimeModule());
//			        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//			        List<DoctorSaveDetails> doctorSaveDetailsDTOs =
//			        		mapper.convertValue(obj.getData(), new TypeReference<List<DoctorSaveDetails>>() {});
//	
//			       // System.out.println("Fetched doctors: " + doctorSaveDetailsDTOs.size());
//	
//			        for (DoctorSaveDetails doctorSaveDetailsDTO : doctorSaveDetailsDTOs) {
//	
//			            LocalDateTime visitedDate = doctorSaveDetailsDTO.getVisitDateTime();
//	
//			            // Fetch booking details
//			            // Iterate over prescribed medicines
//			            for (Medicines m : doctorSaveDetailsDTO.getPrescription().getMedicines()) {
//			                long duration = convertDurationToDays(m.getDuration(),m.getDurationUnit()); // already long?
//			               // System.out.println(duration);
//			                LocalDateTime plusDays = visitedDate.plusDays(duration);
//		                   // System.out.println(plusDays);
//			                LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
//		                    //System.out.println(now);
//		                    //System.out.println(visitedDate);
//			                // Check if today is within duration
//			                if (!now.isBefore(visitedDate) && !now.isAfter(plusDays)) {
//			                	//System.out.println("invoked for times");
//			                    boolean isEvening = Arrays.stream(m.getRemindWhen().split(" "))
//			                            .anyMatch(time -> time.equalsIgnoreCase("Evening"));
//			                    if(isEvening){
//				                    if(bookingResponse == null) {
//				                    	isCalledAlready = true;
//				                    }else{
//				                    if(!bookingResponse.getBookingId().equalsIgnoreCase(doctorSaveDetailsDTO.getBookingId())) {
//				                    	isCalledAlready = true;
//				                    }else {
//				                    	isCalledAlready = false;	
//				                    }}
//				                    if(isCalledAlready){
//			                    try{
//			    		            ResponseEntity<ResponseStructure<BookingResponse>> res =
//			    		                    bookServiceFeign.getBookedService(doctorSaveDetailsDTO.getBookingId());
//			    		            bookingResponse = res.getBody().getData();
//	
//			    		            if (bookingResponse == null) {
//			    		                //System.out.println("No booking found for ID: " + doctorSaveDetailsDTO.getBookingId());
//			    		                continue;
//			    		            }
//	
//			    		            //System.out.println("Booking: " + bookingResponse);
//			    		            }catch(Exception e) {
//			    		            	 System.out.println(e.getMessage());
//			    		            }}
//			                    //System.out.println(isAfternoon);
//			                    if (bookingResponse != null && bookingResponse.getCustomerDeviceId() != null) {
//			                   // System.out.println(bookingResponse.getCustomerDeviceId());	
//			                   // System.out.println("not invoke");
//				 
//				 	        appNotification.sendPushNotification(
//				 			bookingResponse.getCustomerDeviceId(),
//				             "🌆 Good evening!",
//				            "Time to take your prescribed "+m.getName()+","+m.getDose()+" with water.",
//				             "MEDICINE REMINDER",
//				 		    "reminderScreen","default","dashboard"
//				         );	
//			             }}}}}}catch (Exception e) {e.printStackTrace();}}
//	
	 
	 public void sendBulkExerciseReminders(
		        List<ExerciseInfo> reminders) {

		    if (reminders == null || reminders.isEmpty()) {
		        return;
		    }

		    Map<String, List<ExerciseInfo>> patientWiseReminders =
		            reminders.stream()
		                    .collect(Collectors.groupingBy(
		                            ExerciseInfo::getPatientId));

		    patientWiseReminders.forEach(
		            (patientId, exerciseInfos) -> {

		                sendPatientExerciseReminder(
		                        patientId,
		                        exerciseInfos);
		            });
		}
	 
	 private void sendPatientExerciseReminder(
		        String patientId,
		        List<ExerciseInfo> exercises) {
		 String exerciseNames = null;
		 patient_Id = patientId;
		 if(deviceId == null) {
		    deviceId = cllinicFeign.customerDeviceId(patientId);
		 }
		 if(!patient_Id.equalsIgnoreCase(patientId)) {
			  deviceId = cllinicFeign.customerDeviceId(patientId); 
		 }
		 if(exercises.size()>1) {
		    exerciseNames =
		            exercises.stream()
		                    .map(ExerciseInfo::getExerciseName)
		                    .distinct()
		                    .collect(Collectors.joining(", "));}
		 else {
			 exerciseNames =
			            exercises.stream()
			                    .map(ExerciseInfo::getExerciseName)
			                    .distinct().findFirst().get();} 

		    String title = "Home Exercise Reminder";

		    String body =
		            "Please complete your prescribed home exercises today: "
		                    + exerciseNames;
		    
		    if(deviceId != null) {
				appNotification.sendPushNotification(deviceId,title,body, "homeExcercise",
					    "BookingScreen","default","Bookings");}

		 }
	 
}
