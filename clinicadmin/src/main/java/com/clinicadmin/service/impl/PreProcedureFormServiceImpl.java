package com.clinicadmin.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.PreProcedureFormDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.SubServicesDto;
import com.clinicadmin.entity.PreProcedureForm;
import com.clinicadmin.feignclient.ServiceFeignClient;
import com.clinicadmin.repository.PreProcedureFormRepository;
import com.clinicadmin.service.PreProcedureFormService;

@Service
public class PreProcedureFormServiceImpl implements PreProcedureFormService {
	@Autowired
	ServiceFeignClient serviceFeignClient;

	@Autowired
	PreProcedureFormRepository preProcedureFormRepository;

	@Override
	public Response addPreProcedureForm(String hospitalId, String subServiceId,
			PreProcedureFormDTO preProcedureFormDTO) {
		Response response = new Response();
		try {

			ResponseEntity<ResponseStructure<SubServicesDto>> resFromSubService = serviceFeignClient
					.getSubServiceByServiceId(hospitalId, subServiceId);
			ResponseStructure<SubServicesDto> subService = resFromSubService.getBody();
			SubServicesDto subDTO = subService.getData();
			PreProcedureForm preProcedureFormData = new PreProcedureForm();
			preProcedureFormData.setHospitalId(hospitalId);
			preProcedureFormData.setCategoryId(subDTO.getCategoryId());
			preProcedureFormData.setCategoryName(subDTO.getCategoryName());
			preProcedureFormData.setServiceId(subDTO.getServiceId());
			preProcedureFormData.setServiceName(subDTO.getServiceName());
			preProcedureFormData.setSubServiceId(subServiceId);
			preProcedureFormData.setSubServiceName(subDTO.getSubServiceName());
			preProcedureFormData.setPreProcedureName(preProcedureFormDTO.getPreProcedureName());
			preProcedureFormData.setTotalDuration(preProcedureFormDTO.getTotalDuration());
			preProcedureFormData.setPreProcedureDetails(preProcedureFormDTO.getPreProcedureDetails());

			PreProcedureForm savedFormData = preProcedureFormRepository.save(preProcedureFormData);
			PreProcedureFormDTO formDTO = new PreProcedureFormDTO();
			formDTO.setId(savedFormData.getId().toString());
			formDTO.setHospitalId(savedFormData.getHospitalId());
			formDTO.setCategoryId(savedFormData.getCategoryId());
			formDTO.setCategoryName(savedFormData.getServiceId());
			formDTO.setServiceId(savedFormData.getServiceId());
			formDTO.setServiceName(savedFormData.getServiceName());
			formDTO.setSubServiceId(savedFormData.getSubServiceId());
			formDTO.setSubServiceName(savedFormData.getSubServiceName());
			formDTO.setPreProcedureName(savedFormData.getPreProcedureName());
			formDTO.setTotalDuration(savedFormData.getTotalDuration());
			formDTO.setPreProcedureDetails(savedFormData.getPreProcedureDetails());

			response.setSuccess(true);
			response.setData(formDTO);
			response.setMessage(" PreProcedure added successfully");
			response.setStatus(HttpStatus.OK.value());
			return response;
		} catch (Exception e) {

			response.setSuccess(false);

			response.setMessage("Exception occured during the add pre-procedure" + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return response;
		}

	}

	@Override
	public Response getPreProcedureFormBypreProcedureFormId(String hospitalId, String preProcedureFormId) {
	    Response response = new Response();

	    if (hospitalId == null || hospitalId.isEmpty() || preProcedureFormId == null || preProcedureFormId.isEmpty()) {
	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage("Hospital ID or PreProcedureForm ID cannot be null or empty");
	        response.setStatus(HttpStatus.BAD_REQUEST.value());
	        return response;
	    }

	    try {
	        Optional<PreProcedureForm> dbFormData = preProcedureFormRepository
	                .findByHospitalIdAndId(hospitalId, new ObjectId(preProcedureFormId));

	        if (dbFormData.isPresent()) {
	            PreProcedureForm savedData = dbFormData.get();

	            PreProcedureFormDTO formDTO = new PreProcedureFormDTO();
	            formDTO.setId(savedData.getId() != null ? savedData.getId().toString() : null);
	            formDTO.setHospitalId(savedData.getHospitalId());
	            formDTO.setCategoryId(savedData.getCategoryId());
	            formDTO.setCategoryName(savedData.getCategoryName()); // Corrected from setServiceId
	            formDTO.setServiceId(savedData.getServiceId());
	            formDTO.setServiceName(savedData.getServiceName());
	            formDTO.setSubServiceId(savedData.getSubServiceId());
	            formDTO.setSubServiceName(savedData.getSubServiceName());
	            formDTO.setPreProcedureName(savedData.getPreProcedureName());
	            formDTO.setTotalDuration(savedData.getTotalDuration());
	            formDTO.setPreProcedureDetails(savedData.getPreProcedureDetails());

	            response.setSuccess(true);
	            response.setData(formDTO);
	            response.setMessage("PreProcedure form data retrieved successfully");
	            response.setStatus(HttpStatus.OK.value());
	        } else {
	            response.setSuccess(true);
	            response.setData(Collections.emptyList());
	            response.setMessage("PreProcedure form data not found with ID: " + preProcedureFormId);
	            response.setStatus(HttpStatus.OK.value());
	        }
	    } catch (IllegalArgumentException e) {
	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage("Invalid PreProcedureForm ID format: " + e.getMessage());
	        response.setStatus(HttpStatus.BAD_REQUEST.value());
	    } catch (Exception e) {
	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage("Exception occurred while retrieving data: " + e.getMessage());
	        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
	    }

	    return response;
	}

@Override
	public Response getAllPreProcedureForm() {
		Response response = new Response();
		try {

			List<PreProcedureForm> dataList = preProcedureFormRepository.findAll();
			if (!dataList.isEmpty()) {
				List<PreProcedureFormDTO> dtoList = dataList.stream()
						.map(form -> new PreProcedureFormDTO(form.getId().toString(),form.getHospitalId(), form.getCategoryId(),
								form.getCategoryName(), form.getServiceId(), form.getServiceName(),
								form.getSubServiceId(), form.getSubServiceName(), form.getPreProcedureName(),
								form.getTotalDuration(), form.getPreProcedureDetails()))
						.collect(Collectors.toList());
				response.setSuccess(true);
				response.setData(dtoList);
				response.setMessage("Pre Procedure forms retrive successfully");
				response.setStatus(HttpStatus.OK.value());
				return response;
			} else {
				response.setSuccess(true);
				response.setData(Collections.emptyList());
				response.setMessage("Pre Procedure data has empty list");
				response.setStatus(HttpStatus.OK.value());
				return response;
			}

		} catch (Exception e) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Exception occured during get all Pre Procedure data :" + e.getMessage());
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return response;
		}
	}
@Override
public Response updatePreProcedureForm(String hospitalId, String preProcedureFormId, PreProcedureFormDTO dto) {
	Response response = new Response();

	try {
		Optional<PreProcedureForm> optionalForm = preProcedureFormRepository
				.findByHospitalIdAndId(hospitalId, new ObjectId(preProcedureFormId));

		if (!optionalForm.isPresent()) {
			response.setSuccess(false);
			response.setMessage("PreProcedureForm not found for ID: " + preProcedureFormId);
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return response;
		}

		PreProcedureForm form = optionalForm.get();

		// Only update if DTO fields are not null
		if (dto.getPreProcedureName() != null)
			form.setPreProcedureName(dto.getPreProcedureName());

		if (dto.getTotalDuration() != null)
			form.setTotalDuration(dto.getTotalDuration());

		if (dto.getPreProcedureDetails() != null)
			form.setPreProcedureDetails(dto.getPreProcedureDetails());

		// Save updated form
		PreProcedureForm updatedForm = preProcedureFormRepository.save(form);

		// Convert to DTO
		PreProcedureFormDTO updatedDTO = new PreProcedureFormDTO(
				updatedForm.getId().toString(),
				updatedForm.getHospitalId(),
				updatedForm.getCategoryId(),
				updatedForm.getCategoryName(),
				updatedForm.getServiceId(),
				updatedForm.getServiceName(),
				updatedForm.getSubServiceId(),
				updatedForm.getSubServiceName(),
				updatedForm.getPreProcedureName(),
				updatedForm.getTotalDuration(),
				updatedForm.getPreProcedureDetails()
		);

		response.setSuccess(true);
		response.setData(updatedDTO);
		response.setMessage("PreProcedureForm updated successfully");
		response.setStatus(HttpStatus.OK.value());
		return response;

	} catch (Exception e) {
		response.setSuccess(false);
		response.setMessage("Exception occurred while updating: " + e.getMessage());
		response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		return response;
	}
}
@Override
public Response deletePreProcedureForm(String hospitalId, String preProcedureFormId) {
	Response response = new Response();
	try {
		Optional<PreProcedureForm> optionalForm = preProcedureFormRepository
				.findByHospitalIdAndId(hospitalId, new ObjectId(preProcedureFormId));

		if (!optionalForm.isPresent()) {
			response.setSuccess(false);
			response.setMessage("PreProcedureForm not found for ID: " + preProcedureFormId);
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return response;
		}

		preProcedureFormRepository.deleteById(optionalForm.get().getId());

		response.setSuccess(true);
		response.setMessage("PreProcedureForm deleted successfully.");
		response.setStatus(HttpStatus.OK.value());
		return response;

	} catch (Exception e) {
		response.setSuccess(false);
		response.setMessage("Exception occurred while deleting: " + e.getMessage());
		response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		return response;
	}
}

}
