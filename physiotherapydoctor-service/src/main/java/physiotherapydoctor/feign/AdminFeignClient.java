package physiotherapydoctor.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import physiotherapydoctor.dto.BranchDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;

@FeignClient(name = "adminservice")

public interface AdminFeignClient {

	@GetMapping("/admin/getClinicById/{clinicId}")
	ResponseEntity<Response> getClinicById(@PathVariable("clinicId") String clinicId);
	
	 @GetMapping("/admin/getBranchById/{branchId}")
	    ResponseEntity<ResponseStructure<BranchDTO>> getBranchById(
	            @PathVariable("branchId") String branchId);
}
