                                                                                                package com.AdminService.controller;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.AdminService.dto.CategoryMediaCarouselDTO;
import com.AdminService.service.CategoryMediaCarouselServiceImpl;
@RestController
@RequestMapping("/admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class CategoryMediaCarouselController {

    @Autowired
    private CategoryMediaCarouselServiceImpl mediaService;

    // Add new media
    @PostMapping("/categoryAdvertisement/add")
    public ResponseEntity<CategoryMediaCarouselDTO> createMedia(@RequestBody CategoryMediaCarouselDTO mediaDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaService.createMedia(mediaDTO));
    }

    // Get all media

    @GetMapping("/categoryAdvertisement/getAll")
    public ResponseEntity<Iterable<CategoryMediaCarouselDTO>> getAllMedia() {
        Iterable<CategoryMediaCarouselDTO> media = mediaService.getAllMedia();
        
        if (!media.iterator().hasNext()) {
            return ResponseEntity.noContent().build(); // Returns 204 No Content
        }

        return ResponseEntity.ok(media); // Returns 200 OK with media list
    }

    @GetMapping("/categoryAdvertisement/getById/{carouselId}")
    public ResponseEntity<CategoryMediaCarouselDTO> getMediaById(@PathVariable String carouselId) {
        Optional<CategoryMediaCarouselDTO> mediaDTO = mediaService.getMediaById(carouselId);
        return mediaDTO.map(media -> ResponseEntity.ok().body(media)) // if found
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new CategoryMediaCarouselDTO("", "Media not found"))); // if not found
    }
    

    // Update media by ID
    @PutMapping("/categoryAdvertisement/updateById/{carouselId}")
    public ResponseEntity<String> updateMedia(@PathVariable String carouselId, @RequestBody CategoryMediaCarouselDTO mediaDTO) {
        return mediaService.updateMediaOptional(carouselId, mediaDTO)
                .map(dto -> ResponseEntity.ok("Media Carousel updated successfully!"))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Media not found"));
    }

    // Delete media by ID
    
    @DeleteMapping("/categoryAdvertisement/deleteByCarouselId/{carouselId}")
    public ResponseEntity<String> deleteMedia(@PathVariable String carouselId) {
        String result = mediaService.deleteMedia(carouselId);
        return result.equals("Delete successful") ? ResponseEntity.ok(result) :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
}