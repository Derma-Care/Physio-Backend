package physiotherapydoctor.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import feign.FeignException;
import physiotherapydoctor.dto.NotificationDTO;
import physiotherapydoctor.dto.ResBody;
import physiotherapydoctor.service.NotificationService;
import physiotherapydoctor.util.ExtractFeignMessage;
import physiotherapydoctor.util.NotificationFeignImpl;

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
