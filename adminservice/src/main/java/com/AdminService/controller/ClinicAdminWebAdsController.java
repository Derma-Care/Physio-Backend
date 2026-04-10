package com.AdminService.controller;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.AdminService.dto.ClinicAdminWebAdsDTO;
import com.AdminService.service.ClinicAdminWebAdsServiceImpl;

@RestController
@RequestMapping("/admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class ClinicAdminWebAdsController {

    @Autowired
    private ClinicAdminWebAdsServiceImpl adsService;

    @PostMapping("/clinicWebAds/add")
    public ResponseEntity<ClinicAdminWebAdsDTO> createAd(@RequestBody ClinicAdminWebAdsDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adsService.createAd(dto));
    }

    @GetMapping("/clinicWebAds/getAll")
    public ResponseEntity<Iterable<ClinicAdminWebAdsDTO>> getAllAds() {
        Iterable<ClinicAdminWebAdsDTO> ads = adsService.getAllAds();

        if (!ads.iterator().hasNext()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(ads);
    }

    @GetMapping("/clinicWebAds/getById/{adId}")
    public ResponseEntity<ClinicAdminWebAdsDTO> getAdById(@PathVariable String adId) {
        Optional<ClinicAdminWebAdsDTO> dto = adsService.getAdById(adId);
        return dto.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ClinicAdminWebAdsDTO("", "Media not found")));
    }

    @PutMapping("/clinicWebAds/updateById/{adId}")
    public ResponseEntity<String> updateAd(@PathVariable String adId, @RequestBody ClinicAdminWebAdsDTO dto) {
        return adsService.updateAd(adId, dto)
                .map(updated -> ResponseEntity.ok("Web Ad updated successfully!"))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Media not found"));
    }

    @DeleteMapping("/clinicWebAds/deleteById/{adId}")
    public ResponseEntity<String> deleteAd(@PathVariable String adId) {
        String result = adsService.deleteAd(adId);
        return result.equals("Delete successful") ?
                ResponseEntity.ok(result) :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
}
