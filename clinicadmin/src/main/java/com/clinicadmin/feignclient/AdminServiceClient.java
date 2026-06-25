package com.clinicadmin.feignclient;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.clinicadmin.dto.CategoryMediaCarouselDTO;
import com.clinicadmin.dto.ClinicDTO;
import com.clinicadmin.dto.ClinicLoginRequestDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.UpdateClinicLoginCredentialsDTO;
@FeignClient(name = "adminservice")
public interface AdminServiceClient {

	@PostMapping("/admin/clinicLogin/{userName}")
	 public Response clinicLogin(@PathVariable String userName);
	 
	// Update clinic credentials
	@PutMapping("/admin/updateClinicCredentials/{userName}")
	public Response updateClinicCredentials(@RequestHeader("Authorization") String token,@RequestBody UpdateClinicLoginCredentialsDTO updatedCredentials,
			@PathVariable String userName);

	// Get Clinic by ID
	@GetMapping("/admin/getClinicById/{clinicId}")
	 public ResponseEntity<Response> getClinicById(@RequestHeader("Authorization") String token,@PathVariable String clinicId);
	
	 @GetMapping("/admin/getAllClinics")
	    public ResponseEntity<Response> getAllClinics(@RequestHeader("Authorization") String token);

	// Update Clinic
	@PutMapping("/admin/updateClinic/{clinicId}")
	public Response updateClinic(@RequestHeader("Authorization") String token,@PathVariable String clinicId, @RequestBody ClinicDTO clinic);

	// Delete Clinic
	@DeleteMapping("/admin/deleteClinic/{clinicId}")
	public Response deleteClinic(@RequestHeader("Authorization") String token,@PathVariable String clinicId);
	
	@GetMapping("/admin/clinics/recommended")
	public ResponseEntity<Response>getHospitalUsingRecommendentaion(@RequestHeader("Authorization") String token);
	
//	sorted recommended clincs first;
	@GetMapping("/admin/clinics/firstRecommendedTureClincs")
	public ResponseEntity<Response>firstRecommendedTureClincs();
	
	@GetMapping("/admin/getBranchByClinicId/{clinicId}")
	public  ResponseEntity<?> getBranchByClinicId(@RequestHeader("Authorization") String token,@PathVariable String clinicId);
	
	@GetMapping("/admin/getBranchByClinicAndBranchId/{clinicId}/{branchId}")
	public ResponseEntity<Response> getBranchByClinicAndBranchId(@RequestHeader("Authorization") String token,@PathVariable String clinicId,
	                                                      @PathVariable String branchId);
	@GetMapping("/admin/getBranchById/{branchId}")
	public ResponseEntity<Response> getBranchById(@RequestHeader("Authorization") String token,@PathVariable String branchId);
	
	  @GetMapping("/admin/getAllBranches")
	    public ResponseEntity<Response> getAllBranches(@RequestHeader("Authorization") String token);

	 @GetMapping("/admin/getDefaultAdminPermissions")
	    ResponseEntity<Map<String, List<String>>> getDefaultAdminPermissions(@RequestHeader("Authorization") String token);

//	CategoryMediaCarouselDTO
	
    @GetMapping("/admin/categoryAdvertisement/getAll")
    ResponseEntity<Iterable<CategoryMediaCarouselDTO>> getAllMedia();


}