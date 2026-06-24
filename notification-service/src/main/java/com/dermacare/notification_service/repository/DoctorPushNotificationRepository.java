package com.dermacare.notification_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dermacare.notification_service.entity.DoctorPushNotification;

public interface DoctorPushNotificationRepository
        extends MongoRepository<DoctorPushNotification, String> {

    boolean existsByBookingIdAndAppointmentType(
            String bookingId,
            String appointmentType);
}