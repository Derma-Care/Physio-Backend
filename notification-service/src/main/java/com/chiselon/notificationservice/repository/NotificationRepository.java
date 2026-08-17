package com.chiselon.notificationservice.repository;


import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.chiselon.notificationservice.entity.NotificationEntity;


@Repository
public interface NotificationRepository extends MongoRepository<NotificationEntity,String> {

	List<NotificationEntity> findByDataClinicIdAndDataDoctorId(String hospitalId, String doctorId);

	NotificationEntity findByDataBookingId(String bookingId);

	List<NotificationEntity> findByDataMobileNumber(String customerMobileNumber);

	NotificationEntity findByNotificationId(String notificationId);
}
