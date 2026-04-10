package com.dermaCare.customerService.service;

public interface FirebaseMessagingService {
	
public void sendPushNotification(String deviceToken, String title, String body, String type, String screen, String sound);

}
