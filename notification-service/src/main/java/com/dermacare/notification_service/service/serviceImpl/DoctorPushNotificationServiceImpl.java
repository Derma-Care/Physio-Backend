package com.dermacare.notification_service.service.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.dermacare.notification_service.dto.DoctorPushNotificationDTO;
import com.dermacare.notification_service.dto.DoctorRatingNotificationDTO;
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
		System.out.println(dto);
		Response res = new Response();

		try {

			log.info("Doctor notification request received. BookingId: {}, DoctorId: {}, AppointmentType: {}",
					dto.getBookingId(), dto.getDoctorId(), dto.getAppointmentType());

			// Check duplicate notification
			if (repository.existsByBookingIdAndAppointmentType(dto.getBookingId(), dto.getAppointmentType())) {

				log.warn("Notification already exists for BookingId: {} and AppointmentType: {}", dto.getBookingId(),
						dto.getAppointmentType());

				res.setMessage("Notification Already Sent");
				res.setStatus(200);
				res.setSuccess(true);

				return ResponseEntity.status(res.getStatus()).body(res);
			}

			log.info("Fetching FCM token for DoctorId: {}", dto.getDoctorId());

			String token = clinicFeign.getDoctorDeviceId(dto.getDoctorId());

			if (token == null || token.isBlank()) {

				log.error("FCM token not found for DoctorId: {}", dto.getDoctorId());

				res.setMessage("Doctor FCM Token Not Found");
				res.setStatus(404);
				res.setSuccess(false);

				return ResponseEntity.status(res.getStatus()).body(res);
			}

			log.info("FCM token fetched successfully for DoctorId: {}", dto.getDoctorId());

			String title;
			String body;

//			if ("FOLLOW_UP".equalsIgnoreCase(dto.getAppointmentType())) {
//
//				title = "Follow-up Appointment Scheduled";
//				body = "A follow-up session has been scheduled for Patient " + dto.getPatientName() + " at "
//						+ dto.getAppointmentTime();
//
//				log.info("Preparing Follow-up notification.");
//
//			} else {
//
//				title = "New Appointment Booked";
//				body = "A new appointment has been booked for Patient " + dto.getPatientName() + " at "
//						+ dto.getAppointmentTime();
//
//				log.info("Preparing New Appointment notification.");
//			}

			if ("FOLLOW_UP".equalsIgnoreCase(dto.getAppointmentType())) {

				title = "Follow-up Appointment Scheduled";

				body = "A follow-up appointment has been scheduled.\n\n" + "Patient: " + dto.getPatientName()
						+ "\nDate: " + dto.getAppointmentDate() + "\nTime: " + dto.getAppointmentTime();

			} else {

				title = "New Appointment Booked";

				body = "A new appointment has been booked.\n\n" + "Patient: " + dto.getPatientName() + "\nDate: "
						+ dto.getAppointmentDate() + "\nTime: " + dto.getAppointmentTime();
			}
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

			LocalDate appointmentDate = LocalDate.parse(dto.getAppointmentDate(), formatter);
			LocalDate today = LocalDate.now();

			String navigationScreen = appointmentDate.isEqual(today) ? "dashboard" : "appointments";

			log.info("Navigation Screen: {}", navigationScreen);

			log.info("Sending push notification to DoctorId: {}", dto.getDoctorId());

			appNotification.sendPushNotification(token, title, body, "DOCTOR_APPOINTMENT", "Doctor Appointment ",
					"default", navigationScreen);

			log.info("Push notification sent successfully for BookingId: {}", dto.getBookingId());

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

			log.info("Notification details saved successfully. BookingId: {}", dto.getBookingId());

			res.setMessage("Doctor Notification Sent Successfully");
			res.setStatus(200);
			res.setSuccess(true);

		} catch (Exception e) {

			log.error("Exception occurred while sending doctor notification for BookingId: {}", dto.getBookingId(), e);

			res.setMessage(e.getMessage());
			res.setStatus(500);
			res.setSuccess(false);
		}

		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@Override
	public ResponseEntity<?> sendDoctorRatingNotification(DoctorRatingNotificationDTO dto) {

		Response response = new Response();

		try {

			String token = clinicFeign.getDoctorDeviceId(dto.getDoctorId());

			if (token == null || token.isBlank()) {

				response.setSuccess(false);
				response.setStatus(404);
				response.setMessage("Doctor FCM Token Not Found");

				return ResponseEntity.status(404).body(response);
			}

			String title = "New Patient Rating";

			String body = "You received a new rating.\n\n" + "Patient: " + dto.getPatientName() + "\nRating: "
					+ dto.getRating() + "\nFeedback: " + dto.getFeedback();

			appNotification.sendPushNotification(token, title, body, "DOCTOR_RATING", "Doctor Rating", "default",
					"feedback");

			response.setSuccess(true);
			response.setStatus(200);
			response.setMessage("Doctor Rating Notification Sent");

		} catch (Exception e) {

			response.setSuccess(false);
			response.setStatus(500);
			response.setMessage(e.getMessage());
		}

		return ResponseEntity.status(response.getStatus()).body(response);
	}
}