                                                                                   package com.AdminService.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.AdminService.dto.DoctorWebAdsDTO;
import com.AdminService.service.DoctorWebAdsServiceImpl;

@RestController
@RequestMapping("/admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DoctorWebAdsController {

    @Autowired
    private DoctorWebAdsServiceImpl adService;

    // Add new doctor ad
    @PostMapping("/doctorWebAds/add")
    public ResponseEntity<DoctorWebAdsDTO> createAd(@RequestBody DoctorWebAdsDTO adDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adService.createAd(adDTO));
    }

    // Get all doctor ads
    @GetMapping("/doctorWebAds/getAll")
    public ResponseEntity<Iterable<DoctorWebAdsDTO>> getAllAds() {
        Iterable<DoctorWebAdsDTO> ads = adService.getAllAds();

        if (!ads.iterator().hasNext()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ads);
    }

    // Get doctor ad by Id
    @GetMapping("/doctorWebAds/getById/{adId}")
    public ResponseEntity<DoctorWebAdsDTO> getAdById(@PathVariable String adId) {
        Optional<DoctorWebAdsDTO> adDTO = adService.getAdById(adId);
        return adDTO.map(ad -> ResponseEntity.ok().body(ad))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new DoctorWebAdsDTO("", "Ad not found")));
    }

    // Update doctor ad by Id
    @PutMapping("/doctorWebAds/updateById/{adId}")
    public ResponseEntity<String> updateAd(@PathVariable String adId, @RequestBody DoctorWebAdsDTO adDTO) {
        return adService.updateAd(adId, adDTO)
                .map(dto -> ResponseEntity.ok("Doctor Web Ad updated successfully!"))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ad not found"));
    }

    // Delete doctor ad by Id
    @DeleteMapping("/doctorWebAds/deleteById/{adId}")
    public ResponseEntity<String> deleteAd(@PathVariable String adId) {
        String result = adService.deleteAd(adId);
        return result.equals("Delete successful") ? ResponseEntity.ok(result) :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
}
