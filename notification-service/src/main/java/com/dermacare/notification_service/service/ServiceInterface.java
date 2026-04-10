package com.dermacare.notification_service.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.dermacare.notification_service.dto.BookingResponse;
import com.dermacare.notification_service.dto.NotificationDTO;
import com.dermacare.notification_service.dto.NotificationResponse;
import com.dermacare.notification_service.dto.NotificationToCustomer;
import com.dermacare.notification_service.dto.PriceDropAlertDto;
import com.dermacare.notification_service.dto.ResBody;


public interface ServiceInterface {

	public void createNotification(BookingResponse booking);

	public ResBody<List<NotificationDTO>> notificationtodoctor( String hospitalId,
			 String doctorId);

    ResBody<List<NotificationDTO>> sendNotificationToClinic(String clinicId);

    ResBody<NotificationDTO> notificationResponse(NotificationResponse notificationResponse);

    NotificationDTO getNotificationByBookingId(String bookingId);

    NotificationDTO updateNotification(NotificationDTO notificationDTO);

    public ResponseEntity<ResBody<List<NotificationToCustomer>>> notificationToCustomer(
			 String customerMobileNumber);
    
    public void sendAlertNotifications();
    
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
    	
}
