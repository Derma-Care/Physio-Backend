package com.AdminService.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.AdminService.dto.ServiceMediaCarouselDTO;
import com.AdminService.service.ServiceMediaCarouselServiceImpl;

@RestController
@RequestMapping("/admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class ServiceAdvertisementCarousel {

    @Autowired
    private ServiceMediaCarouselServiceImpl mediaService;

    // Add new media
    @PostMapping("/ServiceAdvertisement/add")
    public ResponseEntity<ServiceMediaCarouselDTO> createMedia(@RequestBody ServiceMediaCarouselDTO mediaDTO) {
        ServiceMediaCarouselDTO createdMedia = mediaService.createMedia(mediaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMedia);
    }

    // Get all media
    @GetMapping("/ServiceAdvertisement/getAll")
    public ResponseEntity<?> getAllMedia() {
        var mediaList = mediaService.getAllMedia();
        return mediaList.iterator().hasNext()
                ? ResponseEntity.ok(mediaList)
                : ResponseEntity.status(HttpStatus.OK).body("No media found");
    }

    @GetMapping("/ServiceAdvertisement/getById/{carouselId}")
    public ResponseEntity<?> getMediaById(@PathVariable String carouselId) {
        Optional<ServiceMediaCarouselDTO> mediaDTO = mediaService.getMediaById(carouselId);
        if (mediaDTO.isPresent()) {
            return ResponseEntity.ok(mediaDTO.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Media not found with ID: " + carouselId);
        }
    }


    // Update media by ID
    @PutMapping("/ServiceAdvertisement/updateById/{carouselId}")
    public ResponseEntity<String> updateMedia(@PathVariable String carouselId,
                                              @RequestBody ServiceMediaCarouselDTO mediaDTO) {
        return mediaService.updateMediaOptional(carouselId, mediaDTO)
                .map(dto -> ResponseEntity.ok("Media Carousel updated successfully!"))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Media not found"));
    }

    // Delete media by ID
    @DeleteMapping("/ServiceAdvertisement/deleteByCarouselId/{carouselId}")
    public ResponseEntity<String> deleteMedia(@PathVariable String carouselId) {
        String result = mediaService.deleteMedia(carouselId);
        return result.equalsIgnoreCase("Delete successful")
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
}
