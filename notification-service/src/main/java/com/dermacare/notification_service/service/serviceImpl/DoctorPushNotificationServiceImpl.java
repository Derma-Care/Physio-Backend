package com.dermacare.notification_service.service.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.dermacare.notification_service.dto.DoctorPushNotificationDTO;
import com.dermacare.notification_service.dto.Response;
import com.dermacare.notification_service.entity.DoctorPushNotification;
import com.dermacare.notification_service.feign.CllinicFeign;
import com.dermacare.notification_service.notificationFactory.SendAppNotification;
import com.dermacare.notification_service.repository.DoctorPushNotificationRepository;
import com.dermacare.notification_service.service.DoctorPushNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorPushNotificationServiceImpl implements DoctorPushNotificationService {

	private final DoctorPushNotificationRepository repository;

	private final CllinicFeign clinicFeign;

	private final SendAppNotification appNotification;

	@Override
	public ResponseEntity<?> sendNotification(DoctorPushNotificationDTO dto) {

		Response res = new Response();

		try {

			if (repository.existsByBookingIdAndAppointmentType(dto.getBookingId(), dto.getAppointmentType())) {

				res.setMessage("Notification Already Sent");
				res.setStatus(200);
				res.setSuccess(true);

				return ResponseEntity.status(res.getStatus()).body(res);
			}

			String token = clinicFeign.getDoctorDeviceId(dto.getDoctorId());

			if (token == null || token.isBlank()) {

				res.setMessage("Doctor FCM Token Not Found");
				res.setStatus(404);
				res.setSuccess(false);

				return ResponseEntity.status(res.getStatus()).body(res);
			}

			String title;
			String body;

			if ("FOLLOW_UP".equalsIgnoreCase(dto.getAppointmentType())) {

				title = "Follow-up Appointment Scheduled";

				body = "A follow-up session has been scheduled for Patient " + dto.getPatientName() + " at "
						+ dto.getAppointmentTime();

			} else {

				title = "New Appointment Booked";

				body = "A new appointment has been booked for Patient " + dto.getPatientName() + " at "
						+ dto.getAppointmentTime();
			}
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

			LocalDate appointmentDate = LocalDate.parse(dto.getAppointmentDate(), formatter);

			LocalDate today = LocalDate.now();

			String navigationScreen = appointmentDate.isEqual(today) ? "dashboard" : "appointments";
			appNotification.sendPushNotification(token, title, body, "DOCTOR_APPOINTMENT",
					"/appointments/" + dto.getBookingId(), "default", navigationScreen);

			DoctorPushNotification notification = new DoctorPushNotification();

			notification.setDoctorId(dto.getDoctorId());
			notification.setBookingId(dto.getBookingId());
			notification.setAppointmentType(dto.getAppointmentType());
			notification.setPatientName(dto.getPatientName());
			notification.setAppointmentDate(dto.getAppointmentDate());
			notification.setAppointmentTime(dto.getAppointmentTime());
			notification.setTitle(title);
			notification.setBody(body);
			notification.setSent(true);
			notification.setCreatedAt(LocalDateTime.now().toString());

			repository.save(notification);

			res.setMessage("Doctor Notification Sent Successfully");
			res.setStatus(200);
			res.setSuccess(true);

		} catch (Exception e) {

			res.setMessage(e.getMessage());
			res.setStatus(500);
			res.setSuccess(false);
		}

		return ResponseEntity.status(res.getStatus()).body(res);
	}
}
