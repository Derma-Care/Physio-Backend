package com.dermacare.notification_service.service;

import org.springframework.http.ResponseEntity;

import com.dermacare.notification_service.dto.DoctorPushNotificationDTO;
import com.dermacare.notification_service.dto.DoctorRatingNotificationDTO;

public interface DoctorPushNotificationService {

	ResponseEntity<?> sendNotification(DoctorPushNotificationDTO dto);

	ResponseEntity<?> sendDoctorRatingNotification(DoctorRatingNotificationDTO dto);
}