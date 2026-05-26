package physiotherapydoctor.service;

import physiotherapydoctor.dto.ListOfMedicinesDTO;
import physiotherapydoctor.dto.Response;

public interface ListOfMedicinesService {

	Response create(ListOfMedicinesDTO dto);

	Response update(String id, ListOfMedicinesDTO dto);

	Response delete(String id);

	Response getById(String id);

	Response getAll();

	Response getByClinicId(String clinicId);

	Response addOrSearchMedicine(ListOfMedicinesDTO dto);
}
