package com.chiselon.notificationservice.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;

import com.chiselon.notificationservice.dto.BookingResponse;
import com.chiselon.notificationservice.dto.ExerciseInfo;
import com.chiselon.notificationservice.dto.NotificationDTO;
import com.chiselon.notificationservice.dto.NotificationResponse;
import com.chiselon.notificationservice.dto.NotificationToCustomer;
import com.chiselon.notificationservice.dto.PriceDropAlertDto;
import com.chiselon.notificationservice.dto.ResBody;
import com.chiselon.notificationservice.dto.Response;


public interface ServiceInterface {

	public ResponseEntity<Response> createNotification(BookingResponse booking);

//	public ResBody<List<NotificationDTO>> notificationtodoctor( String hospitalId,
//			 String doctorId);
//
    ResBody<List<NotificationDTO>> sendNotificationToClinic(String clinicId);
//
   ResBody<NotificationDTO> notificationResponse(NotificationResponse notificationResponse);
//
   NotificationDTO getNotificationByBookingId(String bookingId);
//
   NotificationDTO updateNotification(NotificationDTO notificationDTO);
//
   public ResponseEntity<ResBody<List<NotificationToCustomer>>> notificationToCustomer(
			 String customerMobileNumber);
//    
//    public void sendAlertNotifications();
//    
//    public ResponseEntity<?> sendImageNotifications(PriceDropAlertDto priceDropAlertDto);
//   
//    public ResponseEntity<?> priceDropNotifications(String clinicId,String branchId);
//    
//    public ResponseEntity<?> updatePriceDropAlert(
//	         String clinicId,
//	        String branchId,
//	        String id,
//	        PriceDropAlertDto dto) ;
//    
//    public ResponseEntity<?> deletePriceDropAlerts(
//	         String clinicId,
//	        String branchId,
//	        String id);
    
    
    public ResponseEntity<?> sendImageNotifications(PriceDropAlertDto priceDropAlertDto);
   
    public ResponseEntity<?> priceDropNotifications(String clinicId,String branchId);
    
    public ResponseEntity<?> updatePriceDropAlert(
	         String clinicId,
	        String branchId,
	        String id,
	        PriceDropAlertDto dto) ;
    
    public ResponseEntity<?> deletePriceDropAlerts(
	         String clinicId,
	        String branchId,
	        String id);
    
    public void sendNotificationToTherapist(Map<String, String> data);
    
    public void sendOverallFeedbackNotificationToTherapist(Map<String, String> data) ;
    
    public void sendSessionFeedbackNotificationToTherapist(Map<String, String> data);
    
    public void sendSessionReassignNotificationToTherapist(Map<String, String> data);
    
    public void sendSessionWithdrawNotificationToTherapist(Map<String, String> data);	  
    
    public void sendBulkExerciseReminders(
	        List<ExerciseInfo> reminders);

    	
}
