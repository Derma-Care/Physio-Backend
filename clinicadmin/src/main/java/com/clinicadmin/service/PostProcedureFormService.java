package com.clinicadmin.service;

import com.clinicadmin.dto.PostProcedureFormDTO;
import com.clinicadmin.dto.Response;

public interface PostProcedureFormService {

	Response addPostProcedureForm(String hospitalId, String subServiceId, PostProcedureFormDTO dto);

	Response getPostProcedureFormById(String hospitalId, String postProcedureFormId);

	Response getAllPostProcedureForms();

	Response updatePostProcedureForm(String hospitalId, String postProcedureFormId, PostProcedureFormDTO dto);

	Response deletePostProcedureForm(String hospitalId, String postProcedureFormId);

}
