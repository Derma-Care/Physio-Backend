package com.clinicadmin.service.impl;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.FollowOptionDTO;
import com.clinicadmin.dto.FollowUpOptionData;
import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.FollowOption;
import com.clinicadmin.repository.FollowOptionRepository;
import com.clinicadmin.service.FollowOptionService;

import lombok.RequiredArgsConstructor;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j

@RequiredArgsConstructor
public class FollowOptionServiceImpl implements FollowOptionService {

    private final FollowOptionRepository repository;
    private FollowOptionDTO toDTO(FollowOption entity) {
        return FollowOptionDTO.builder()
                .id(entity.getId())
                .followOptions(entity.getFollowOptions()) // already a List<FollowUpOptionData>
                .build();
    }

    private FollowOption toEntity(FollowOptionDTO dto) {
        return FollowOption.builder()
                .followOptions(dto.getFollowOptions())
                .build();
    }


    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response create(FollowOptionDTO dto) {

        log.info("Received request to create Follow Option. ClinicId: {}, BranchId: {}",
                dto.getClinicId(), dto.getBranchId());

        try {

            log.debug("Mapping FollowOptionDTO to FollowOption entity.");

            FollowOption entity = toEntity(dto);

            log.info("Saving Follow Option.");

            FollowOption saved = repository.save(entity);

            log.info("Follow Option created successfully. FollowOptionId: {}",
                    saved.getId());

            log.debug("Returning success response for FollowOptionId: {}",
                    saved.getId());

            return Response.builder()
                    .success(true)
                    .status(201)
                    .message("Follow Option Created Successfully")
                    .data(toDTO(saved))
                    .build();

        } catch (Exception e) {

            log.error("Error occurred while creating Follow Option. ClinicId: {}, BranchId: {}, Error: {}",
                    dto.getClinicId(),
                    dto.getBranchId(),
                    e.getMessage(),
                    e);

            return Response.builder()
                    .success(false)
                    .status(500)
                    .message("Failed to create Follow Option: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }

    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response getAll() {

        log.info("Received request to fetch all Follow Options.");

        try {

            log.debug("Fetching all Follow Options from the database.");

            List<FollowOptionDTO> all = repository.findAll()
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} Follow Option(s).", all.size());

            log.debug("Returning all Follow Options in the response.");

            return Response.builder()
                    .success(true)
                    .status(200)
                    .message("All Follow Options")
                    .data(all)
                    .build();

        } catch (Exception e) {

            log.error("Error occurred while fetching Follow Options. Error: {}",
                    e.getMessage(), e);

            return Response.builder()
                    .success(false)
                    .status(500)
                    .message("Failed to fetch Follow Options: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response getById(String id) {

        log.info("Received request to fetch Follow Option. FollowOptionId: {}", id);

        try {

            log.debug("Searching for Follow Option with Id: {}", id);

            Optional<FollowOption> option = repository.findById(id);

            if (option.isPresent()) {

                log.info("Follow Option found. FollowOptionId: {}", id);

                log.debug("Returning Follow Option details for Id: {}", id);

                return Response.builder()
                        .success(true)
                        .status(200)
                        .message("Follow Option Found")
                        .data(toDTO(option.get()))
                        .build();

            } else {

                log.warn("Follow Option not found. FollowOptionId: {}", id);

                return Response.builder()
                        .success(false)
                        .status(404)
                        .message("Follow Option Not Found")
                        .build();
            }

        } catch (Exception e) {

            log.error("Error occurred while fetching Follow Option. FollowOptionId: {}, Error: {}",
                    id, e.getMessage(), e);

            return Response.builder()
                    .success(false)
                    .status(500)
                    .message("Failed to fetch Follow Option: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response update(String id, FollowOptionDTO dto) {

        log.info("Received request to update Follow Option. FollowOptionId: {}", id);

        try {

            // ================= VALIDATE REQUEST =================
            if (dto == null || dto.getFollowOptions() == null || dto.getFollowOptions().isEmpty()) {

                log.warn("Invalid update request. FollowOptionDTO or followOptions is null/empty. FollowOptionId: {}", id);

                return Response.builder()
                        .success(false)
                        .status(400)
                        .message("FollowOptionDTO or followOptions cannot be null/empty")
                        .build();
            }

            if (id == null || id.isBlank()) {

                log.warn("Invalid FollowOptionId received.");

                return Response.builder()
                        .success(false)
                        .status(400)
                        .message("Invalid ID")
                        .build();
            }

            log.debug("Fetching Follow Option from database. FollowOptionId: {}", id);

            Optional<FollowOption> existingOption = repository.findById(id);

            if (existingOption.isPresent()) {

                log.info("Follow Option found. FollowOptionId: {}", id);

                FollowOption entityToUpdate = existingOption.get();

                log.debug("Filtering valid follow-up options.");

                List<FollowUpOptionData> validOptions = dto.getFollowOptions()
                        .stream()
                        .filter(opt -> opt != null
                                && opt.getLabel() != null && !opt.getLabel().isBlank()
                                && opt.getValue() != null && !opt.getValue().isBlank())
                        .toList();

                if (validOptions.isEmpty()) {

                    log.warn("No valid follow-up options found for FollowOptionId: {}", id);

                    return Response.builder()
                            .success(false)
                            .status(400)
                            .message("No valid follow-up options provided")
                            .build();
                }

                log.debug("Updating {} follow-up option(s).", validOptions.size());

                entityToUpdate.setFollowOptions(validOptions);

                log.info("Saving updated Follow Option. FollowOptionId: {}", id);

                FollowOption updated = repository.save(entityToUpdate);

                log.info("Follow Option updated successfully. FollowOptionId: {}", id);

                return Response.builder()
                        .success(true)
                        .status(200)
                        .message("Follow Option Updated Successfully")
                        .data(toDTO(updated))
                        .build();

            } else {

                log.warn("Follow Option not found. FollowOptionId: {}", id);

                return Response.builder()
                        .success(false)
                        .status(404)
                        .message("Follow Option Not Found")
                        .build();
            }

        } catch (Exception e) {

            log.error("Error occurred while updating Follow Option. FollowOptionId: {}, Error: {}",
                    id, e.getMessage(), e);

            return Response.builder()
                    .success(false)
                    .status(500)
                    .message("Failed to update Follow Option: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response delete(String id) {

        log.info("Received request to delete Follow Option. FollowOptionId: {}", id);

        try {

            if (id == null || id.isBlank()) {

                log.warn("Invalid FollowOptionId received for deletion.");

                return Response.builder()
                        .success(false)
                        .status(400)
                        .message("Invalid ID")
                        .build();
            }

            log.debug("Checking existence of Follow Option. FollowOptionId: {}", id);

            if (repository.existsById(id)) {

                log.info("Follow Option found. Deleting FollowOptionId: {}", id);

                repository.deleteById(id);

                log.info("Follow Option deleted successfully. FollowOptionId: {}", id);

                return Response.builder()
                        .success(true)
                        .status(200)
                        .message("Follow Option Deleted")
                        .build();

            } else {

                log.warn("Follow Option not found. FollowOptionId: {}", id);

                return Response.builder()
                        .success(false)
                        .status(404)
                        .message("Follow Option Not Found")
                        .build();
            }

        } catch (Exception e) {

            log.error("Error occurred while deleting Follow Option. FollowOptionId: {}, Error: {}",
                    id, e.getMessage(), e);

            return Response.builder()
                    .success(false)
                    .status(500)
                    .message("Failed to delete Follow Option: " + e.getMessage())
                    .build();
        }
    }
}
