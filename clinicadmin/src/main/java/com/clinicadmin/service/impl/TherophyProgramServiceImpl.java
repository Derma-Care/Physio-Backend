package com.clinicadmin.service.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import com.clinicadmin.dto.ProgramWithTherophy;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.TheraphyNamesDTO;
import com.clinicadmin.dto.TheraphyProgramWithTheraphyNamesDto;
import com.clinicadmin.dto.TherapyServiceDTO;
import com.clinicadmin.dto.TherophyProgramsDTO;
import com.clinicadmin.entity.TherapyExercises;
import com.clinicadmin.entity.TherophyProgramEntity;
import com.clinicadmin.repository.TherapyExercisesRepository;
import com.clinicadmin.repository.TherophyProgramRepository;
import com.clinicadmin.service.TherophyProgramService;
import lombok.RequiredArgsConstructor;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TherophyProgramServiceImpl implements TherophyProgramService {

    private final TherophyProgramRepository repository;
    
    private final TherapyServiceServiceImpl therapyServiceServiceImpl;
    
    @Autowired
    private TherapyExercisesRepository therapyExercisesRepository;

    private TherophyProgramEntity mapToEntity(TherophyProgramsDTO dto) {
        log.debug("Mapping DTO to Entity programName={} clinicId={} branchId={}", dto.getProgramName(), dto.getClinicId(), dto.getBranchId());
        return new TherophyProgramEntity(
                dto.getId(),
                dto.getProgramName(),
                dto.getTherophyIds(),
                dto.getClinicId(),
                dto.getBranchId()
        );
    }

    private TherophyProgramsDTO mapToDTO(TherophyProgramEntity entity) {
        log.debug("Mapping Entity to DTO id={}", entity.getId());
        return new TherophyProgramsDTO(
                entity.getId(),
                entity.getProgramName(),
                entity.getTherophyIds(),
                entity.getClinicId(),
                entity.getBranchId()
        );
    }
    

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "createFallback")
    public ResponseEntity<Response> create(TherophyProgramsDTO dto) {
        log.info("Entering create programName={} clinicId={} branchId={}", dto.getProgramName(), dto.getClinicId(), dto.getBranchId());
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
            log.error("Operation failed", e);
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
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getByIdFallback")
    public ResponseEntity<Response> getById(String id) {
        log.info("Entering getById id={}", id);
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
            log.error("Operation failed", e);
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
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getByclinicAndBranchIdAndIdFallback")
    public ResponseEntity<Response> getByclinicAndBranchIdAndId(String cid,String bid,String id) {
        log.info("Entering getByclinicAndBranchIdAndId clinicId={} branchId={} id={}", cid,bid,id);
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
            log.error("Operation failed", e);
            return ResponseEntity.internalServerError().body(
                    Response.builder()
                            .success(false)
                            .message("Error fetching program: " + e.getMessage())
                            .status(500)
                            .build()
            );
        }
    }

    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getByclinicAndBranchIdFallback")
    public ResponseEntity<Response> getByclinicAndBranchId(String cid, String bid) {
        log.info("Entering getByclinicAndBranchId clinicId={} branchId={}", cid,bid);

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
            log.error("Operation failed", e);

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
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllFallback")
    public ResponseEntity<Response> getAll() {
        log.info("Entering getAll");
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
            log.error("Operation failed", e);
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
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updateFallback")
    public ResponseEntity<Response> update(String id, TherophyProgramsDTO dto) {
        log.info("Entering update id={} programName={}", id, dto.getProgramName());
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
            log.error("Operation failed", e);
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
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteFallback")
    public ResponseEntity<Response> delete(String id) {
        log.info("Entering delete id={}", id);
        try {
            if (!repository.existsById(id)) {
                log.warn("Program not found");
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
            log.error("Operation failed", e);
            return ResponseEntity.internalServerError().body(
                    Response.builder()
                            .success(false)
                            .message("Error deleting program: " + e.getMessage())
                            .status(500)
                            .build()
            );
        }}
    

    // ================= RATE LIMIT FALLBACKS =================

    public ResponseEntity<Response> createFallback(TherophyProgramsDTO dto, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public ResponseEntity<Response> getByIdFallback(String id, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public ResponseEntity<Response> getByclinicAndBranchIdAndIdFallback(
            String cid, String bid, String id, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public ResponseEntity<Response> getByclinicAndBranchIdFallback(
            String cid, String bid, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public ResponseEntity<Response> getAllFallback(Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public ResponseEntity<Response> updateFallback(
            String id, TherophyProgramsDTO dto, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public ResponseEntity<Response> deleteFallback(String id, Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse();
    }

    public ResponseEntity<Response> buildRateLimitResponse() {
        log.warn("Returning rate limit response");
        return ResponseEntity.status(429).body(
                Response.builder()
                        .success(false)
                        .message("Too many requests. Please try again after some time.")
                        .status(429)
                        .build()
        );
    }

}