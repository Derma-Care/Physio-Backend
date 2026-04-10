package com.pharmacyManagement.service;

import com.pharmacyManagement.dto.CityDTO;
import com.pharmacyManagement.dto.Response;

public interface CityService {

    Response addCity(CityDTO dto);

    Response getAllCities();

    Response getCityById(String id);

    Response updateCity(String id, CityDTO dto);

    Response deleteCity(String id);
}
