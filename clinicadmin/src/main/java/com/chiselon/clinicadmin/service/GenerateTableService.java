package com.chiselon.clinicadmin.service;

import com.chiselon.clinicadmin.dto.PhysiotherapyRecordDTO;
import com.chiselon.clinicadmin.dto.Response;

public interface GenerateTableService {

    Response generateTable(PhysiotherapyRecordDTO request);
}
