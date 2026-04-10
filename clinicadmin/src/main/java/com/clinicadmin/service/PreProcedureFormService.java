package com.clinicadmin.service;

import com.clinicadmin.dto.PreProcedureFormDTO;
import com.clinicadmin.dto.Response;

public interface PreProcedureFormService {

	public Response addPreProcedureForm(String hospitalId, String subServiceId, PreProcedureFormDTO preProcedureFormDTO );
	public Response getPreProcedureFormBypreProcedureFormId(String hospitalId, String preProcedureFormId);
	public Response getAllPreProcedureForm();
	public Response updatePreProcedureForm(String hospitalId, String preProcedureFormId, PreProcedureFormDTO dto);
	public Response deletePreProcedureForm(String hospitalId, String preProcedureFormId);
}
