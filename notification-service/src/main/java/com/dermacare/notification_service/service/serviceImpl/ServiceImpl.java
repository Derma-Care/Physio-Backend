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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import com.dermacare.notification_service.dto.BookingResponse;
import com.dermacare.notification_service.dto.CustomerInfo;
import com.dermacare.notification_service.dto.CustomerOnbordingDTO;
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
//import com.dermacare.notification_service.feign.DoctorFeign;
import com.dermacare.notification_service.notificationFactory.SendAppNotification;
import com.dermacare.notification_service.repository.NotificationRepository;
import com.dermacare.notification_service.repository.PriceDropAlertNotifications;
import com.dermacare.notification_service.service.ServiceInterface;
import com.dermacare.notification_service.util.FeignImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import feign.FeignException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
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
    private FeignImpl feignImpl;
    
    @Autowired
    private PriceDropAlertNotifications priceDropAlertNotifications;

	Set<String> bookings = new LinkedHashSet<>();
	
	 BookingResponse bookingResponse;	 
	// private boolean isCalledAlready;	 
	 String imag = null;
	 String customerDeviceId = null;
	 String Id = null;
			
	 @Override
	 @Secured("ROLE_BOOKINGSERVICE")
	 @RateLimiter(name = "notificationService", fallbackMethod = "createNotificationFallback")
	 public ResponseEntity<Response> createNotification(BookingResponse bookingDTO) {

	     log.info("Create notification request received. BookingId={}, CustomerId={}, ClinicId={}, BranchId={}",
	             bookingDTO.getBookingId(),
	             bookingDTO.getCustomerId(),
	             bookingDTO.getClinicId(),
	             bookingDTO.getBranchId());

	     Response res = new Response();

	     try {

	         log.debug("Converting booking to notification entity. BookingId={}",
	                 bookingDTO.getBookingId());

	         convertToNotification(bookingDTO);

	         String title = buildTitle(bookingDTO);
	         String body = buildBody(bookingDTO);

	         log.debug("Notification title and body prepared. BookingId={}",
	                 bookingDTO.getBookingId());

	         log.info("Fetching customer device id. CustomerId={}",
	                 bookingDTO.getCustomerId());

	         customerDeviceId =
	        		 feignImpl.customerDeviceId(bookingDTO.getCustomerId());

	         log.info("Customer device id fetched successfully. DevicePresent={}",
	                 customerDeviceId != null);

	         log.info("Fetching clinic device id. ClinicId={}, BranchId={}",
	                 bookingDTO.getClinicId(),
	                 bookingDTO.getBranchId());

	         Id = feignImpl.getDeviceId(
	                 bookingDTO.getClinicId(),
	                 bookingDTO.getBranchId());

	         log.info("Clinic device id fetched successfully. DevicePresent={}",
	                 Id != null);

	         try {

	             if (customerDeviceId != null) {

	                 log.info("Sending notification to customer. BookingId={}, DeviceId={}",
	                         bookingDTO.getBookingId(),
	                         customerDeviceId);

	                 appNotification.sendPushNotification(
	                         customerDeviceId,
	                         title,
	                         body,
	                         "BOOKING",
	                         "BookingScreen",
	                         "default",
	                         "dashboard");

	                 log.info("Customer notification sent successfully. BookingId={}",
	                         bookingDTO.getBookingId());
	             }

	             if (Id != null) {

	                 String content =
	                         "AppointmentId:" + bookingDTO.getBookingId() + "\n\n"
	                                 + "Doctor:" + bookingDTO.getDoctorName() + "\n\n"
	                                 + "Branch:" + bookingDTO.getBranchname() + "\n\n"
	                                 + "Date:" + bookingDTO.getServiceDate() + "\n\n"
	                                 + "Time:" + bookingDTO.getServicetime();

	                 log.info("Sending notification to clinic admin. BookingId={}, DeviceId={}",
	                         bookingDTO.getBookingId(),
	                         Id);

	                 appNotification.sendPushNotification(
	                         Id,
	                         "An appointment has been successfully confirmed for "
	                                 + bookingDTO.getName() + ".\n\n",
	                         content,
	                         "BOOKING",
	                         "BookingScreen",
	                         "default",
	                         "dashboard");

	                 log.info("Clinic admin notification sent successfully. BookingId={}",
	                         bookingDTO.getBookingId());
	             }

	             res.setMessage("notification sent");
	             res.setStatus(200);
	             res.setSuccess(true);

	             log.info("Notification process completed successfully. BookingId={}",
	                     bookingDTO.getBookingId());

	         } catch (Exception e) {

	             log.error("Failed while sending push notification. BookingId={}, Error={}",
	                     bookingDTO.getBookingId(),
	                     e.getMessage(),
	                     e);

	             res.setMessage(e.getMessage());
	             res.setStatus(404);
	             res.setSuccess(false);
	         }

	     } catch (Exception e) {

	         log.error("Unexpected error while creating notification. BookingId={}, Error={}",
	                 bookingDTO.getBookingId(),
	                 e.getMessage(),
	                 e);

	         res.setMessage(e.getMessage());
	         res.setStatus(500);
	         res.setSuccess(false);
	     }

	     log.info("Returning createNotification response. Status={}", res.getStatus());

	     return ResponseEntity.status(res.getStatus()).body(res);
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
	
	

	@RateLimiter(name = "notificationService", fallbackMethod = "notificationtodoctorFallback")
	public ResBody<List<NotificationDTO>> notificationtodoctor(String hospitalId,
	                                                           String doctorId) {

	    log.info("Fetching doctor notifications. HospitalId={}, DoctorId={}",
	            hospitalId, doctorId);

	    ResBody<List<NotificationDTO>> res = new ResBody<>();
	    List<NotificationDTO> eligibleNotifications = new ArrayList<>();
	    List<NotificationDTO> reversedEligibleNotifications = new ArrayList<>();

	    try {

	        log.debug("Fetching notifications from database");

	        List<NotificationEntity> entity =
	                repository.findByDataClinicIdAndDataDoctorId(hospitalId, doctorId);

	        log.info("Notifications fetched successfully. Count={}",
	                entity != null ? entity.size() : 0);

	        List<NotificationDTO> dto =
	                new ObjectMapper().convertValue(entity,
	                        new TypeReference<List<NotificationDTO>>() {});

	        DateTimeFormatter dateFormatter =
	                DateTimeFormatter.ofPattern("dd-MM-yyyy");

	        String currentDate = LocalDate.now().format(dateFormatter);

	        if (dto != null) {

	            for (NotificationDTO n : dto) {

	                if (n.getData().getStatus().equalsIgnoreCase("Confirmed")
	                        && n.getDate().equals(currentDate)) {

	                    eligibleNotifications.add(n);
	                }
	            }
	        }

	        log.info("Eligible notifications found={}",
	                eligibleNotifications.size());

	        for (int i = eligibleNotifications.size() - 1; i >= 0; i--) {
	            reversedEligibleNotifications.add(
	                    eligibleNotifications.get(i));
	        }

	        if (!eligibleNotifications.isEmpty()) {

	            log.info("Returning {} notifications to doctor",
	                    reversedEligibleNotifications.size());

	            res = new ResBody<>(
	                    "Notification sent Successfully",
	                    200,
	                    reversedEligibleNotifications);

	        } else {

	            log.warn("No notifications found for HospitalId={}, DoctorId={}",
	                    hospitalId, doctorId);

	            res = new ResBody<>(
	                    "NotificationInfo Not Found",
	                    200,
	                    null);
	        }

	    } catch (Exception e) {

	        log.error("Error while fetching doctor notifications. HospitalId={}, DoctorId={}, Error={}",
	                hospitalId,
	                doctorId,
	                e.getMessage(),
	                e);

	        res = new ResBody<>(e.getMessage(), 500, null);
	    }

	    return res;
	}
		
	@RateLimiter(name = "notificationService",
	        fallbackMethod = "sendNotificationToClinicFallback")
	public ResBody<List<NotificationDTO>> sendNotificationToClinic(
	        String clinicId) {

	    log.info("Fetching clinic notifications. ClinicId={}", clinicId);

	    ResBody<List<NotificationDTO>> response = new ResBody<>();

	    List<NotificationDTO> list = new ArrayList<>();
	    List<NotificationDTO> reversedList = new ArrayList<>();

	    try {

	        log.debug("Fetching all notifications from repository");

	        List<NotificationEntity> entity = repository.findAll();

	        log.info("Total notifications fetched={}",
	                entity != null ? entity.size() : 0);

	        List<NotificationDTO> dto =
	                new ObjectMapper().convertValue(entity,
	                        new TypeReference<List<NotificationDTO>>() {});

	        String currentDate = LocalDate.now()
	                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

	        if (dto != null) {

	            for (NotificationDTO n : dto) {

	                if (n.getData().getStatus().equalsIgnoreCase("Pending")
	                        && n.getData().getClinicId().equals(clinicId)
	                        && n.getDate().equals(currentDate)) {

	                    list.add(n);
	                }
	            }
	        }

	        log.info("Pending notifications found={}", list.size());

	        for (int i = list.size() - 1; i >= 0; i--) {
	            reversedList.add(list.get(i));
	        }

	        if (!list.isEmpty()) {

	            log.info("Returning {} notifications to clinic {}",
	                    reversedList.size(), clinicId);

	            response = new ResBody<>(
	                    "Notifications Are sent to the admin",
	                    200,
	                    reversedList);

	        } else {

	            log.warn("No notifications found for clinic={}", clinicId);

	            response = new ResBody<>(
	                    "Notifications Are Not Found",
	                    200,
	                    null);
	        }

	    } catch (Exception e) {

	        log.error("Failed to fetch clinic notifications. ClinicId={}, Error={}",
	                clinicId,
	                e.getMessage(),
	                e);

	        response = new ResBody<>(e.getMessage(), 500, null);
	    }

	    return response;
	}
	
	
	@RateLimiter(name = "notificationService",
	        fallbackMethod = "notificationResponseFallback")
	public ResBody<NotificationDTO> notificationResponse(
	        NotificationResponse notificationResponse) {

	    log.info(
	            "Notification response received. NotificationId={}, AppointmentId={}, Status={}",
	            notificationResponse.getNotificationId(),
	            notificationResponse.getAppointmentId(),
	            notificationResponse.getStatus());

	    try {

	        log.debug("Fetching appointment details from booking service");

	        ResponseEntity<ResponseStructure<BookingResponse>> response =
	                bookServiceFeign.getBookedService(
	                        notificationResponse.getAppointmentId());

	        BookingResponse booking =
	                response.getBody().getData();

	        log.debug("Fetching notification entity");

	        NotificationEntity notificationEntity =
	                repository.findByNotificationId(
	                        notificationResponse.getNotificationId());

	        if (booking == null) {

	            log.warn("Booking not found. AppointmentId={}",
	                    notificationResponse.getAppointmentId());

	            return new ResBody<>(
	                    "Booking not found for given appointment ID",
	                    404,
	                    null);
	        }

	        if (notificationEntity == null) {

	            log.warn("Notification not found. NotificationId={}",
	                    notificationResponse.getNotificationId());

	            return new ResBody<>(
	                    "Notification not found for given notification ID",
	                    404,
	                    null);
	        }

	        log.info("Booking and notification validated successfully");

	        if (booking.getDoctorId().equalsIgnoreCase(notificationResponse.getDoctorId())
	                && booking.getClinicId().equalsIgnoreCase(notificationResponse.getHospitalId())
	                && booking.getBookingId().equalsIgnoreCase(notificationResponse.getAppointmentId())
	                && booking.getSubServiceId().equalsIgnoreCase(notificationResponse.getSubServiceId())) {

	            String status = notificationResponse.getStatus();

	            log.info("Processing notification status={}", status);

	            switch (status) {

	                case "Accepted":

	                    log.info("Appointment accepted. AppointmentId={}",
	                            booking.getBookingId());

	                    booking.setStatus("Confirmed");
	                    notificationEntity.getData().setStatus("Confirmed");

	                    repository.save(notificationEntity);

	                    log.debug("Notification status updated to CONFIRMED");

	                    try {

	                        if (booking.getCustomerDeviceId() != null) {

	                            log.info("Sending acceptance notification to customer");

	                            appNotification.sendPushNotification(
	                                    booking.getCustomerDeviceId(),
	                                    " Hello " + booking.getName(),
	                                    booking.getDoctorName()
	                                            + " Accepted Your Appointment For "
	                                            + booking.getSubServiceName(),
	                                    "BOOKING SUCCESS",
	                                    "BookingVerificationScreen",
	                                    "default",
	                                    "dashboard");
	                        }

	                    } catch (Exception ex) {

	                        log.error(
	                                "Failed to send customer notification. AppointmentId={}",
	                                booking.getBookingId(),
	                                ex);
	                    }

	                    break;

	                case "Rejected":

	                    log.info("Appointment rejected. AppointmentId={}",
	                            booking.getBookingId());

	                    booking.setStatus("Rejected");
	                    booking.setReasonForCancel(
	                            notificationResponse.getReasonForCancel());

	                    cllinicFeign.makingFalseDoctorSlot(
	                            booking.getDoctorId(),
	                            booking.getBranchId(),
	                            booking.getServiceDate(),
	                            booking.getServicetime());

	                    notificationEntity.getData().setStatus("Rejected");

	                    repository.save(notificationEntity);

	                    log.debug("Notification status updated to REJECTED");

	                    break;

	                default:

	                    log.info("Appointment moved to pending state");

	                    booking.setStatus("Pending");
	                    notificationEntity.getData().setStatus("Pending");

	                    repository.save(notificationEntity);
	            }

	            log.info("Updating appointment in booking service");

	            ResponseEntity<?> book =
	                    bookServiceFeign.updateAppointment(booking);

	            if (book != null) {

	                log.info("Appointment updated successfully. AppointmentId={}",
	                        booking.getBookingId());

	                return new ResBody<>(
	                        "Appointment And Notification Status updated",
	                        200,
	                        null);
	            }

	            log.warn("Booking service returned null response");

	            return new ResBody<>(
	                    "Appointment Status Not updated",
	                    200,
	                    null);
	        }

	        log.warn("Validation failed for notification response");

	        return new ResBody<>(
	                "Status Not updated, please check provided details",
	                200,
	                null);

	    } catch (FeignException e) {

	        log.error("Feign exception while processing notification response. Error={}",
	                e.getMessage(),
	                e);

	        return new ResBody<>(e.getMessage(), 500, null);

	    } catch (Exception e) {

	        log.error("Unexpected exception while processing notification response. Error={}",
	                e.getMessage(),
	                e);

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
//	// @RateLimiter(name = "notificationService", fallbackMethod = "sendAlertNotificationsFallback")
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
//	
	 
//	 
//	 private boolean calculateTimeDifferenceForAlertNotification(String serviceTime) {	
//		 
//		   try {		   
//             SimpleDateFormat inputFormat = new SimpleDateFormat("hh:mm a"); ////used for convert string to date object
//			   
//			   ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
//		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
//		        String formattedTimeByZone = istTime.format(formatter);	////current asia time generated.present in form of string		   
//		        Date formattedCurrentTime = inputFormat.parse(formattedTimeByZone);	////converting String to date object	        
//		       SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");////converting form 12 hrs to 24 hrs
//		       String modifiedcurrentTime = simpleDateFormat.format(formattedCurrentTime);
//		       		      
//		      Date serviceTimeStringToDteObject = inputFormat.parse(serviceTime);		      
//		      SimpleDateFormat simpleDateFormatForNotificationTime = new SimpleDateFormat("HH:mm");
//		      String modifiedServiceTime = simpleDateFormatForNotificationTime.format(serviceTimeStringToDteObject);
//		      
//		       Date sTime = simpleDateFormat.parse(modifiedServiceTime );
//		       Date cTime = simpleDateFormat.parse(modifiedcurrentTime);
////		       
////		       System.out.println(sTime);
////		       System.out.println(cTime);
//		       long differenceInMilliSeconds
//		           =  sTime.getTime() - cTime.getTime();     
//		       		      
//		       long differenceInMinutes
//		           = differenceInMilliSeconds / (60 * 1000);///it wont ignores hours convert then into minutes
//		       System.out.println(differenceInMinutes);
//		       
//		       if(differenceInMinutes != 0 && differenceInMinutes >= 1 &&  differenceInMinutes <= 5  ) {
//		    	   return true;
//		    	 }else{
//		    	    return false;}
//		   }catch(ParseException e) {
//			   return false;}
//		   }
	
		 
//	 private void sendAlertPushNotification(String appointmentId) {
//		 try {
//			 ResponseEntity<ResponseStructure<BookingResponse>> res = bookServiceFeign.getBookedService(appointmentId);
//		        BookingResponse b = res.getBody().getData();
//		       
//		        if (b != null) {
//		        	 try {
//	                        if(customerDeviceId != null && b.getDoctorDeviceId() != null) {
//	                            appNotification.sendPushNotification(
//	                            		customerDeviceId,
//	                                " Hello " + b.getName()+ "," ,
//	                                b.getDoctorName() + " Connect With You Through Video Call within 5 Minutes ", "Alert",
//	                			    "AlertScreen","default","dashboard");
//	                            
//	                            appNotification.sendPushNotification(
//	                                b.getDoctorDeviceId(),
//	                                " Hello " +b.getDoctorName()+ "," , " You Have a Video Consultation within 5 Minutes With " +
//	                                b.getName(), "Alert",
//	                			    "AlertScreen","default","dashboard");}
//	                            
//	                            if(b.getDoctorWebDeviceId() != null) {
//	                            	 appNotification.sendPushNotification(
//	     	                                b.getDoctorWebDeviceId(),
//	     	                                " Hello " +b.getDoctorName()+ "," , " You Have a Video Consultation within 5 Minutes With " +
//	     	                                b.getName(), "Alert",
//	     	                			    "AlertScreen","default","dashboard");}
//	                            
//	                            if(b.getClinicDeviceId() != null) {
//	                            	 appNotification.sendPushNotification(
//	     	                                b.getClinicDeviceId(),
//	     	                                " Hello ClinicAdmin", b.getDoctorName()+ " Have a Video Consultation within 5 Minutes With " +
//	     	                                b.getName(), "Alert",
//	     	                			    "AlertScreen","default","dashboard");}
//	                            
//	                            //System.out.println("Notification sent to doctor and customer");
//	                    }catch (Exception ex) {}}
//			 }catch(Exception e) {}
//		  }
//	 
//	 
	
	@Override
	@RateLimiter(name = "notificationService", fallbackMethod = "getNotificationByBookingIdFallback")
	public NotificationDTO getNotificationByBookingId(String bookingId) {

	    log.info("Fetching notification by bookingId={}", bookingId);

	    try {

	        NotificationEntity notification =
	                repository.findByDataBookingId(bookingId);

	        if (notification == null) {

	            log.warn("No notification found for bookingId={}", bookingId);
	            return null;
	        }

	        log.debug("Notification entity found for bookingId={}", bookingId);

	        NotificationDTO dto =
	                new ObjectMapper().convertValue(
	                        notification,
	                        NotificationDTO.class);

	        log.info("Successfully converted notification entity to DTO. BookingId={}",
	                bookingId);

	        return dto;

	    } catch (Exception e) {

	        log.error("Error while fetching notification. BookingId={}, Error={}",
	                bookingId,
	                e.getMessage(),
	                e);

	        throw e;
	    }
	}

	@Override
	@RateLimiter(name = "notificationService", fallbackMethod = "updateNotificationFallback")
	public NotificationDTO updateNotification(NotificationDTO notificationDTO) {

	    log.info("Update notification request received");

	    try {

	        log.debug("Converting NotificationDTO to NotificationEntity");

	        NotificationEntity entity =
	                new ObjectMapper().convertValue(
	                        notificationDTO,
	                        NotificationEntity.class);

	        log.info("Saving notification entity to database");

	        NotificationEntity notification =
	                repository.save(entity);

	        log.info("Notification saved successfully. NotificationId={}",
	                notification.getNotificationId());

	        NotificationDTO dto =
	                new ObjectMapper().convertValue(
	                        notification,
	                        NotificationDTO.class);

	        log.info("Notification converted back to DTO successfully");

	        return dto;

	    } catch (Exception e) {

	        log.error("Failed to update notification. Error={}",
	                e.getMessage(),
	                e);

	        throw e;
	    }
	}

	@Override
	@RateLimiter(name = "notificationService", fallbackMethod = "notificationToCustomerFallback")
	public ResponseEntity<ResBody<List<NotificationToCustomer>>> notificationToCustomer(
	        String customerMobileNumber) {

	    log.info("Fetching customer notifications. MobileNumber={}",
	            customerMobileNumber);

	    long startTime = System.currentTimeMillis();

	    ResBody<List<NotificationToCustomer>> res =
	            new ResBody<List<NotificationToCustomer>>();

	    List<NotificationToCustomer> eligibleNotifications =
	            new ArrayList<>();

	    try {

	        log.debug("Fetching notifications from repository");

	        List<NotificationEntity> entity =
	                repository.findByDataMobileNumber(customerMobileNumber);

	        log.info("Notifications fetched successfully. Count={}",
	                entity != null ? entity.size() : 0);

	        List<NotificationDTO> dto =
	                new ObjectMapper().convertValue(
	                        entity,
	                        new TypeReference<List<NotificationDTO>>() {});

	        DateTimeFormatter dateFormatter =
	                DateTimeFormatter.ofPattern("dd-MM-yyyy");

	        String currentDate =
	                LocalDate.now().format(dateFormatter);

	        log.debug("Current date for filtering notifications={}",
	                currentDate);

	        if (dto != null) {

	            for (NotificationDTO n : dto) {

	                if (n.getData().getStatus()
	                        .equalsIgnoreCase("Confirmed")
	                        && n.getDate().equals(currentDate)) {

	                    log.debug(
	                            "Eligible notification found. DoctorName={}, ClinicName={}",
	                            n.getData().getDoctorName(),
	                            n.getData().getClinicName());

	                    NotificationToCustomer notification =
	                            new NotificationToCustomer();

	                    notification.setMessage("Appointment Accepted");
	                    notification.setHospitalName(
	                            n.getData().getClinicName());
	                    notification.setDoctorName(
	                            n.getData().getDoctorName());
	                    notification.setServiceDate(
	                            n.getData().getServiceDate());
	                    notification.setServiceTime(
	                            n.getData().getServicetime());
	                    notification.setServiceFee(
	                            n.getData().getTotalFee());
	                    notification.setConsultationType(
	                            n.getData().getConsultationType());
	                    notification.setConsultationFee(
	                            n.getData().getConsultationFee());

	                    eligibleNotifications.add(notification);
	                }
	            }
	        }

	        log.info("Eligible customer notifications count={}",
	                eligibleNotifications.size());

	        if (!eligibleNotifications.isEmpty()) {

	            log.info("Returning customer notifications successfully. Count={}",
	                    eligibleNotifications.size());

	            res = new ResBody<>(
	                    "Notification sent Successfully",
	                    200,
	                    eligibleNotifications);

	        } else {

	            log.warn("No eligible notifications found for customerMobileNumber={}",
	                    customerMobileNumber);

	            res = new ResBody<>(
	                    "Notifications Not Found",
	                    200,
	                    null);
	        }

	    } catch (FeignException e) {

	        log.error(
	                "Feign exception while fetching customer notifications. MobileNumber={}, Error={}",
	                customerMobileNumber,
	                e.getMessage(),
	                e);

	        res = new ResBody<>(
	                e.getMessage(),
	                500,
	                null);

	    } catch (Exception e) {

	        log.error(
	                "Unexpected exception while fetching customer notifications. MobileNumber={}, Error={}",
	                customerMobileNumber,
	                e.getMessage(),
	                e);

	        res = new ResBody<>(
	                e.getMessage(),
	                500,
	                null);
	    }

	    long endTime = System.currentTimeMillis();

	    log.info(
	            "notificationToCustomer completed. MobileNumber={}, Status={}, ExecutionTime={} ms",
	            customerMobileNumber,
	            res.getStatus(),
	            (endTime - startTime));

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
//	 private int convertDurationToDays(String duration,String durationUnit) {
//	        if (duration == null || duration.isEmpty()) {
//	            throw new IllegalArgumentException("Duration cannot be null or empty");
//	        }
//
//	        duration = duration.toLowerCase().trim();
//
//	        // Split into number and unit
//	        String[] parts = duration.split(" ");
//	        if (parts.length < 1) {
//	            throw new IllegalArgumentException("Invalid duration format: " + duration);
//	        }
//
//	        int number = Integer.parseInt(parts[0]); 
//	       //// String unit = parts[1];
//
//	        switch (durationUnit) {
//	            case "day":
//	            case "Day":
//	            case "Days":
//	            case "days":
//	                return number;
//	            case "week":
//	            case "Week":
//	            case "Weeks":
//	            case "weeks":
//	                return number * 7;
//	            case "month":
//	            case "Month":
//	            case "Months":
//	            case "months":
//	                return number * 30; // Approximation
//	            default:
//	                throw new IllegalArgumentException("Unsupported duration unit: " + durationUnit);
//	        }
//	    }
//		
	 
	
	 
//	 @Scheduled(cron = "0 30 8 * * ?")
//	/// @RateLimiter(name = "notificationService", fallbackMethod = "sendBirthdayWishesFallback")
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
//	
	
	
	@RateLimiter(name = "notificationService",
	        fallbackMethod = "priceDropNotificationsFallback")
	public ResponseEntity<?> priceDropNotifications(
	        String clinicId,
	        String branchId) {

	    log.info("Fetching price drop notifications. ClinicId={}, BranchId={}",
	            clinicId,
	            branchId);

	    Response res = new Response();

	    try {

	        List<PriceDropAlertEntity> entities =
	                priceDropAlertNotifications
	                        .findByClinicIdAndBranchId(clinicId, branchId);

	        log.info("Price drop alerts fetched. Count={}",
	                entities != null ? entities.size() : 0);

	        List<PriceDropAlertDto> response = new LinkedList<>();

	        if (entities != null) {

	            ObjectMapper mapper = new ObjectMapper();
	            mapper.registerModule(new JavaTimeModule());
	            mapper.disable(
	                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	            List<PriceDropAlertDto> dto =
	                    mapper.convertValue(
	                            entities,
	                            new TypeReference<List<PriceDropAlertDto>>() {
	                            });

	            for (PriceDropAlertDto obj : dto) {

	                log.debug("Processing alert");

	                obj.setImage(null);

	                if (obj.getTokens() != null) {

	                    log.debug("Total tokens found={}",
	                            obj.getTokens().size());

	                    for (String token : obj.getTokens()) {

	                        try {

	                            CustomerOnbordingDTO customer =
	                                    cllinicFeign.getCustomerByToken(token);

	                            if (customer != null) {

	                                Map<String, CustomerInfo> info =
	                                        new HashMap<>();

	                                CustomerInfo customerInfo =
	                                        new CustomerInfo();

	                                customerInfo.setMobileNumber(
	                                        customer.getMobileNumber());

	                                customerInfo.setCustomerId(
	                                        customer.getCustomerId());

	                                customerInfo.setPatientId(
	                                        customer.getPatientId());

	                                info.put(
	                                        customer.getFullName(),
	                                        customerInfo);

	                                if (obj.getCustomerData() != null) {

	                                    obj.getCustomerData().add(info);

	                                } else {

	                                    List<Map<String, CustomerInfo>>
	                                            customerData =
	                                            new ArrayList<>();

	                                    customerData.add(info);

	                                    obj.setCustomerData(customerData);
	                                }
	                            }

	                        } catch (Exception ex) {

	                            log.error(
	                                    "Failed to fetch customer by token={}",
	                                    token,
	                                    ex);
	                        }
	                    }
	                }

	                obj.setTokens(null);

	                response.add(obj);
	            }
	        }

	        log.info("Returning {} price drop alerts",
	                response.size());

	        res.setData(response);
	        res.setMessage("fetched successfully");
	        res.setStatus(200);
	        res.setSuccess(true);

	    } catch (Exception e) {

	        log.error(
	                "Failed to fetch price drop notifications. ClinicId={}, BranchId={}",
	                clinicId,
	                branchId,
	                e);

	        res.setMessage(e.getMessage());
	        res.setStatus(500);
	        res.setSuccess(false);
	    }

	    return ResponseEntity.status(res.getStatus()).body(res);
	}
	
	
	 	
	 @RateLimiter(name = "notificationService", fallbackMethod = "sendImageNotificationsFallback")
	 public ResponseEntity<?> sendImageNotifications(PriceDropAlertDto priceDropAlertDto) {

	     log.info("Send image notification request received. SendAll={}, Title={}",
	             priceDropAlertDto.getSendAll(),
	             priceDropAlertDto.getTitle());

	     long startTime = System.currentTimeMillis();

	     Response res = new Response();

	     try {

	         if (priceDropAlertDto.getImage() != null) {

	             log.debug("Image found in request");

	             imag = priceDropAlertDto.getImage();

	         } else {

	             log.debug("No image found in request");

	             imag = "";
	         }

	         if (priceDropAlertDto.getSendAll()) {

	             log.info("Sending notification to all customers");

	             List<CustomerOnbordingDTO> customers =
	                     new ObjectMapper().convertValue(
	                             cllinicFeign.getAllCustomers()
	                                     .getBody()
	                                     .getData(),
	                             new TypeReference<List<CustomerOnbordingDTO>>() {
	                             });

	             log.info("Total customers fetched={}",
	                     customers != null ? customers.size() : 0);

	             customers.stream().map(customer -> {

	                 try {

	                     if (customer.getDeviceId() != null) {

	                         log.debug(
	                                 "Sending notification to customer. CustomerId={}, DeviceId={}",
	                                 customer.getCustomerId(),
	                                 customer.getDeviceId());

	                         appNotification.sendPushNotificationForImage(
	                                 customer.getDeviceId(),
	                                 priceDropAlertDto.getTitle(),
	                                 priceDropAlertDto.getBody(),
	                                 "Notification",
	                                 "NotificationScreen",
	                                 "default",
	                                 imag);

	                         log.debug(
	                                 "Notification sent successfully to customerId={}",
	                                 customer.getCustomerId());
	                     }

	                 } catch (Exception ex) {

	                     log.error(
	                             "Failed to send notification to customerId={}",
	                             customer.getCustomerId(),
	                             ex);
	                 }

	                 return customer;

	             }).toList();

	         } else {

	             log.info("Sending notification to selected tokens. Count={}",
	                     priceDropAlertDto.getTokens() != null
	                             ? priceDropAlertDto.getTokens().size()
	                             : 0);

	             priceDropAlertDto.getTokens().stream().map(token -> {

	                 try {

	                     log.debug("Sending notification to token={}", token);

	                     appNotification.sendPushNotificationForImage(
	                             token,
	                             priceDropAlertDto.getTitle(),
	                             priceDropAlertDto.getBody(),
	                             "Notification",
	                             "NotificationScreen",
	                             "default",
	                             imag);

	                 } catch (Exception ex) {

	                     log.error("Failed to send notification to token={}",
	                             token,
	                             ex);
	                 }

	                 return token;

	             }).toList();
	         }

	         log.debug("Converting DTO to PriceDropAlertEntity");

	         PriceDropAlertEntity entity =
	                 new ObjectMapper().convertValue(
	                         priceDropAlertDto,
	                         PriceDropAlertEntity.class);

	         entity.setLocalDateTime(
	                 LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

	         log.info("Saving price drop alert history");

	         priceDropAlertNotifications.save(entity);

	         res.setStatus(200);
	         res.setMessage("successfully sent notification");
	         res.setSuccess(true);

	         log.info("Image notification completed successfully");

	     } catch (Exception e) {

	         log.error("Error while sending image notification. Error={}",
	                 e.getMessage(),
	                 e);

	         res.setStatus(500);
	         res.setMessage(e.getMessage());
	         res.setSuccess(false);
	     }

	     log.info("sendImageNotifications completed in {} ms",
	             (System.currentTimeMillis() - startTime));

	     return ResponseEntity.status(res.getStatus()).body(res);
	 }
	 
	
	 
	 @RateLimiter(name = "notificationService",
		        fallbackMethod = "updatePriceDropAlertFallback")
		public ResponseEntity<?> updatePriceDropAlert(
		        String clinicId,
		        String branchId,
		        String id,
		        PriceDropAlertDto dto) {

		    log.info(
		            "Update price drop alert request received. ClinicId={}, BranchId={}, AlertId={}",
		            clinicId,
		            branchId,
		            id);

		    Response res = new Response();

		    try {

		        PriceDropAlertEntity existingEntity =
		                priceDropAlertNotifications
		                        .findByClinicIdAndBranchIdAndId(
		                                clinicId,
		                                branchId,
		                                id);

		        if (existingEntity == null) {

		            log.warn(
		                    "Price drop alert not found. ClinicId={}, BranchId={}, AlertId={}",
		                    clinicId,
		                    branchId,
		                    id);

		            res.setMessage(
		                    "No Price Drop Alert found for Clinic ID: "
		                            + clinicId
		                            + " and Branch ID: "
		                            + branchId);

		            res.setStatus(404);
		            res.setSuccess(false);

		            return ResponseEntity.status(404).body(res);
		        }

		        log.info("Existing alert found. Updating alertId={}", id);

		        ObjectMapper mapper = new ObjectMapper();

		        PriceDropAlertEntity updatedEntity =
		                mapper.convertValue(
		                        dto,
		                        PriceDropAlertEntity.class);

		        updatedEntity.setId(existingEntity.getId());

		        updatedEntity =
		                priceDropAlertNotifications.save(updatedEntity);

		        log.info("Price drop alert updated successfully. AlertId={}",
		                updatedEntity.getId());

		        List<PriceDropAlertEntity> updatedList =
		                new ArrayList<>();

		        updatedList.add(updatedEntity);

		        res.setData(updatedList);
		        res.setMessage(
		                "Price drop alert(s) updated successfully");
		        res.setStatus(200);
		        res.setSuccess(true);

		    } catch (Exception e) {

		        log.error(
		                "Failed to update price drop alert. ClinicId={}, BranchId={}, AlertId={}",
		                clinicId,
		                branchId,
		                id,
		                e);

		        res.setMessage(e.getMessage());
		        res.setStatus(500);
		        res.setSuccess(false);
		    }

		    return ResponseEntity.status(res.getStatus()).body(res);
		}
	 
	 @RateLimiter(name = "notificationService",
		        fallbackMethod = "deletePriceDropAlertsFallback")
		public ResponseEntity<?> deletePriceDropAlerts(
		        String clinicId,
		        String branchId,
		        String id) {

		    log.info(
		            "Delete price drop alert request received. ClinicId={}, BranchId={}, AlertId={}",
		            clinicId,
		            branchId,
		            id);

		    Response res = new Response();

		    try {

		        PriceDropAlertEntity existingEntity =
		                priceDropAlertNotifications
		                        .findByClinicIdAndBranchIdAndId(
		                                clinicId,
		                                branchId,
		                                id);

		        if (existingEntity == null) {

		            log.warn(
		                    "Price drop alert not found. ClinicId={}, BranchId={}, AlertId={}",
		                    clinicId,
		                    branchId,
		                    id);

		            res.setMessage(
		                    "No Price Drop Alerts found for Clinic ID: "
		                            + clinicId
		                            + " and Branch ID: "
		                            + branchId);

		            res.setStatus(404);
		            res.setSuccess(false);

		            return ResponseEntity.status(404).body(res);
		        }

		        log.info("Deleting price drop alert. AlertId={}", id);

		        priceDropAlertNotifications.delete(existingEntity);

		        log.info("Price drop alert deleted successfully. AlertId={}",
		                id);

		        res.setMessage(
		                "Price drop alerts deleted successfully for Clinic ID: "
		                        + clinicId
		                        + " and Branch ID: "
		                        + branchId);

		        res.setStatus(200);
		        res.setSuccess(true);

		    } catch (Exception e) {

		        log.error(
		                "Failed to delete price drop alert. ClinicId={}, BranchId={}, AlertId={}",
		                clinicId,
		                branchId,
		                id,
		                e);

		        res.setMessage(e.getMessage());
		        res.setStatus(500);
		        res.setSuccess(false);
		    }

		    return ResponseEntity.status(res.getStatus()).body(res);
		}
	 
	 
	 public ResponseEntity<Response> buildRateLimitResponse() {
	        return ResponseEntity.status(429)
	                .body(new Response(
	                        "Too many requests. Please try again after some time.",
	                        429,
	                        false,
	                        null));
	    }
	 
    public ResponseEntity<Response> createNotificationFallback(BookingResponse bookingDTO, Exception ex){return buildRateLimitResponse();}
    public ResBody<List<NotificationDTO>> notificationtodoctorFallback(String hospitalId,String doctorId,Exception ex){return new ResBody<>("Too many requests",429,null);}
    public ResBody<List<NotificationDTO>> sendNotificationToClinicFallback(String clinicId,Exception ex){return new ResBody<>("Too many requests",429,null);}
    public ResBody<NotificationDTO> notificationResponseFallback(NotificationResponse notificationResponse,Exception ex){return new ResBody<>("Too many requests",429,null);}
    public void sendAlertNotificationsFallback(Exception ex){}
    public NotificationDTO getNotificationByBookingIdFallback(String bookingId,Exception ex){return null;}
    public NotificationDTO updateNotificationFallback(NotificationDTO notificationDTO,Exception ex){return null;}
    public ResponseEntity<ResBody<List<NotificationToCustomer>>> notificationToCustomerFallback(String customerMobileNumber,Exception ex){return ResponseEntity.status(429).body( new ResBody<>("Too many requests",429,null));}
    public ResponseEntity<?> sendImageNotificationsFallback(PriceDropAlertDto priceDropAlertDto,Exception ex){return buildRateLimitResponse();}
    public ResponseEntity<?> priceDropNotificationsFallback(String clinicId,String branchId,Exception ex){return buildRateLimitResponse();}
    public void sendBirthdayWishesFallback(Exception ex){}
    public ResponseEntity<?> updatePriceDropAlertFallback(String clinicId,String branchId,String id,PriceDropAlertDto dto,Exception ex){return buildRateLimitResponse();}
    public ResponseEntity<?> deletePriceDropAlertsFallback(String clinicId,String branchId,String id,Exception ex){return buildRateLimitResponse();}

}