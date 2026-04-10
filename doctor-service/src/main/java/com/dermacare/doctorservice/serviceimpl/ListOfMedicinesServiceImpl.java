package com.dermacare.doctorservice.serviceimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.dermacare.doctorservice.dto.ListOfMedicinesDTO;
import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.model.ListOfMedicines;
import com.dermacare.doctorservice.repository.ListOfMedicinesRepository;
import com.dermacare.doctorservice.service.ListOfMedicinesService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListOfMedicinesServiceImpl implements ListOfMedicinesService {

    private final ListOfMedicinesRepository repository;

    // ✅ Create
    @Override
    public Response create(ListOfMedicinesDTO dto) {
        ListOfMedicines saved = repository.save(convertToEntity(dto));
        return new Response(true, convertToDTO(saved), "Medicine list created successfully", HttpStatus.CREATED.value());
    }

    // ✅ Update
    @Override
    public Response update(String id, ListOfMedicinesDTO dto) {
        ListOfMedicines existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine list not found with id: " + id));

        existing.setClinicId(dto.getClinicId());
        existing.setListOfMedicines(dto.getListOfMedicines());
        ListOfMedicines updated = repository.save(existing);

        return new Response(true, convertToDTO(updated), "Medicine list updated successfully", HttpStatus.OK.value());
    }

    // ✅ Delete
    @Override
    public Response delete(String id) {
        repository.deleteById(id);
        return new Response(true, null, "Medicine list deleted successfully", HttpStatus.OK.value());
    }

    // ✅ Get by ID
    @Override
    public Response getById(String id) {
        return repository.findById(id)
                .map(entity -> new Response(true, convertToDTO(entity), "Medicine list fetched successfully", HttpStatus.OK.value()))
                .orElse(new Response(false, null, "Medicine list not found with id: " + id, HttpStatus.NOT_FOUND.value()));
    }

    // ✅ Get all
    @Override
    public Response getAll() {
        List<ListOfMedicinesDTO> medicines = repository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new Response(true, medicines, "All medicine lists fetched successfully", HttpStatus.OK.value());
    }

    // ✅ Get by clinic ID
    @Override
    public Response getByClinicId(String clinicId) {
        List<ListOfMedicinesDTO> medicines = repository.findByClinicId(clinicId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new Response(true, medicines, "Medicine lists fetched for clinicId: " + clinicId, HttpStatus.OK.value());
    }

    // ✅ Add or search medicine (using only ListOfMedicinesDTO)
    @Override
    public Response addOrSearchMedicine(ListOfMedicinesDTO dto) {
        String clinicId = dto.getClinicId();
        List<String> medicinesToAdd = dto.getListOfMedicines();

        if (clinicId == null || clinicId.isEmpty() || medicinesToAdd == null || medicinesToAdd.isEmpty()) {
            return new Response(false, null, "clinicId and listOfMedicines are required", HttpStatus.BAD_REQUEST.value());
        }

        // Find existing list for clinic
        List<ListOfMedicines> lists = repository.findByClinicId(clinicId);
        ListOfMedicines targetList;

        if (lists.isEmpty()) {
            // No list → create new
            targetList = new ListOfMedicines();
            targetList.setClinicId(clinicId);
            targetList.setListOfMedicines(new ArrayList<>(medicinesToAdd));
        } else {
            // Use first list
            targetList = lists.get(0);
            if (targetList.getListOfMedicines() == null) {
                targetList.setListOfMedicines(new ArrayList<>());
            }
            for (String med : medicinesToAdd) {
                if (!targetList.getListOfMedicines().contains(med)) {
                    targetList.getListOfMedicines().add(med);
                }
            }
        }

        ListOfMedicines saved = repository.save(targetList);
        return new Response(true, convertToDTO(saved), "Medicine list updated/added successfully", HttpStatus.CREATED.value());
    }

    // ✅ Helper methods for conversion
    private ListOfMedicinesDTO convertToDTO(ListOfMedicines entity) {
        return new ListOfMedicinesDTO(entity.getId(), entity.getClinicId(), entity.getListOfMedicines());
    }

    private ListOfMedicines convertToEntity(ListOfMedicinesDTO dto) {
        return new ListOfMedicines(dto.getId(), dto.getClinicId(), dto.getListOfMedicines());
    }
}