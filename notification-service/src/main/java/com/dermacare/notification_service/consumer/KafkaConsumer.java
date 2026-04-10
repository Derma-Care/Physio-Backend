package com.dermacare.notification_service.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import com.dermacare.notification_service.dto.BookingResponse;
import com.dermacare.notification_service.entity.Booking;
import com.dermacare.notification_service.service.ServiceInterface;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class KafkaConsumer {
	
	
	@Autowired
    private ServiceInterface service;
	@Autowired
	private ObjectMapper mapper;

	@KafkaListener(topics = "bookedServices", groupId = "notificationgroup")
	public void consumeBookingData(String jsonBooking) {
		BookingResponse booking;
		try {
		   booking = mapper.readValue(jsonBooking, BookingResponse.class);
			//System.out.println("Booking received: " + booking);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Unable to Create Notification");
		}
         service.createNotification(booking);
         
	}
}
