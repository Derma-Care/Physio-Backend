package com.chiselon.notificationservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chiselon.notificationservice.dto.DoctorPushNotificationDTO;
import com.chiselon.notificationservice.dto.DoctorRatingNotificationDTO;
import com.chiselon.notificationservice.service.DoctorPushNotificationService;

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
    
    @PostMapping("/doctor-rating/send")
    public ResponseEntity<?> sendDoctorRatingNotification(
            @RequestBody DoctorRatingNotificationDTO dto) {

        return service.sendDoctorRatingNotification(dto);
    }
}
