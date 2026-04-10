package com.pharmacyManagement.service;

import com.pharmacyManagement.dto.AreaDTO;
import com.pharmacyManagement.dto.Response;

public interface AreaService {

    Response addArea(AreaDTO dto);

    Response updateArea(String id, AreaDTO dto);

    Response getAreaById(String id);

    Response getAllAreas();

    Response deleteArea(String id);

	Response getAreasByCityId(String cityId);
}
