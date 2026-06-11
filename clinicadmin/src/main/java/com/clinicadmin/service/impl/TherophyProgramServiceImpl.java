package com.clinicadmin.service.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.clinicadmin.dto.ProgramWithTherophy;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.TheraphyNamesDTO;
import com.clinicadmin.dto.TheraphyProgramWithTheraphyNamesDto;
import com.clinicadmin.dto.TherapyServiceDTO;
import com.clinicadmin.dto.TherophyProgramsDTO;
import com.clinicadmin.entity.TherapyExercises;
import com.clinicadmin.entity.TherophyProgramEntity;
import com.clinicadmin.repository.TherophyProgramRepository;
import com.clinicadmin.service.TherophyProgramService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TherophyProgramServiceImpl implements TherophyProgramService {

    private final TherophyProgramRepository repository;
    
    private final TherapyServiceServiceImpl therapyServiceServiceImpl;

    private TherophyProgramEntity mapToEntity(TherophyProgramsDTO dto) {
        return new TherophyProgramEntity(
                dto.getId(),
                dto.getProgramName(),
                dto.getTherophyIds(),
                dto.getClinicId(),
                dto.getBranchId()
        );
    }

    private TherophyProgramsDTO mapToDTO(TherophyProgramEntity entity) {
        return new TherophyProgramsDTO(
                entity.getId(),
                entity.getProgramName(),
                entity.getTherophyIds(),
                entity.getClinicId(),
                entity.getBranchId()
        );
    }
    

    @Override
    public ResponseEntity<Response> create(TherophyProgramsDTO dto) {
        try {
            TherophyProgramEntity saved = repository.save(mapToEntity(dto));

            return ResponseEntity.ok(
                    Response.builder()
                            .success(true)
                            .data(mapToDTO(saved))
                            .message("Program created successfully")
                            .status(200)
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Response.builder()
                            .success(false)
                            .message("Error creating program: " + e.getMessage())
                            .status(500)
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity<Response> getById(String id) {
        try {
            TherophyProgramEntity entity = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Program not found"));

            return ResponseEntity.ok(
                    Response.builder()
                            .success(true)
                            .data(mapToDTO(entity))
                            .message("Program fetched successfully")
                            .status(200)
                            .build()
            );

        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(
                    Response.builder()
                            .success(false)
                            .message(e.getMessage())
                            .status(404)
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Response.builder()
                            .success(false)
                            .message("Error fetching program: " + e.getMessage())
                            .status(500)
                            .build()
            );
        }
    }
    
    @Override
    public ResponseEntity<Response> getByclinicAndBranchIdAndId(String cid,String bid,String id) {
        try {
            TherophyProgramEntity entity = repository.findByClinicIdAndBranchIdAndId(cid, bid, id);
            ProgramWithTherophy programWithTherophy = null;           
              List<TherapyServiceDTO> lst = new ArrayList<>();
              if(entity != null) {
            	   for(String s:entity.getTherophyIds()) {
            		   TherapyServiceDTO thry = therapyServiceServiceImpl.getTherapyWithExercisesWithId(s);
            		  // System.out.println(thry);
            		   lst.add(thry);}
           programWithTherophy = new ProgramWithTherophy();
           programWithTherophy.setBranchId(entity.getBranchId());
           programWithTherophy.setClinicId(entity.getClinicId());
           programWithTherophy.setId(entity.getId());
           programWithTherophy.setProgramName(entity.getProgramName());
           programWithTherophy.setTherophyData(lst);
           long count = lst.stream()
                   .filter(Objects::nonNull)
                   .count();
           programWithTherophy.setTotalTherophyIds(Integer.valueOf(String.valueOf(count)));  
            return ResponseEntity.ok(
                    Response.builder()
                            .success(true)
                            .data(programWithTherophy)
                            .message("Program fetched successfully")
                            .status(200)
                            .build()
            );}else {
            	 return ResponseEntity.ok(
                         Response.builder()
                                 .success(false)
                                 .data(null)
                                 .message("Program not found")
                                 .status(404)
                                 .build());
            }

        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(
                    Response.builder()
                            .success(false)
                            .message(e.getMessage())
                            .status(404)
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Response.builder()
                            .success(false)
                            .message("Error fetching program: " + e.getMessage())
                            .status(500)
                            .build()
            );
        }
    }

    public ResponseEntity<Response> getByclinicAndBranchId(String cid, String bid) {

        try {

            List<TherophyProgramEntity> entity =
                    repository.findByClinicIdAndBranchId(cid, bid);

            List<TheraphyProgramWithTheraphyNamesDto> responseList =
                    new LinkedList<>();

            if (entity != null && !entity.isEmpty()) {

                for (TherophyProgramEntity e : entity) {

                    TheraphyProgramWithTheraphyNamesDto programDto =
                            new TheraphyProgramWithTheraphyNamesDto();

                    List<TheraphyNamesDTO> therapyList =
                            new LinkedList<>();

                    int totalProgramAmount = 0;

                    if (e.getTherophyIds() != null &&
                            !e.getTherophyIds().isEmpty()) {

                        for (String therapyId : e.getTherophyIds()) {

                            TherapyServiceDTO therapy =
                                    therapyServiceServiceImpl.getById(therapyId);
                         //   System.out.println(therapy);

                            if (therapy != null) {

                                int therapyTotalAmount = 0;

                                // Calculate Therapy Total Price
                                if (therapy.getExercises() != null &&
                                        !therapy.getExercises().isEmpty()) {

                                    for (TherapyExercises ex :
                                            therapy.getExercises()) {

                                        if (ex != null) {
                                          try {                                        
                                            int exerciseAmount = 
                                                    ex.getTotalPrice();

                                            therapyTotalAmount += exerciseAmount;
                                          }catch(Exception exception) {}
                                        }}}
                                // Add therapy total to program total
                                totalProgramAmount += therapyTotalAmount;

                                TheraphyNamesDTO dto =
                                        new TheraphyNamesDTO();

                                dto.setTheraphyId(therapyId);
                                dto.setTheraphyName(
                                        therapy.getTherapyName());

                                dto.setTheraphyTotalAmount(
                                        therapyTotalAmount);

                                therapyList.add(dto);
                            }
                        }
                    }

                    // Set Program Details
                    programDto.setId(e.getId());
                    programDto.setProgramName(e.getProgramName());
                    programDto.setClinicId(e.getClinicId());
                    programDto.setBranchId(e.getBranchId());
                    programDto.setTherophy(therapyList);
                    programDto.setTotalProgramAmount(totalProgramAmount);

                    long count = therapyList.stream()
                            .filter(Objects::nonNull)
                            .count();

                    programDto.setTheraphyCount(count);

                    responseList.add(programDto);
                }
            }

            if (responseList != null && !responseList.isEmpty()) {

                return ResponseEntity.ok(
                        Response.builder()
                                .success(true)
                                .data(responseList)
                                .message("Program fetched successfully")
                                .status(200)
                                .build()
                );
            }

            return ResponseEntity.ok(
                    Response.builder()
                            .success(false)
                            .data(null)
                            .message("Programs not found")
                            .status(404)
                            .build()
            );

        } catch (RuntimeException e) {

            return ResponseEntity.status(404).body(
                    Response.builder()
                            .success(false)
                            .message(e.getMessage())
                            .status(404)
                            .build()
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    Response.builder()
                            .success(false)
                            .message("Error fetching program: "
                                    + e.getMessage())
                            .status(500)
                            .build()
            );
        }
    }
    
    @Override
    public ResponseEntity<Response> getAll() {
        try {
            List<TherophyProgramsDTO> list = repository.findAll()
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                    Response.builder()
                            .success(true)
                            .data(list)
                            .message("All programs fetched")
                            .status(200)
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Response.builder()
                            .success(false)
                            .message("Error fetching programs: " + e.getMessage())
                            .status(500)
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity<Response> update(String id, TherophyProgramsDTO dto) {
        try {
            TherophyProgramEntity existing = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Program not found"));

            existing.setProgramName(dto.getProgramName());
            existing.setTherophyIds(dto.getTherophyIds());
            existing.setClinicId(dto.getClinicId());
            existing.setBranchId(dto.getBranchId());

            TherophyProgramEntity updated = repository.save(existing);

            return ResponseEntity.ok(
                    Response.builder()
                            .success(true)
                            .data(mapToDTO(updated))
                            .message("Program updated successfully")
                            .status(200)
                            .build()
            );

        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(
                    Response.builder()
                            .success(false)
                            .message(e.getMessage())
                            .status(404)
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Response.builder()
                            .success(false)
                            .message("Error updating program: " + e.getMessage())
                            .status(500)
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity<Response> delete(String id) {
        try {
            if (!repository.existsById(id)) {
                throw new RuntimeException("Program not found");
            }

            repository.deleteById(id);

            return ResponseEntity.ok(
                    Response.builder()
                            .success(true)
                            .message("Program deleted successfully")
                            .status(200)
                            .build()
            );

        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(
                    Response.builder()
                            .success(false)
                            .message(e.getMessage())
                            .status(404)
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Response.builder()
                            .success(false)
                            .message("Error deleting program: " + e.getMessage())
                            .status(500)
                            .build()
            );
        }
    }}