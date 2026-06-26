package com.dermacare.notification_service.service;

import org.springframework.http.ResponseEntity;

import com.dermacare.notification_service.dto.BookingResponse;
import com.dermacare.notification_service.dto.DoctorPushNotificationDTO;

public interface DoctorPushNotificationService {



	ResponseEntity<?> sendNotification(DoctorPushNotificationDTO dto);
}