package physiotherapydoctor.service;

import physiotherapydoctor.dto.PhysiotherapyRecordTemplateRequest;
import physiotherapydoctor.dto.Response;

public interface PhysiotherapyRecordTemplateService {

	Response create(PhysiotherapyRecordTemplateRequest request);

	Response getById(String templateRecordId);

	Response getAll();

	Response update(String templateRecordId, PhysiotherapyRecordTemplateRequest request);

	Response delete(String templateRecordId);
}
