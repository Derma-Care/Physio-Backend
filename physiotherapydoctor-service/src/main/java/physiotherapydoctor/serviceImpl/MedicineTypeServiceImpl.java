package physiotherapydoctor.serviceImpl;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.MedicineTypeDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.entity.MedicineType;
import physiotherapydoctor.repository.MedicineTypeRepository;
import physiotherapydoctor.service.MedicineTypeService;

@Service
@Slf4j
public class MedicineTypeServiceImpl implements MedicineTypeService {

    @Autowired
    private MedicineTypeRepository repository;

    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "addMedicineTypeFallback")
    @Secured("ROLE_DOCTOR")
    public Response addMedicineType(MedicineTypeDTO dto) {

        long startTime = System.currentTimeMillis();

        log.info("Entered addMedicineType()");

        try {

            MedicineType entity;

            if (dto.getId() != null) {

                log.debug("Fetching medicine type by id : {}", dto.getId());

                entity = repository.findById(dto.getId())
                        .orElse(new MedicineType());

            } else {

                log.info("Creating new MedicineType document");

                entity = new MedicineType();
                entity.setMedicineTypes(new ArrayList<>());
            }

            if (entity.getMedicineTypes() == null) {

                log.debug("Initializing medicine types list");

                entity.setMedicineTypes(new ArrayList<>());
            }

            int addedCount = 0;

            for (String type : dto.getMedicineTypes()) {

                if (!entity.getMedicineTypes().contains(type)) {

                    entity.getMedicineTypes().add(type);
                    addedCount++;

                    log.debug("Added medicine type : {}", type);
                }
            }

            log.info("Total new medicine types added : {}", addedCount);

            log.debug("Saving medicine type entity");

            MedicineType saved = repository.save(entity);

            log.info("Medicine types saved successfully with id : {}",
                    saved.getId());

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("addMedicineType() completed successfully in {} ms",
                    executionTime);

            return Response.builder()
                    .success(true)
                    .status(201)
                    .message("Medicine types updated successfully")
                    .data(toDTO(saved))
                    .build();

        } catch (Exception e) {

            log.error("Exception occurred while adding medicine types. Error : {}",
                    e.getMessage(),
                    e);

            return Response.builder()
                    .success(false)
                    .status(500)
                    .message("Error adding medicine types")
                    .data(null)
                    .build();
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getMedicineTypesByIdFallback")
    @Secured("ROLE_DOCTOR")
    public Response getMedicineTypesById(String id) {

        long startTime = System.currentTimeMillis();

        log.info("Entered getMedicineTypesById() with id : {}", id);

        try {

            log.debug("Fetching medicine types from repository with id : {}", id);

            Optional<MedicineType> entity = repository.findById(id);

            if (entity.isEmpty()) {

                log.warn("Medicine types not found with id : {}", id);

                return Response.builder()
                        .success(false)
                        .status(404)
                        .message("Medicine types not found")
                        .data(null)
                        .build();
            }

            log.info("Medicine types found with id : {}", id);

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("getMedicineTypesById() completed successfully in {} ms",
                    executionTime);

            return Response.builder()
                    .success(true)
                    .status(200)
                    .message("Fetched medicine types successfully")
                    .data(toDTO(entity.get()))
                    .build();

        } catch (Exception e) {

            log.error("Exception occurred while fetching medicine types by id : {}. Error : {}",
                    id,
                    e.getMessage(),
                    e);

            return Response.builder()
                    .success(false)
                    .status(500)
                    .message("Error fetching medicine types")
                    .data(null)
                    .build();
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "searchOrAddMedicineTypeFallback")
    @Secured("ROLE_DOCTOR")
    public Response searchOrAddMedicineType(MedicineTypeDTO dto) {

        long startTime = System.currentTimeMillis();

        log.info("Entered searchOrAddMedicineType()");

        try {

            log.debug("Fetching all medicine type documents");

            List<MedicineType> list = repository.findAll();

            log.info("Fetched {} medicine type documents",
                    list.size());

            MedicineType entity;

            if (list.isEmpty()) {

                log.info("No medicine type document found. Creating new document");

                entity = new MedicineType();
                entity.setMedicineTypes(new ArrayList<>());

            } else {

                entity = list.get(0);

                log.debug("Using existing medicine type document with id : {}",
                        entity.getId());
            }

            List<String> added = new ArrayList<>();

            for (String type : dto.getMedicineTypes()) {

                if (!entity.getMedicineTypes().contains(type)) {

                    entity.getMedicineTypes().add(type);
                    added.add(type);

                    log.debug("Added new medicine type : {}", type);
                }
            }

            log.debug("Saving medicine type document");

            MedicineType saved = repository.save(entity);

            String msg = added.isEmpty()
                    ? "Medicine types already exist"
                    : "New medicine types added: " + added;

            log.info(msg);

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("searchOrAddMedicineType() completed successfully in {} ms",
                    executionTime);

            return Response.builder()
                    .success(true)
                    .status(200)
                    .message(msg)
                    .data(toDTO(saved))
                    .build();

        } catch (Exception e) {

            log.error("Exception occurred while searching/adding medicine types. Error : {}",
                    e.getMessage(),
                    e);

            return Response.builder()
                    .success(false)
                    .status(500)
                    .message("Error processing medicine types")
                    .data(null)
                    .build();
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getAllMedicineTypesFallback")
    @Secured("ROLE_DOCTOR")
    public Response getAllMedicineTypes() {

        long startTime = System.currentTimeMillis();

        log.info("Entered getAllMedicineTypes()");

        Response response = new Response();

        try {

            log.debug("Fetching all medicine types from repository");

            List<MedicineType> list = repository.findAll();

            log.info("Fetched {} medicine type records",
                    list.size());

            if (list.isEmpty()) {

                log.warn("No medicine types found");

                response.setSuccess(true);
                response.setMessage("No medicine types found");
                response.setStatus(200);

            } else {

                response.setSuccess(true);
                response.setData(list);
                response.setMessage("Medicine types fetched successfully");
                response.setStatus(200);

                log.info("Medicine types fetched successfully");
            }

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("getAllMedicineTypes() completed successfully in {} ms",
                    executionTime);

        } catch (Exception e) {

            log.error("Exception occurred while fetching all medicine types. Error : {}",
                    e.getMessage(),
                    e);

            response.setSuccess(false);
            response.setMessage("Error fetching medicine types");
            response.setStatus(500);
        }

        return response;
    }
    
    
    private MedicineTypeDTO toDTO(MedicineType entity) { 
    	MedicineTypeDTO dto = new MedicineTypeDTO();
    	dto.setId(entity.getId()); 
    	dto.setMedicineTypes(entity.getMedicineTypes()); 
    	return dto; }
    
    

    private Response buildRateLimitResponse(Exception ex) {
        return Response.builder()
                .success(false)
                .status(429)
                .message("Rate limit exceeded. Please try again later.")
                .data(null)
                .build();
    }

    public Response addMedicineTypeFallback(MedicineTypeDTO dto, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response getMedicineTypesByIdFallback(String id, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response searchOrAddMedicineTypeFallback(MedicineTypeDTO dto, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response getAllMedicineTypesFallback(Exception ex) {
        return buildRateLimitResponse(ex);
    }

}