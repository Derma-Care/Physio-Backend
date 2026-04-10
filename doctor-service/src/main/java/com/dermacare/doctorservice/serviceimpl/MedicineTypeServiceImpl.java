package com.dermacare.doctorservice.serviceimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dermacare.doctorservice.dto.MedicineTypeDTO;
import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.model.MedicineType;
import com.dermacare.doctorservice.repository.MedicineTypeRepository;
import com.dermacare.doctorservice.service.MedicineTypeService;

@Service
public class MedicineTypeServiceImpl implements MedicineTypeService {

    @Autowired
    private MedicineTypeRepository repository;

    @Override
    public Response addMedicineType(MedicineTypeDTO dto) {

        MedicineType entity;

        if (dto.getId() != null) {
            entity = repository.findById(dto.getId()).orElse(new MedicineType());
        } else {
            entity = new MedicineType();
            entity.setMedicineTypes(new ArrayList<>());
        }

        if (entity.getMedicineTypes() == null) {
            entity.setMedicineTypes(new ArrayList<>());
        }

        for (String type : dto.getMedicineTypes()) {
            if (!entity.getMedicineTypes().contains(type)) {
                entity.getMedicineTypes().add(type);
            }
        }

        MedicineType saved = repository.save(entity);

        return Response.builder()
                .success(true)
                .status(201)
                .message("Medicine types updated successfully")
                .data(toDTO(saved))
                .build();
    }

    @Override
    public Response getMedicineTypesById(String id) {

        Optional<MedicineType> entity = repository.findById(id);

        if (entity.isEmpty()) {
            return Response.builder()
                    .success(false)
                    .status(404)
                    .message("Medicine types not found")
                    .data(null)
                    .build();
        }

        return Response.builder()
                .success(true)
                .status(200)
                .message("Fetched medicine types successfully")
                .data(toDTO(entity.get()))
                .build();
    }

    @Override
    public Response searchOrAddMedicineType(MedicineTypeDTO dto) {

        // get first document if exists
        List<MedicineType> list = repository.findAll();

        MedicineType entity;

        if (list.isEmpty()) {
            entity = new MedicineType();
            entity.setMedicineTypes(new ArrayList<>());
        } else {
            entity = list.get(0); // assuming only one document
        }

        List<String> added = new ArrayList<>();

        for (String type : dto.getMedicineTypes()) {

            if (!entity.getMedicineTypes().contains(type)) {
                entity.getMedicineTypes().add(type);
                added.add(type);
            }
        }

        MedicineType saved = repository.save(entity);

        String msg = added.isEmpty()
                ? "Medicine types already exist"
                : "New medicine types added: " + added;

        return Response.builder()
                .success(true)
                .status(200)
                .message(msg)
                .data(toDTO(saved))
                .build();
    }
    private MedicineTypeDTO toDTO(MedicineType entity) {

        MedicineTypeDTO dto = new MedicineTypeDTO();
        dto.setId(entity.getId());
        dto.setMedicineTypes(entity.getMedicineTypes());

        return dto;
    }
    
    @Override
    public Response getAllMedicineTypes() {

        Response response = new Response();

        try {

            List<MedicineType> list = repository.findAll();

            if (list.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("No medicine types found");
                response.setStatus(404);
            } else {
                response.setSuccess(true);
                response.setData(list);
                response.setMessage("Medicine types fetched successfully");
                response.setStatus(200);
            }

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error fetching medicine types");
            response.setStatus(500);
        }

        return response;
    }
}