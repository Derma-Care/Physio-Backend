package com.dermacare.doctorservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class NotificationDTO {
	private String notificationId;;
	private String message;
	private String date;
	private String time;
	private BookingResponse data;
	private String[] actions;

}
