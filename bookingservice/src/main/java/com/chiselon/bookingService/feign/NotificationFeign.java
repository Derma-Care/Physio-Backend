package com.chiselon.bookingService.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.chiselon.bookingService.dto.BookingResponse;
import com.chiselon.bookingService.dto.NotificationDTO;
import com.chiselon.bookingService.util.Response;


@FeignClient(value = "notification-service")
public interface NotificationFeign {


    @GetMapping("/api/notificationservice/getNotificationByBookingId/{id}")
    public NotificationDTO getNotificationByBookingId(@PathVariable String id);


    @PutMapping("/api/notificationservice/updateNotification")
    public NotificationDTO updateNotification(@RequestBody NotificationDTO notificationDTO );


    @PostMapping("/api/notificationservice/notifications")
    public ResponseEntity<Response> createNotification(@RequestHeader("Authorization") String token,@RequestBody BookingResponse booking);


}