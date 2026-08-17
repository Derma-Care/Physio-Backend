package com.chiselon.physiotherapydoctor.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import feign.FeignException;
import com.chiselon.physiotherapydoctor.dto.NotificationDTO;
import com.chiselon.physiotherapydoctor.dto.ResBody;
import com.chiselon.physiotherapydoctor.service.NotificationService;
import com.chiselon.physiotherapydoctor.util.ExtractFeignMessage;
import com.chiselon.physiotherapydoctor.util.NotificationFeignImpl;

@Service
public class NotificationServiceImpl implements NotificationService {

	@Autowired
	private NotificationFeignImpl notificationFeign;

	public ResponseEntity<ResBody<List<NotificationDTO>>> notificationToDoctor(String hospitalId, String doctorId) {
		try {
			return notificationFeign.notificationtodoctor(hospitalId, doctorId);
		} catch (FeignException e) {
			ResBody<List<NotificationDTO>> res = new ResBody<List<NotificationDTO>>(ExtractFeignMessage.clearMessage(e),
					e.status(), null);
			return ResponseEntity.status(e.status()).body(res);
		}
	}

}
