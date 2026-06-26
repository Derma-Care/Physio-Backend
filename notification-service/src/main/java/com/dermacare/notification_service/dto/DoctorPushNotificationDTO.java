package com.dermacare.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorPushNotificationDTO {

    private String doctorId;

    private String bookingId;

    private String patientName;

    private String appointmentDate;

    private String appointmentTime;

    private String appointmentType;
}
