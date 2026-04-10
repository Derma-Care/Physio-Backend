package com.AdminService.controller;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.AdminService.dto.DoctorVerticalAdsDto;
import com.AdminService.dto.DoctorWebAdsDTO;
import com.AdminService.service.DoctorWebVerticalAdsServiceImpl;



@RestController
@RequestMapping("/admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DoctorWebVerticalAdsController {
	
	 @Autowired
	    private DoctorWebVerticalAdsServiceImpl adService;

	    // Add new doctor ad
	    @PostMapping("/doctorWebVerticalAds/add")
	    public ResponseEntity<DoctorWebAdsDTO> createAd(@RequestBody DoctorVerticalAdsDto adDTO) {
	        return ResponseEntity.status(HttpStatus.CREATED).body(adService.createAd(adDTO));
	    }

	    // Get all doctor ads
	    @GetMapping("/doctorWebVerticalAds/getAll")
	    public ResponseEntity<Iterable<DoctorVerticalAdsDto>> getAllAds() {
	        Iterable<DoctorVerticalAdsDto> ads = adService.getAllAds();

	        if (!ads.iterator().hasNext()) {
	            return ResponseEntity.noContent().build();
	        }
	        return ResponseEntity.ok(ads);
	    }

	    // Get doctor ad by Id
	    @GetMapping("/doctorWebVerticalAds/getById/{adId}")
	    public ResponseEntity<DoctorVerticalAdsDto> getAdById(@PathVariable String adId) {
	        Optional<DoctorVerticalAdsDto> adDTO = adService.getAdById(adId);
	        return adDTO.map(ad -> ResponseEntity.ok().body(ad))
	                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
	                        .body(new DoctorVerticalAdsDto("", "Ad not found")));
	    }

	    // Update doctor ad by Id
	    @PutMapping("/doctorWebVerticalAds/updateById/{adId}")
	    public ResponseEntity<String> updateAd(@PathVariable String adId, @RequestBody DoctorVerticalAdsDto adDTO) {
	        return adService.updateAd(adId, adDTO)
	                .map(dto -> ResponseEntity.ok("Doctor Web Ad updated successfully!"))
	                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ad not found"));
	    }

	    // Delete doctor ad by Id
	    @DeleteMapping("/doctorWebVerticalAds/deleteById/{adId}")
	    public ResponseEntity<String> deleteAd(@PathVariable String adId) {
	        String result = adService.deleteAd(adId);
	        return result.equals("Delete successful") ? ResponseEntity.ok(result) :
	                ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
	    }
	

}