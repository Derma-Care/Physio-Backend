package com.dermacare.notification_service.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "DoctorPushNotifications")
public class DoctorPushNotification {

    @Id
    private String id;

    private String doctorId;

    private String bookingId;

    private String appointmentType;

    private String patientName;

    private String appointmentDate;

    private String appointmentTime;

    private String title;

    private String body;

    private boolean sent;

    private String createdAt;
}