package com.chiselon.notificationservice.service.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.chiselon.notificationservice.dto.DoctorPushNotificationDTO;
import com.chiselon.notificationservice.dto.DoctorRatingNotificationDTO;
import com.chiselon.notificationservice.dto.Response;
import com.chiselon.notificationservice.entity.DoctorPushNotification;
import com.chiselon.notificationservice.feign.CllinicFeign;
import com.chiselon.notificationservice.notificationFactory.SendAppNotification;
import com.chiselon.notificationservice.repository.DoctorPushNotificationRepository;
import com.chiselon.notificationservice.service.DoctorPushNotificationService;
import com.chiselon.notificationservice.util.KeyCloakTokenStore;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorPushNotificationServiceImpl implements DoctorPushNotificationService {

	private final DoctorPushNotificationRepository repository;
	private final CllinicFeign clinicFeign;
	private final SendAppNotification appNotification;
	  private final KeyCloakTokenStore KeyCloakTokenStore;

	@Override
	@RateLimiter(name = "notification-service", fallbackMethod = "sendNotificationFallback")
	public ResponseEntity<?> sendNotification(DoctorPushNotificationDTO dto) {

	    long startTime = System.currentTimeMillis();

	    log.info(
	            "Doctor notification request received. BookingId={}, DoctorId={}, AppointmentType={}, PatientName={}",
	            dto.getBookingId(),
	            dto.getDoctorId(),
	            dto.getAppointmentType(),
	            dto.getPatientName());

	    log.debug("Complete request payload: {}", dto);

	    Response res = new Response();

	    try {

	        log.debug(
	                "Checking duplicate notification. BookingId={}, AppointmentType={}",
	                dto.getBookingId(),
	                dto.getAppointmentType());

	        boolean notificationExists =
	                repository.existsByBookingIdAndAppointmentType(
	                        dto.getBookingId(),
	                        dto.getAppointmentType());

	        log.debug("Duplicate notification check result={}", notificationExists);

	        if (notificationExists) {

	            log.warn(
	                    "Notification already exists. BookingId={}, AppointmentType={}",
	                    dto.getBookingId(),
	                    dto.getAppointmentType());

	            res.setMessage("Notification Already Sent");
	            res.setStatus(200);
	            res.setSuccess(true);

	            return ResponseEntity.status(res.getStatus()).body(res);
	        }

	        log.info("Fetching doctor device token. DoctorId={}",
	                dto.getDoctorId());

	        String token = clinicFeign.getDoctorDeviceId(KeyCloakTokenStore.getAccess_token(),dto.getDoctorId());

	        if (token == null || token.isBlank()) {

	            log.error("Doctor FCM token not found. DoctorId={}",
	                    dto.getDoctorId());

	            res.setMessage("Doctor FCM Token Not Found");
	            res.setStatus(404);
	            res.setSuccess(false);

	            return ResponseEntity.status(res.getStatus()).body(res);
	        }

	        log.info("Doctor device token fetched successfully");

	        String title;
	        String body;

	        if ("FOLLOW_UP".equalsIgnoreCase(dto.getAppointmentType())) {

	            log.info("Preparing follow-up appointment notification");

	            title = "Follow-up Appointment Scheduled";

	            body =
	                    "A follow-up appointment has been scheduled.\n\n"
	                            + "Patient: "
	                            + dto.getPatientName()
	                            + "\nDate: "
	                            + dto.getAppointmentDate()
	                            + "\nTime: "
	                            + dto.getAppointmentTime();

	        } else {

	            log.info("Preparing new appointment notification");

	            title = "New Appointment Booked";

	            body =
	                    "A new appointment has been booked.\n\n"
	                            + "Patient: "
	                            + dto.getPatientName()
	                            + "\nDate: "
	                            + dto.getAppointmentDate()
	                            + "\nTime: "
	                            + dto.getAppointmentTime();
	        }

	        log.debug("Notification title prepared: {}", title);

	        DateTimeFormatter formatter =
	                DateTimeFormatter.ofPattern("yyyy-MM-dd");

	        LocalDate appointmentDate =
	                LocalDate.parse(dto.getAppointmentDate(), formatter);

	        LocalDate today = LocalDate.now();

	        String navigationScreen =
	                appointmentDate.isEqual(today)
	                        ? "dashboard"
	                        : "appointments";

	        log.info(
	                "Navigation screen determined. AppointmentDate={}, Today={}, Screen={}",
	                appointmentDate,
	                today,
	                navigationScreen);

	        log.info(
	                "Sending push notification. DoctorId={}, BookingId={}",
	                dto.getDoctorId(),
	                dto.getBookingId());

	        appNotification.sendPushNotification(
	                token,
	                title,
	                body,
	                "DOCTOR_APPOINTMENT",
	                "Doctor Appointment",
	                "default",
	                navigationScreen);

	        log.info(
	                "Push notification sent successfully. BookingId={}",
	                dto.getBookingId());

	        log.debug(
	                "Creating notification audit entity. BookingId={}",
	                dto.getBookingId());

	        DoctorPushNotification notification =
	                new DoctorPushNotification();

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

	        log.info(
	                "Saving notification record. BookingId={}, DoctorId={}",
	                dto.getBookingId(),
	                dto.getDoctorId());

	        DoctorPushNotification savedNotification =
	                repository.save(notification);

	        log.info(
	                "Notification record saved successfully. NotificationId={}, BookingId={}",
	                savedNotification.getId(),
	                dto.getBookingId());

	        res.setMessage("Doctor Notification Sent Successfully");
	        res.setStatus(200);
	        res.setSuccess(true);

	        log.info(
	                "Doctor notification completed successfully. BookingId={}",
	                dto.getBookingId());

	    } catch (Exception e) {

	        log.error(
	                "Error while sending doctor notification. BookingId={}, DoctorId={}, Error={}",
	                dto.getBookingId(),
	                dto.getDoctorId(),
	                e.getMessage(),
	                e);

	        res.setMessage(e.getMessage());
	        res.setStatus(500);
	        res.setSuccess(false);
	    }

	    log.info(
	            "sendNotification completed. BookingId={}, Status={}, ExecutionTime={} ms",
	            dto.getBookingId(),
	            res.getStatus(),
	            (System.currentTimeMillis() - startTime));

	    return ResponseEntity.status(res.getStatus()).body(res);
	}
    
    public ResponseEntity<?> sendNotificationFallback(
            DoctorPushNotificationDTO dto,
            Exception ex) {

        Response res = new Response();
        res.setMessage("Too many requests. Please try again after some time.");
        res.setStatus(429);
        res.setSuccess(false);

        return ResponseEntity.status(429).body(res);
    }

    @Override
    public ResponseEntity<?> sendDoctorRatingNotification(
            DoctorRatingNotificationDTO dto) {

        long startTime = System.currentTimeMillis();

        log.info(
                "Doctor rating notification request received. DoctorId={}, PatientName={}, Rating={}",
                dto.getDoctorId(),
                dto.getPatientName(),
                dto.getRating());

        log.debug("Doctor rating notification payload: {}", dto);

        Response response = new Response();

        try {

            log.info("Fetching doctor device token. DoctorId={}",
                    dto.getDoctorId());

            String token =
                    clinicFeign.getDoctorDeviceId(KeyCloakTokenStore.getAccess_token(),dto.getDoctorId());

            if (token == null || token.isBlank()) {

                log.warn(
                        "Doctor FCM token not found. DoctorId={}",
                        dto.getDoctorId());

                response.setSuccess(false);
                response.setStatus(404);
                response.setMessage("Doctor FCM Token Not Found");

                return ResponseEntity.status(404).body(response);
            }

            log.info(
                    "Doctor device token fetched successfully. DoctorId={}",
                    dto.getDoctorId());

            String title = "New Patient Rating";

            String body =
                    "You received a new rating.\n\n"
                            + "Patient: "
                            + dto.getPatientName()
                            + "\nRating: "
                            + dto.getRating()
                            + "\nReview: "
                            + dto.getFeedback();

            log.debug(
                    "Notification content prepared. DoctorId={}, Rating={}",
                    dto.getDoctorId(),
                    dto.getRating());

            log.info(
                    "Sending doctor rating notification. DoctorId={}",
                    dto.getDoctorId());

            appNotification.sendPushNotification(
                    token,
                    title,
                    body,
                    "DOCTOR_RATING",
                    "Doctor Rating",
                    "default",
                    "feedback");

            log.info(
                    "Doctor rating notification sent successfully. DoctorId={}",
                    dto.getDoctorId());

            response.setSuccess(true);
            response.setStatus(200);
            response.setMessage("Doctor Rating Notification Sent");

            log.info(
                    "Doctor rating notification process completed successfully. DoctorId={}",
                    dto.getDoctorId());

        } catch (Exception e) {

            log.error(
                    "Failed to send doctor rating notification. DoctorId={}, PatientName={}, Error={}",
                    dto.getDoctorId(),
                    dto.getPatientName(),
                    e.getMessage(),
                    e);

            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
        }

        log.info(
                "sendDoctorRatingNotification completed. DoctorId={}, Status={}, ExecutionTime={} ms",
                dto.getDoctorId(),
                response.getStatus(),
                (System.currentTimeMillis() - startTime));

        return ResponseEntity.status(response.getStatus()).body(response);
    }
}