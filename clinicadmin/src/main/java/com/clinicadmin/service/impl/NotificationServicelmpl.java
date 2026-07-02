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
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Service
public class NotificationServicelmpl implements NotificationService {
	
	@Autowired
	private ImageForNotificationRepo imageForNotificationRepo;
	
	@Autowired
	private FeignImpl notificationFeign;
	
	@Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "storeImageForNotificationFallback")
    public ResponseEntity<?> storeImageForNotification(ImageForNotificationDto imageForNotificationDto) {
        Response response = new Response();
        try {
            ImageForNotification enty = imageForNotificationRepo.findByImageName("NotificationImage");
            if (enty == null) {
                imageForNotificationDto.setImageName("NotificationImage");
                ImageForNotification entity = new ObjectMapper().convertValue(imageForNotificationDto,
                        ImageForNotification.class);
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
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    public byte[] getImageForNotification() {
        try {
            ImageForNotification enty = imageForNotificationRepo.findByImageName("NotificationImage");
            if (enty != null) {
                return enty.getImage();
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "pricedropFallback")
    public ResponseEntity<?> pricedrop(PriceDropAlertDto priceDropAlertDto) {
        Response response = new Response();
        try {
            return notificationFeign.pricedrop(priceDropAlertDto);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "priceDropNotificationFallback")
    public ResponseEntity<?> priceDropNotification(String clinicId, String branchId) {
        Response response = new Response();
        try {
            return notificationFeign.priceDropNotification(clinicId, branchId);
        } catch (Exception e) {
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
        Response response = new Response();
        try {
            return notificationFeign.updatePriceDropNotification(clinicId, branchId, id, dto);
        } catch (Exception e) {
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
            return notificationFeign.deletePriceDropNotification(clinicId, branchId, id);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    
    public ResponseEntity<?> storeImageForNotificationFallback(
            ImageForNotificationDto dto,
            Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<?> pricedropFallback(
            PriceDropAlertDto dto,
            Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<?> priceDropNotificationFallback(
            String clinicId,
            String branchId,
            Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<?> updatePriceDropNotificationFallback(
            String clinicId,
            String branchId,
            String id,
            PriceDropAlertDto dto,
            Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<?> deletePriceDropNotificationFallback(
            String clinicId,
            String branchId,
            String id,
            Exception ex) {
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
