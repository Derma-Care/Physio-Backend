package com.dermacare.notification_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dermacare.notification_service.dto.DoctorPushNotificationDTO;
import com.dermacare.notification_service.service.DoctorPushNotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notificationservice")
@RequiredArgsConstructor
public class DoctorPushNotificationController {

    private final DoctorPushNotificationService service;

    @PostMapping("doctor-push/send")
    public ResponseEntity<?> sendNotification(
            @RequestBody DoctorPushNotificationDTO dto) {

        return service.sendNotification(dto);
    }
}