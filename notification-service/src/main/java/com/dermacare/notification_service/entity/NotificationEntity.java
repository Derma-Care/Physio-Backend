package com.dermacare.notification_service.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@Document(collection = "DermaCareNotifications")
public class NotificationEntity {
@Id
    private String notificationId;
	private String message;
	private String date;
	private String time;
	private Booking data;
	private boolean isAlerted;
	private String[] actions;
}
