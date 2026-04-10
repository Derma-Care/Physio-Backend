package com.AdminService.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.AdminService.dto.VedioCallDTO;
import com.AdminService.entity.VedioCall;
import com.AdminService.repository.VedioCallRepository;
import com.AdminService.util.Response;

@Service
public class VedioCallServiceImpl implements VedioCallService {
	@Autowired
	private VedioCallRepository vedioCallRepository;

//	-------------------------Adding vedio call Id's----------------------------------------------
	@Override
	public Response addVedioCallCredential(VedioCallDTO dto) {
		// Clear existing credentials
		vedioCallRepository.deleteAll();

		// Save new credential
		VedioCall call = new VedioCall();
		call.setAppId(dto.getAppId());
		call.setSignId(dto.getSignId());

		VedioCall savedData = vedioCallRepository.save(call);

		VedioCallDTO savedDto = new VedioCallDTO();
		savedDto.setId(savedData.getId().toString());
		savedDto.setAppId(savedData.getAppId());
		savedDto.setSignId(savedData.getSignId());

		Response response = new Response();
		response.setData(savedDto);
		response.setSuccess(true);
		response.setStatus(HttpStatus.OK.value());
		response.setMessage("Credential added successfully (previous removed)");
		return response;
	}

//	-------------------------Retrieve vedio call Id's----------------------------------------------

	@Override
	public Response getCredentials() {
		List<VedioCall> vedioIds = vedioCallRepository.findAll();
		Response response = new Response();

		if (vedioIds.isEmpty()) {
			response.setStatus(HttpStatus.OK.value());
			response.setSuccess(true);
			response.setMessage("No credentials available");
			return response;
		}

		VedioCall latest = vedioIds.get(0); // only one will be present

		VedioCallDTO dto = new VedioCallDTO();
		dto.setId(latest.getId().toString());
		dto.setAppId(latest.getAppId());
		dto.setSignId(latest.getSignId());

		response.setSuccess(true);
		response.setData(dto);
		response.setMessage("Credential retrieved successfully");
		response.setStatus(HttpStatus.OK.value());
		return response;
	}

}