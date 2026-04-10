package com.dermacare.doctorservice.serviceimpl;



import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;


import com.dermacare.doctorservice.dto.ExtractFeignMessage;
import com.dermacare.doctorservice.dto.NotificationDTO;
import com.dermacare.doctorservice.dto.NotificationResponse;
import com.dermacare.doctorservice.dto.ResBody;
import com.dermacare.doctorservice.feignclient.NotificationFeign;
import com.dermacare.doctorservice.service.NotificationService;

import feign.FeignException;

@Service
public class NotificationServiceImpl implements NotificationService {
	
	@Autowired
	private NotificationFeign notificationFeign;
		    

	public ResponseEntity<ResBody<List<NotificationDTO>>> notificationToDoctor(String hospitalId,
			 String doctorId){
		try {
			//System.out.println(jwtToken);
			//System.out.println(expireTime);
		return notificationFeign.notificationtodoctor(hospitalId, doctorId);
		}catch(FeignException e) {
			ResBody<List<NotificationDTO>> res = new ResBody<List<NotificationDTO>>(ExtractFeignMessage.clearMessage(e),e.status(),null);
			return ResponseEntity.status(e.status()).body(res);}
		}
		
}