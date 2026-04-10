package com.pharmacyManagement.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.pharmacyManagement.dto.CityDTO;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.entity.City;
import com.pharmacyManagement.repository.CityRepository;
import com.pharmacyManagement.service.CityService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;

    @Override
    public Response addCity(CityDTO dto) {
        try {

            if (cityRepository.existsByCityNameIgnoreCase(dto.getCityName())) {
                return new Response(false, null, "City already exists", 400, null);
            }

            City city = new City();
            city.setCityName(dto.getCityName());
            City savedCity = cityRepository.save(city);

            CityDTO responseDTO = new CityDTO();
            responseDTO.setId(savedCity.getId());
            responseDTO.setCityName(savedCity.getCityName());

            return new Response(true, responseDTO, "City added successfully", 200,null);

        } catch (Exception e) {
            return new Response(false, null, "Failed to add city: " + e.getMessage(), 500,null);
        }
    }

    @Override
    public Response getAllCities() {
        try {
            List<CityDTO> list = cityRepository.findAll()
                    .stream()
                    .map(c -> {
                        CityDTO dto = new CityDTO();
                        dto.setId(c.getId());
                        dto.setCityName(c.getCityName());
                        return dto;
                    }).collect(Collectors.toList());

            return new Response(true, list, "All cities fetched", 200,null);

        } catch (Exception e) {
            return new Response(false, null, "Failed to fetch cities: " + e.getMessage(), 500,null);
        }
    }

    @Override
    public Response getCityById(String id) {
        try {
            Optional<City> city = cityRepository.findById(id);

            if (city.isEmpty()) {
                return new Response(false, null, "City not found", 404,null);
            }

            CityDTO dto = new CityDTO();
            dto.setId(city.get().getId());
            dto.setCityName(city.get().getCityName());

            return new Response(true, dto, "City found", 200,null);

        } catch (Exception e) {
            return new Response(false, null, "Failed to fetch city: " + e.getMessage(), 500,null);
        }
    }

    @Override
    public Response updateCity(String id, CityDTO dto) {
        try {
            Optional<City> existingCity = cityRepository.findById(id);

            if (existingCity.isEmpty()) {
                return new Response(false, null, "City not found", 404,null);
            }

            City city = existingCity.get();
            city.setCityName(dto.getCityName());
            City updated = cityRepository.save(city);

            CityDTO updatedDTO = new CityDTO();
            updatedDTO.setId(updated.getId());
            updatedDTO.setCityName(updated.getCityName());

            return new Response(true, updatedDTO, "City updated successfully", 200,null);

        } catch (Exception e) {
            return new Response(false, null, "Failed to update city: " + e.getMessage(), 500,null);
        }
    }

    @Override
    public Response deleteCity(String id) {
        try {
            if (!cityRepository.existsById(id)) {
                return new Response(false, null, "City not found", 404,null);
            }

            cityRepository.deleteById(id);
            return new Response(true, null, "City deleted successfully", 200,null);

        } catch (Exception e) {
            return new Response(false, null, "Failed to delete city: " + e.getMessage(), 500,null);
        }
    }
}
