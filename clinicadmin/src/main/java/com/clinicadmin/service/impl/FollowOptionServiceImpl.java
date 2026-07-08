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
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Service
@RequiredArgsConstructor
@Slf4j
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
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "createFallback")
    public Response create(FollowOptionDTO dto) {
        log.info("Creating follow option");
        log.debug("Request contains {} options", dto.getFollowOptions() != null ? dto.getFollowOptions().size() : 0);
        log.debug("Saving follow option to repository");
        FollowOption saved = repository.save(toEntity(dto));
        log.info("Follow option created successfully id={}", saved.getId());
        return Response.builder()
                .success(true)
                .status(201)
                .message("Follow Option Created Successfully")
                .data(toDTO(saved))
                .build();
    }

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getAllFallback")
    public Response getAll() {
        log.info("Fetching all follow options");
        log.debug("Calling repository.findAll()");
        List<FollowOptionDTO> all = repository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return Response.builder()
                .success(true)
                .status(200)
                .message("All Follow Options")
                .data(all)
                .build();
    }

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getByIdFallback")
    public Response getById(String id) {
        log.info("Fetching follow option id={}", id);
        Optional<FollowOption> option = repository.findById(id);
        if (option.isPresent()) {
            return Response.builder()
                    .success(true)
                    .status(200)
                    .message("Follow Option Found")
                    .data(toDTO(option.get()))
                    .build();
        } else {
            return Response.builder()
                    .success(false)
                    .status(404)
                    .message("Follow Option Not Found")
                    .build();
        }
    }
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updateFallback")
    public Response update(String id, FollowOptionDTO dto) {
        log.info("Updating follow option id={}", id);
        if (dto == null || dto.getFollowOptions() == null || dto.getFollowOptions().isEmpty()) {
            return Response.builder()
                    .success(false)
                    .status(400)
                    .message("FollowOptionDTO or followOptions cannot be null/empty")
                    .build();
        }

        if (id == null || id.isBlank()) {
            return Response.builder()
                    .success(false)
                    .status(400)
                    .message("Invalid ID")
                    .build();
        }

        Optional<FollowOption> existingOption = repository.findById(id);
        if (existingOption.isPresent()) {
            FollowOption entityToUpdate = existingOption.get();

            // Filter out any null or invalid FollowUpOptionData
            List<FollowUpOptionData> validOptions = dto.getFollowOptions().stream()
                    .filter(opt -> opt != null
                            && opt.getLabel() != null && !opt.getLabel().isBlank()
                            && opt.getValue() != null && !opt.getValue().isBlank())
					.toList(); // Java 16+, for older versions use .collect(Collectors.toList())

            if (validOptions.isEmpty()) {
                return Response.builder()
                        .success(false)
                        .status(400)
                        .message("No valid follow-up options provided")
                        .build();
            }

            // Update with valid options only
            entityToUpdate.setFollowOptions(validOptions);

            log.debug("Saving updated follow option id={}", id);
            FollowOption updated = repository.save(entityToUpdate);
            log.info("Follow option updated successfully id={}", updated.getId());
            return Response.builder()
                    .success(true)
                    .status(200)
                    .message("Follow Option Updated Successfully")
                    .data(toDTO(updated))
                    .build();
        } else {
            return Response.builder()
                    .success(false)
                    .status(404)
                    .message("Follow Option Not Found")
                    .build();
        }
    }

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteFallback")
    public Response delete(String id) {
        log.info("Deleting follow option id={}", id);
        if (repository.existsById(id)) {
            log.debug("Deleting follow option from repository id={}", id);
            repository.deleteById(id);
            log.info("Follow option deleted successfully id={}", id);
            return Response.builder()
                    .success(true)
                    .status(200)
                    .message("Follow Option Deleted")
                    .build();
        } else {
            return Response.builder()
                    .success(false)
                    .status(404)
                    .message("Follow Option Not Found")
                    .build();
        }
    }

    public Response createFallback(FollowOptionDTO dto, Exception ex) {
        log.error("Rate limit triggered in create", ex);
        return buildRateLimitResponse(ex);
    }

    public Response getAllFallback(Exception ex) {
        log.error("Rate limit triggered in getAll", ex);
        return buildRateLimitResponse(ex);
    }

    public Response getByIdFallback(String id, Exception ex) {
        log.error("Rate limit triggered in getById id={}", id, ex);
        return buildRateLimitResponse(ex);
    }

    public Response updateFallback(String id, FollowOptionDTO dto, Exception ex) {
        log.error("Rate limit triggered in update id={}", id, ex);
        return buildRateLimitResponse(ex);
    }

    public Response deleteFallback(String id, Exception ex) {
        log.error("Rate limit triggered in delete id={}", id, ex);
        return buildRateLimitResponse(ex);
    }

    public Response buildRateLimitResponse(Exception ex) {
        return Response.builder()
                .success(false)
                .status(429)
                .message("Too many requests. Please try again later.")
                .data(null)
                .build();
    }

}