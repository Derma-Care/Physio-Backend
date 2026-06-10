package physiotherapydoctor.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import physiotherapydoctor.dto.Response;

@FeignClient(name = "adminservice")

public interface AdminFeignClient {

	@GetMapping("/admin/getClinicById/{clinicId}")
	ResponseEntity<Response> getClinicById(@PathVariable("clinicId") String clinicId);
}
