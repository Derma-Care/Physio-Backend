package com.clinicadmin.service;

import com.clinicadmin.dto.ProcedureFormDTO;
import com.clinicadmin.dto.Response;

public interface ProcedureFormService {

	Response addProcedureForm(String hospitalId, String subServiceId, ProcedureFormDTO dto);

	Response getProcedureById(String hospitalId, String procedureId);

	Response getAllProcedureForms();

	Response updateProcedureForm(String hospitalId, String procedureId, ProcedureFormDTO dto);

	Response deleteProcedureForm(String hospitalId, String procedureId);

}
