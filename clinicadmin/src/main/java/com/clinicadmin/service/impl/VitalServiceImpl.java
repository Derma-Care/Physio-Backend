package com.clinicadmin.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.BookingResponse;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.VitalsDTO;
import com.clinicadmin.entity.Vitals;
import com.clinicadmin.repository.VitalsRepository;
import com.clinicadmin.service.VitalService;
import com.clinicadmin.utils.FeignImpl;
import com.clinicadmin.utils.KeyCloakTokenStore;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Service
public class VitalServiceImpl implements VitalService {

    private static final Logger log = LoggerFactory.getLogger(VitalServiceImpl.class);

    @Autowired
    private VitalsRepository vitalsRepository;

    @Autowired
    private FeignImpl bookingFeign;
    
	@Autowired	
	public KeyCloakTokenStore keyCloakTokenStore;

    @Override
    @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "postVitalsFallback")
    public Response postVitals(String bookingId, VitalsDTO dto) {
        log.info("Post vitals request received | bookingId={}", bookingId);

        Response res = new Response();
        try {
            log.debug("Calling Booking Service bookingId={}", bookingId);

            ResponseEntity<ResponseStructure<BookingResponse>> bookingResponse =
                    bookingFeign.getBookedService(keyCloakTokenStore.getAccess_token(),bookingId);

            BookingResponse resbody = bookingResponse.getBody().getData();

            log.debug("Booking Service response patientId={} bookingId={}", resbody.getPatientId(), resbody.getBookingId());

            if (!resbody.getBookingId().equals(bookingId)) {
                log.warn("Booking validation failed bookingId={}", bookingId);
                res.setSuccess(false);
                res.setMessage("Appointment data is not found for this id: " + bookingId);
                res.setStatus(HttpStatus.OK.value());
                return res;
            }

            // ✅ Create new vitals
            Vitals vital = new Vitals();
            vital.setPatientId(resbody.getPatientId());
            vital.setPatientName(resbody.getName());
            vital.setBloodPressure(dto.getBloodPressure());
            vital.setHeight(dto.getHeight());
            vital.setBmi(dto.getBmi());
            vital.setTemperature(dto.getTemperature());
            vital.setWeight(dto.getWeight());
            vital.setBookingId(bookingId);

            // ✅ ADD DATE
            vital.setDate(LocalDateTime.now());

            log.debug("Saving vitals bookingId={} patientId={}", bookingId, resbody.getPatientId());
           // log.debug("Saving updated vitals bookingId={} patientId={}", bookingId, patientId);
                Vitals savedVitals = vitalsRepository.save(vital);
                //log.info("Vitals updated successfully bookingId={} patientId={}", bookingId, patientId);
            log.info("Vitals saved successfully id={} bookingId={}", savedVitals.getId(), bookingId);

            // ✅ Prepare DTO
            VitalsDTO dto1 = new VitalsDTO();
            dto1.setId(savedVitals.getId().toString());
            dto1.setPatientId(savedVitals.getPatientId());
            dto1.setPatientName(savedVitals.getPatientName());
            dto1.setBloodPressure(savedVitals.getBloodPressure());
            dto1.setBmi(savedVitals.getBmi());
            dto1.setHeight(savedVitals.getHeight());
            dto1.setTemperature(savedVitals.getTemperature());
            dto1.setWeight(savedVitals.getWeight());
            dto1.setBookingId(savedVitals.getBookingId());

            // ✅ ADD DATE IN RESPONSE
            dto1.setDate(savedVitals.getDate());

            res.setSuccess(true);

            // ✅ RETURN AS ARRAY
            res.setData(Collections.singletonList(dto1));

            res.setMessage("Vitals data added successfully");
            res.setStatus(HttpStatus.OK.value());

            return res;

        } catch (Exception e) {
            log.error("Exception occurred while adding vitals | bookingId={}", bookingId, e);

            res.setSuccess(false);
            res.setMessage("Exception occurs during adding vitals: " + e.getMessage());
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

            return res;
        }
    }

    @Override
    @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getPatientByBookingIdAndPatientIdFallback")
    public Response getPatientByBookingIdAndPatientId(String bookingId, String patientId) {

        log.info("Fetching vitals | bookingId={}, patientId={}", bookingId, patientId);

        Response res = new Response();

        try {

            List<Vitals> vitalsList =
                    vitalsRepository.findByBookingIdAndPatientId(bookingId, patientId);

            log.debug("Repository returned {} vitals records", vitalsList.size());

            if (!vitalsList.isEmpty()) {

                List<VitalsDTO> dtoList = new ArrayList<>();

                for (Vitals savedVitals : vitalsList) {

                    VitalsDTO dto1 = new VitalsDTO();

                    dto1.setId(savedVitals.getId().toString());
                    dto1.setPatientId(savedVitals.getPatientId());
                    dto1.setPatientName(savedVitals.getPatientName());
                    dto1.setBloodPressure(savedVitals.getBloodPressure());
                    dto1.setBmi(savedVitals.getBmi());
                    dto1.setHeight(savedVitals.getHeight());
                    dto1.setTemperature(savedVitals.getTemperature());
                    dto1.setWeight(savedVitals.getWeight());
                    dto1.setBookingId(savedVitals.getBookingId());

                    // ✅ ADD DATE
                    dto1.setDate(savedVitals.getDate());

                    dtoList.add(dto1);
                }

                log.info("Vitals retrieved successfully bookingId={} patientId={} count={}", bookingId, patientId, dtoList.size());
                res.setSuccess(true);
                res.setData(dtoList);
                res.setMessage("Vitals data retrieved successfully");
                res.setStatus(HttpStatus.OK.value());

                return res;

            } else {

                log.warn("Vitals not found bookingId={} patientId={}", bookingId, patientId);
                res.setSuccess(true);
                res.setData(Collections.emptyList());
                res.setMessage("Vitals data not found");
                res.setStatus(HttpStatus.OK.value());

                return res;
            }

        } catch (Exception e) {

            log.error("Exception occurred while retrieving vitals", e);

            res.setSuccess(false);
            res.setMessage("Exception occurs during retrieving vitals: " + e.getMessage());
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

            return res;
        }
    }
    @Override
    @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updateVitalsFallback")
    public Response updateVitals(String bookingId, String patientId, VitalsDTO dto) {

        log.info("Update vitals request | bookingId={}, patientId={}", bookingId, patientId);

        Response res = new Response();

        try {

            List<Vitals> vitOpt =
                    vitalsRepository.findByBookingIdAndPatientId(bookingId, patientId);

            log.debug("Repository returned {} records for update", vitOpt.size());

            if (!vitOpt.isEmpty()) {

                Vitals vital = vitOpt.get(0);

                if (dto.getBloodPressure() != null) vital.setBloodPressure(dto.getBloodPressure());
                if (dto.getBmi() != null) vital.setBmi(dto.getBmi());
                if (dto.getHeight() != null) vital.setHeight(dto.getHeight());
                if (dto.getPatientName() != null) vital.setPatientName(dto.getPatientName());
                if (dto.getTemperature() != null) vital.setTemperature(dto.getTemperature());
                if (dto.getWeight() != 0) vital.setWeight(dto.getWeight());

                //log.debug("Saving vitals bookingId={} patientId={}", bookingId, resbody.getPatientId());
            Vitals savedVitals = vitalsRepository.save(vital);
            log.info("Vitals saved successfully id={} bookingId={}", savedVitals.getId(), bookingId);

                VitalsDTO dtoResp = new VitalsDTO();
                dtoResp.setId(savedVitals.getId().toString());
                dtoResp.setPatientId(savedVitals.getPatientId());
                dtoResp.setPatientName(savedVitals.getPatientName());
                dtoResp.setBloodPressure(savedVitals.getBloodPressure());
                dtoResp.setBmi(savedVitals.getBmi());
                dtoResp.setHeight(savedVitals.getHeight());
                dtoResp.setTemperature(savedVitals.getTemperature());
                dtoResp.setWeight(savedVitals.getWeight());
                dtoResp.setBookingId(savedVitals.getBookingId());

                // ✅ ADD DATE
                dtoResp.setDate(savedVitals.getDate());

                res.setSuccess(true);

                // ✅ ARRAY RESPONSE
                res.setData(Collections.singletonList(dtoResp));

                res.setMessage("Vitals updated successfully");
                res.setStatus(HttpStatus.OK.value());

            } else {
                res.setSuccess(true);
                res.setData(Collections.emptyList());
                res.setMessage("Vitals data not found");
                res.setStatus(HttpStatus.OK.value());
            }

        } catch (Exception e) {

            log.error("Exception occurred while updating vitals bookingId={} patientId={}", bookingId, patientId, e);
            res.setSuccess(false);
            res.setMessage("Exception occurred while updating data: " + e.getMessage());
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return res;
    }

    @Override
    @Secured({"ROLE_CLINICADMIN","ROLE_DOCTOR"})
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deleteVitalsFallback")
    public Response deleteVitals(String bookingId, String patientId) {

        log.info("Delete vitals request received | bookingId={}, patientId={}", bookingId, patientId);

        Response resp = new Response();

        try {

        	List<Vitals> vit =
        	        vitalsRepository.findByBookingIdAndPatientId(bookingId, patientId);

            log.debug("Repository returned {} records for delete", vit.size());

            if (!vit.isEmpty()) {

                log.debug("Deleting vitals bookingId={} patientId={}", bookingId, patientId);
                vitalsRepository.deleteByBookingIdAndPatientId(bookingId, patientId);
                log.info("Vitals deleted successfully bookingId={} patientId={}", bookingId, patientId);

                resp.setSuccess(true);
                resp.setMessage("Vitals Deleted");
                resp.setStatus(HttpStatus.OK.value());

            } else {

                resp.setSuccess(true);
                resp.setData(Collections.emptyList());
                resp.setMessage("Vitals Data not found");
                resp.setStatus(HttpStatus.OK.value());
            }

        } catch (Exception e) {

            log.error("Exception occurred while deleting vitals bookingId={} patientId={}", bookingId, patientId, e);
            resp.setSuccess(false);
            resp.setMessage("Exception occured during deleting data " + e.getMessage());
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return resp;
    }


    // ================= RATE LIMIT FALLBACKS =================

    public Response postVitalsFallback(
            String bookingId,
            VitalsDTO dto,
            Exception ex) {
        log.error("Rate limiter triggered", ex);
        return buildRateLimitResponse();
    }

    public Response getPatientByBookingIdAndPatientIdFallback(
            String bookingId,
            String patientId,
            Exception ex) {
        log.error("Rate limiter triggered", ex);
        return buildRateLimitResponse();
    }

    public Response updateVitalsFallback(
            String bookingId,
            String patientId,
            VitalsDTO dto,
            Exception ex) {
        log.error("Rate limiter triggered", ex);
        return buildRateLimitResponse();
    }

    public Response deleteVitalsFallback(
            String bookingId,
            String patientId,
            Exception ex) {
        log.error("Rate limiter triggered", ex);
        return buildRateLimitResponse();
    }

    public Response buildRateLimitResponse() {
        log.warn("Returning rate limit response");
        Response response = new Response();
        response.setSuccess(false);
        response.setMessage("Too many requests. Please try again after some time.");
        response.setStatus(429);
        return response;
    }

}