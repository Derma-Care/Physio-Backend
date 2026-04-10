package com.dermaCare.customerService.service;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service
public class FirebaseMessagingServiceImpl implements FirebaseMessagingService  {
	
	public void sendPushNotification(String deviceToken, String title, String body, String type, String screen, String sound) {
	    Message message = Message.builder()
	        .setToken(deviceToken)
	        .setNotification(Notification.builder()
	            .setTitle(title)
	            .setBody(body)
	            .build())
	        .putData("type", type)
	        .putData("screen", screen)
	        .putData("sound", sound) // Custom key, handled by app
	        .build();

	    try {
	        String response = FirebaseMessaging.getInstance().send(message);
	        System.out.println("Successfully sent message: " + response);
	    } catch (FirebaseMessagingException e) {
	        e.printStackTrace();
	    }
	}
}