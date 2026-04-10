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

import com.clinicadmin.dto.PostProcedureFormDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.SubServicesDto;
import com.clinicadmin.entity.PostProcedureForm;
import com.clinicadmin.feignclient.ServiceFeignClient;
import com.clinicadmin.repository.PostProcedureFormRepository;
import com.clinicadmin.service.PostProcedureFormService;

@Service
public class PostProcedureFormServiceImpl implements PostProcedureFormService {

    @Autowired
    PostProcedureFormRepository postProcedureFormRepository;

    @Autowired
    ServiceFeignClient serviceFeignClient;

    @Override
    public Response addPostProcedureForm(String hospitalId, String subServiceId, PostProcedureFormDTO dto) {
        Response response = new Response();
        try {
            ResponseEntity<ResponseStructure<SubServicesDto>> subServiceResponse =
                    serviceFeignClient.getSubServiceByServiceId(hospitalId, subServiceId);
            ResponseStructure<SubServicesDto> dbData =  subServiceResponse.getBody();
            SubServicesDto subDTO =   dbData.getData();

            PostProcedureForm form = new PostProcedureForm();
            form.setHospitalId(hospitalId);
            form.setSubServiceId(subServiceId);
            form.setSubServiceName(subDTO.getSubServiceName());
            form.setServiceId(subDTO.getServiceId());
            form.setServiceName(subDTO.getServiceName());
            form.setCategoryId(subDTO.getCategoryId());
            form.setCategoryName(subDTO.getCategoryName());
            form.setPostProcedureName(dto.getPostProcedureName());
            form.setTotalDuration(dto.getTotalDuration());
            form.setPostProcedureDetails(dto.getPostProcedureDetails());

            PostProcedureForm savedFormData = postProcedureFormRepository.save(form);
            PostProcedureFormDTO formDTO = new PostProcedureFormDTO();
			formDTO.setId(savedFormData.getId().toString());
			formDTO.setHospitalId(savedFormData.getHospitalId());
			formDTO.setCategoryId(savedFormData.getCategoryId());
			formDTO.setCategoryName(savedFormData.getCategoryName());
			formDTO.setServiceId(savedFormData.getServiceId());
			formDTO.setServiceName(savedFormData.getServiceName());
			formDTO.setSubServiceId(savedFormData.getSubServiceId());
			formDTO.setSubServiceName(savedFormData.getSubServiceName());
			formDTO.setPostProcedureName(savedFormData.getPostProcedureName());
			formDTO.setTotalDuration(savedFormData.getTotalDuration());
			formDTO.setPostProcedureDetails(savedFormData.getPostProcedureDetails());
            

            response.setSuccess(true);
            response.setData(formDTO);
            response.setMessage("PostProcedureForm added successfully");
            response.setStatus(HttpStatus.OK.value());
            return response;

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Exception while adding post-procedure: " + e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return response;
        }
    }

    @Override
    public Response getPostProcedureFormById(String hospitalId, String postProcedureFormId) {
        Response response = new Response();
        try {
            Optional<PostProcedureForm> optional = postProcedureFormRepository
                    .findByHospitalIdAndId(hospitalId, new ObjectId(postProcedureFormId));

            if (optional.isPresent()) {
                PostProcedureForm form = optional.get();
                PostProcedureFormDTO dto = new PostProcedureFormDTO(
                        form.getId().toString(), form.getHospitalId(), form.getSubServiceId(),
                        form.getSubServiceName(), form.getServiceId(), form.getServiceName(),
                        form.getCategoryId(), form.getCategoryName(), form.getPostProcedureName(),
                        form.getTotalDuration(), form.getPostProcedureDetails()
                );

                response.setSuccess(true);
                response.setData(dto);
                response.setMessage("PostProcedureForm retrieved successfully");
                response.setStatus(HttpStatus.OK.value());
                return response;
            } else {
                response.setSuccess(true);
                response.setData(Collections.emptyList());
                response.setMessage("No PostProcedureForm found with ID: " + postProcedureFormId);
                response.setStatus(HttpStatus.OK.value());
                return response;
            }

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error fetching PostProcedureForm: " + e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return response;
        }
    }

    @Override
    public Response getAllPostProcedureForms() {
        Response response = new Response();
        try {
            List<PostProcedureForm> forms = postProcedureFormRepository.findAll();

            if (!forms.isEmpty()) {
                List<PostProcedureFormDTO> dtoList = forms.stream().map(form -> new PostProcedureFormDTO(
                        form.getId().toString(), form.getHospitalId(), form.getSubServiceId(),
                        form.getSubServiceName(), form.getServiceId(), form.getServiceName(),
                        form.getCategoryId(), form.getCategoryName(), form.getPostProcedureName(),
                        form.getTotalDuration(), form.getPostProcedureDetails()
                )).collect(Collectors.toList());

                response.setSuccess(true);
                response.setData(dtoList);
                response.setMessage("PostProcedureForms retrieved successfully");
                response.setStatus(HttpStatus.OK.value());
                return response;
            } else {
                response.setSuccess(true);
                response.setData(Collections.emptyList());
                response.setMessage("No PostProcedureForms found");
                response.setStatus(HttpStatus.OK.value());
                return response;
            }

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Exception while retrieving all post-procedure forms: " + e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return response;
        }
    }

    @Override
    public Response updatePostProcedureForm(String hospitalId, String postProcedureFormId, PostProcedureFormDTO dto) {
        Response response = new Response();
        try {
            Optional<PostProcedureForm> optional = postProcedureFormRepository
                    .findByHospitalIdAndId(hospitalId, new ObjectId(postProcedureFormId));

            if (!optional.isPresent()) {
                response.setSuccess(false);
                response.setMessage("PostProcedureForm not found for ID: " + postProcedureFormId);
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return response;
            }

            PostProcedureForm form = optional.get();

            if (dto.getPostProcedureName() != null)
                form.setPostProcedureName(dto.getPostProcedureName());

            if (dto.getTotalDuration() != null)
                form.setTotalDuration(dto.getTotalDuration());

            if (dto.getPostProcedureDetails() != null)
                form.setPostProcedureDetails(dto.getPostProcedureDetails());

            PostProcedureForm updated = postProcedureFormRepository.save(form);

            PostProcedureFormDTO updatedDTO = new PostProcedureFormDTO(
                    updated.getId().toString(), updated.getHospitalId(), updated.getSubServiceId(),
                    updated.getSubServiceName(), updated.getServiceId(), updated.getServiceName(),
                    updated.getCategoryId(), updated.getCategoryName(), updated.getPostProcedureName(),
                    updated.getTotalDuration(), updated.getPostProcedureDetails()
            );

            response.setSuccess(true);
            response.setData(updatedDTO);
            response.setMessage("PostProcedureForm updated successfully");
            response.setStatus(HttpStatus.OK.value());
            return response;

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Exception while updating PostProcedureForm: " + e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return response;
        }
    }

    @Override
    public Response deletePostProcedureForm(String hospitalId, String postProcedureFormId) {
        Response response = new Response();
        try {
            Optional<PostProcedureForm> optional = postProcedureFormRepository
                    .findByHospitalIdAndId(hospitalId, new ObjectId(postProcedureFormId));

            if (!optional.isPresent()) {
                response.setSuccess(false);
                response.setMessage("PostProcedureForm not found for ID: " + postProcedureFormId);
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return response;
            }

            postProcedureFormRepository.deleteById(optional.get().getId());

            response.setSuccess(true);
            response.setMessage("PostProcedureForm deleted successfully.");
            response.setStatus(HttpStatus.OK.value());
            return response;

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Exception while deleting PostProcedureForm: " + e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return response;
        }
    }
}

