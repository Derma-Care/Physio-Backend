package com.chiselon.notificationservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.notificationservice.entity.DoctorPushNotification;

public interface DoctorPushNotificationRepository
        extends MongoRepository<DoctorPushNotification, String> {

    boolean existsByBookingIdAndAppointmentType(
            String bookingId,
            String appointmentType);
}