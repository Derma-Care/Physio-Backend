package com.chiselon.adminservice.service;

import com.chiselon.adminservice.dto.StoreUpdatedQAInClinicsDTO;
import com.chiselon.adminservice.util.Response;

public interface StoreUpdatedQAInClinicsService {

	Response updateQaAndAnswers(String storeUpdatedQAInClinicsDTO, StoreUpdatedQAInClinicsDTO dto);

	Response getById(String id);

	Response getAll();

	Response deleteById(String id);

	Response saveQaAndAnswers(StoreUpdatedQAInClinicsDTO StoreUpdatedQAInClinicsDTO);

}
