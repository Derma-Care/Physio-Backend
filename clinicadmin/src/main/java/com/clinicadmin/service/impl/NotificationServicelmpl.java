package com.clinicadmin.service.impl;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.clinicadmin.dto.ImageForNotificationDto;
import com.clinicadmin.dto.PriceDropAlertDto;
import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.ImageForNotification;
import com.clinicadmin.feignclient.NotificationFeign;
import com.clinicadmin.repository.ImageForNotificationRepo;
import com.clinicadmin.service.NotificationService;
import com.clinicadmin.utils.FeignImpl;
import com.clinicadmin.utils.KeyCloakTokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationServicelmpl implements NotificationService {
	
	@Autowired
	private ImageForNotificationRepo imageForNotificationRepo;
	
	@Autowired
	private FeignImpl notificationFeign;
	
	 @Autowired
	 private KeyCloakTokenStore keyCloakTokenStore;
	
	
	@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "storeImageForNotificationFallback")
    public ResponseEntity<?> storeImageForNotification(ImageForNotificationDto imageForNotificationDto) {
        log.info("Store notification image request received");
        Response response = new Response();
        try {
            log.debug("Looking up notification image");
            ImageForNotification enty = imageForNotificationRepo.findByImageName("NotificationImage");
            if (enty == null) {
                imageForNotificationDto.setImageName("NotificationImage");
                ImageForNotification entity = new ObjectMapper().convertValue(imageForNotificationDto,
                        ImageForNotification.class);
                log.debug("Saving notification image");
                imageForNotificationRepo.save(entity);
            } else {
                ImageForNotification entity = new ObjectMapper().convertValue(enty, ImageForNotification.class);
                entity.setImage(Base64.getDecoder().decode(imageForNotificationDto.getImage()));
                imageForNotificationRepo.save(entity);
            }

            response.setSuccess(true);
            response.setMessage("Image Saved Successfully");
            response.setStatus(200);

        } catch (Exception e) {
            log.error("Operation failed", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    public byte[] getImageForNotification() {
        log.info("Fetching notification image");
        try {
            ImageForNotification enty = imageForNotificationRepo.findByImageName("NotificationImage");
            if (enty != null) {
                return enty.getImage();
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Operation failed", e);
            return null;
        }
    }

    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "pricedropFallback")
    public ResponseEntity<?> pricedrop(PriceDropAlertDto priceDropAlertDto) {
        log.info("Creating price drop alert");
        Response response = new Response();
        try {
            log.debug("Calling notification service for price drop");
            return notificationFeign.pricedrop(keyCloakTokenStore.getAccess_token(),priceDropAlertDto);
        } catch (Exception e) {
            log.error("Operation failed", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "priceDropNotificationFallback")
    public ResponseEntity<?> priceDropNotification(String clinicId, String branchId) {
        log.info("Fetching price drop notifications clinicId={} branchId={}", clinicId, branchId);
        Response response = new Response();
        try {
            return notificationFeign.priceDropNotification(keyCloakTokenStore.getAccess_token(),clinicId, branchId);
        } catch (Exception e) {
            log.error("Operation failed", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updatePriceDropNotificationFallback")
    public ResponseEntity<?> updatePriceDropNotification(String clinicId, String branchId, String id,
            PriceDropAlertDto dto) {
        log.info("Updating price drop notification clinicId={} branchId={} id={}", clinicId, branchId, id);
         
        Response response = new Response();
        try {
            return notificationFeign.updatePriceDropNotification(keyCloakTokenStore.getAccess_token(),clinicId, branchId, id, dto);
        } catch (Exception e) {
            log.error("Operation failed", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "deletePriceDropNotificationFallback")
    public ResponseEntity<?> deletePriceDropNotification(String clinicId, String branchId, String id) {
        Response response = new Response();
        try {
            return notificationFeign.deletePriceDropNotification(keyCloakTokenStore.getAccess_token(),clinicId, branchId, id);
        } catch (Exception e) {
            log.error("Operation failed", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    
    public ResponseEntity<?> storeImageForNotificationFallback(
            ImageForNotificationDto dto,
            Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<?> pricedropFallback(
            PriceDropAlertDto dto,
            Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<?> priceDropNotificationFallback(
            String clinicId,
            String branchId,
            Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<?> updatePriceDropNotificationFallback(
            String clinicId,
            String branchId,
            String id,
            PriceDropAlertDto dto,
            Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<?> deletePriceDropNotificationFallback(
            String clinicId,
            String branchId,
            String id,
            Exception ex) {
        log.error("Rate limiter fallback triggered", ex);
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<?> buildRateLimitResponse(Exception ex) {
        Response response = new Response();
        response.setSuccess(false);
        response.setStatus(429);
        response.setMessage("Too many requests. Please try again after some time.");
        return ResponseEntity.status(429).body(response);
    }

}
