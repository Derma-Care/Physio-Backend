package physiotherapydoctor.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.TherapistResponseDTO;
import physiotherapydoctor.feign.ClinicAdminFeign;
import physiotherapydoctor.service.PhysiotherapyDoctorDetails;

@Service
public class PhysiotherapyDoctorDetailsImpl implements PhysiotherapyDoctorDetails {

	@Autowired
	private ClinicAdminFeign clinicAdminFeign;

	@Override
	public Response getPhysioDoctorDetails(String clinicId, String branchId) {
		ResponseEntity<Response> clinicdata = clinicAdminFeign.getTherapistWithRequiredFileds(clinicId, branchId);
		Object obj = clinicdata.getBody().getData();
		ObjectMapper mapper = new ObjectMapper();
		List<TherapistResponseDTO> dto = mapper.convertValue(obj, new TypeReference<List<TherapistResponseDTO>>() {
		});

		Response response = new Response();
		response.setSuccess(true);
		response.setData(dto);
		response.setMessage("Successfully fetched therapist details");
		response.setStatus(HttpStatus.OK.value());

		return response;

	}

}
