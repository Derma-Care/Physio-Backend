package com.chiselon.notificationservice.service;

import org.springframework.http.ResponseEntity;

import com.chiselon.notificationservice.dto.DoctorPushNotificationDTO;
import com.chiselon.notificationservice.dto.DoctorRatingNotificationDTO;

public interface DoctorPushNotificationService {

	ResponseEntity<?> sendNotification(DoctorPushNotificationDTO dto);

	ResponseEntity<?> sendDoctorRatingNotification(DoctorRatingNotificationDTO dto);
}