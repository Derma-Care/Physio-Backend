package com.dermacare.category_services.service.Impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dermacare.category_services.dto.SubServiceDTO;
import com.dermacare.category_services.dto.SubServicesInfoDto;
import com.dermacare.category_services.entity.SubServiceInfoEntity;
import com.dermacare.category_services.entity.SubServices;
import com.dermacare.category_services.entity.SubServicesInfoEntity;
import com.dermacare.category_services.repository.SubServiceRepository;
import com.dermacare.category_services.repository.SubServicesInfoRepository;
import com.dermacare.category_services.service.SubServiceInfo;
import com.dermacare.category_services.util.Converter;
import com.dermacare.category_services.util.Response;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SubServiceInfoServiceImpl implements SubServiceInfo {

    @Autowired
    private Converter converter;

    @Autowired
    private SubServicesInfoRepository subServicesInfoRepository;

    @Autowired
    private SubServiceRepository subServiceRepository;

    // ================== ADD ==================
    @Override
    public Response addSubService(SubServicesInfoDto requestDto) {
        Response response = new Response();

        try {
            SubServicesInfoEntity entity = converter.entityConverter(requestDto);

            if (entity.getCategoryName() == null) {
                return fail(response, 400, "Invalid CategoryId");
            }

            if (entity.getSubServices() == null || entity.getSubServices().isEmpty()) {
                return fail(response, 400, "SubServices list cannot be empty");
            }

            for (SubServiceInfoEntity e : entity.getSubServices()) {
                if (e.getServiceName() == null) {
                    return fail(response, 400, "Invalid ServiceId");
                }
            }

            Set<String> uniqueKeys = new HashSet<>();

            for (SubServiceDTO dto : requestDto.getSubServices()) {

                String name = dto.getSubServiceName().trim().toLowerCase();
                String serviceId = dto.getServiceId();

                String key = serviceId + "_" + name;

                if (!uniqueKeys.add(key)) {
                    return fail(response, 400,
                            "Duplicate SubService in same Service: " + dto.getSubServiceName());
                }

                boolean exists = subServicesInfoRepository
                        .existsBySubServicesServiceIdAndSubServicesSubServiceNameIgnoreCase(serviceId, name);

                if (exists) {
                    return fail(response, 409,
                            "SubService '" + dto.getSubServiceName() + "' already exists");
                }
            }

            SubServicesInfoEntity saved = subServicesInfoRepository.save(entity);

            response.setData(converter.dtoConverter(saved));
            return success(response, "Saved successfully");

        } catch (Exception e) {
            return error(response, e);
        }
    }

    // ================== GET BY CATEGORY ==================
    @Override
    public Response getSubServiceByIdCategory(String categoryId) {
        Response response = new Response();
        try {
            List<SubServicesInfoEntity> list =
                    subServicesInfoRepository.findByCategoryId(categoryId);

            if (list == null || list.isEmpty()) {
                return fail(response, 404, "No SubServices found");
            }

            List<SubServicesInfoDto> dto =
                    new ObjectMapper().convertValue(list, new TypeReference<>() {});

            response.setData(dto);
            return success(response, "Fetched successfully");

        } catch (Exception e) {
            return error(response, e);
        }
    }

    // ================== GET BY SERVICE ==================
    @Override
    public Response getSubServicesByServiceId(String serviceId) {
        Response response = new Response();
        try {
            List<SubServicesInfoEntity> list =
                    subServicesInfoRepository.findByServiceId(serviceId);

            if (list == null || list.isEmpty()) {
                return fail(response, 404, "No SubServices found");
            }

            List<SubServicesInfoDto> dto =
                    new ObjectMapper().convertValue(list, new TypeReference<>() {});

            response.setData(dto);
            return success(response, "Fetched successfully");

        } catch (Exception e) {
            return error(response, e);
        }
    }

    // ================== GET BY SUBSERVICE ID ==================
    @Override
    public Response getSubServiceBySubServiceId(String subServiceId) {
        Response response = new Response();
        try {
            SubServicesInfoEntity entity =
                    subServicesInfoRepository.findBySubServicesSubServiceId(subServiceId);

            if (entity == null) {
                return fail(response, 404, "SubService not found");
            }

            response.setData(converter.dtoConverter(entity));
            return success(response, "Fetched successfully");

        } catch (Exception e) {
            return error(response, e);
        }
    }

    // ================== DELETE ==================
    @Override
    public Response deleteSubService(String subServiceId) {
        Response response = new Response();

        try {
            SubServicesInfoEntity entity =
                    subServicesInfoRepository.findBySubServicesSubServiceId(subServiceId);

            if (entity == null) {
                return fail(response, 404, "SubService not found");
            }

            List<SubServiceInfoEntity> list = entity.getSubServices();

            boolean removed = list.removeIf(s -> subServiceId.equals(s.getSubServiceId()));

            if (!removed) {
                return fail(response, 404, "SubService not found in list");
            }

            if (list.isEmpty()) {
                subServicesInfoRepository.delete(entity);
            } else {
                entity.setSubServices(list);
                subServicesInfoRepository.save(entity);
            }

            // delete related
            try {
                ObjectId objectId = new ObjectId(subServiceId);
                List<SubServices> related =
                        subServiceRepository.findBySubServiceId(objectId);

                if (related != null && !related.isEmpty()) {
                    subServiceRepository.deleteAll(related);
                }
            } catch (Exception ignored) {}

            return success(response, "Deleted successfully");

        } catch (Exception e) {
            return error(response, e);
        }
    }

    // ================== UPDATE ==================
    @Override
    public Response updateBySubServiceId(String subServiceId, SubServicesInfoDto dtoReq) {
        Response response = new Response();

        try {
            SubServicesInfoEntity entity =
                    subServicesInfoRepository.findBySubServicesSubServiceId(subServiceId);

            if (entity == null) {
                return fail(response, 404, "SubService not found");
            }

            if (dtoReq.getSubServices() == null || dtoReq.getSubServices().isEmpty()) {
                return fail(response, 400, "SubService data required");
            }

            SubServiceDTO dto = dtoReq.getSubServices().get(0);

            String newName = dto.getSubServiceName().trim();
            String serviceId = dto.getServiceId();

            Optional<SubServiceInfoEntity> currentOpt =
                    entity.getSubServices().stream()
                            .filter(s -> s.getSubServiceId().equals(subServiceId))
                            .findFirst();

            if (currentOpt.isEmpty()) {
                return fail(response, 404, "SubService not found in entity");
            }

            SubServiceInfoEntity current = currentOpt.get();

            boolean exists = subServicesInfoRepository
                    .existsBySubServicesServiceIdAndSubServicesSubServiceNameIgnoreCase(serviceId, newName);

            if (exists && !current.getSubServiceName().equalsIgnoreCase(newName)) {
                return fail(response, 409, "Duplicate SubService name");
            }

            // update main
            current.setSubServiceName(newName);

            // update child collection
            try {
                ObjectId objectId = new ObjectId(subServiceId);
                List<SubServices> subs =
                        subServiceRepository.findBySubServiceId(objectId);

                if (subs != null && !subs.isEmpty()) {
                    subs.forEach(s -> s.setSubServiceName(newName));
                    subServiceRepository.saveAll(subs);
                }
            } catch (Exception ignored) {}

            subServicesInfoRepository.save(entity);

            return success(response, "Updated successfully");

        } catch (Exception e) {
            return error(response, e);
        }
    }

    // ================== GET ALL ==================
    @Override
    public Response getAllSubServices() {
        Response response = new Response();

        try {
            List<SubServicesInfoEntity> entities = subServicesInfoRepository.findAll();

            if (entities == null || entities.isEmpty()) {
                return fail(response, 404, "No data found");
            }

            List<SubServicesInfoDto> list = new ArrayList<>();
            for (SubServicesInfoEntity e : entities) {
                list.add(converter.dtoConverter(e));
            }

            response.setData(list);
            return success(response, "Fetched successfully");

        } catch (Exception e) {
            return error(response, e);
        }
    }

    // ================== COMMON METHODS ==================
    private Response success(Response r, String msg) {
        r.setStatus(200);
        r.setSuccess(true);
        r.setMessage(msg);
        return r;
    }

    private Response fail(Response r, int status, String msg) {
        r.setStatus(status);
        r.setSuccess(false);
        r.setMessage(msg);
        return r;
    }

    private Response error(Response r, Exception e) {
        r.setStatus(500);
        r.setSuccess(false);
        r.setMessage("Error: " + e.getMessage());
        return r;
    }
}