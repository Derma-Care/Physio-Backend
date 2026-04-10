package com.AdminService.service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.AdminService.dto.DoctorWebAdsDTO;
import com.AdminService.entity.DoctorWebAds;
import com.AdminService.repository.DoctorWebAdsRepository;

import java.util.Base64;

@Service
public class DoctorWebAdsServiceImpl {

    private final DoctorWebAdsRepository adsRepository;

    @Value("${app.video.folder}")
    private String videoFolder;

    @Value("${app.video.base-url}")
    private String videoBaseUrl;

    @Value("${app.image.folder}")
    private String imageFolder;

    @Value("${app.image.base-url}")
    private String imageBaseUrl;

    public DoctorWebAdsServiceImpl(DoctorWebAdsRepository adsRepository) {
        this.adsRepository = adsRepository;
    }

    private static final Pattern VIDEO_PATTERN = Pattern.compile(
            ".*\\.(mp4|avi|mov|wmv|flv|mkv|webm|ogg|m4v)$",
            Pattern.CASE_INSENSITIVE
    );

    public DoctorWebAdsDTO createAd(DoctorWebAdsDTO dto) {
        DoctorWebAds ad = new DoctorWebAds();
        String mediaData = dto.getMediaUrlOrImage();

        if (isVideoUrl(mediaData)) {
            ad.setMediaUrlOrImage(mediaData);
        } else if (isBase64Video(mediaData)) {
            ad.setMediaUrlOrImage(saveBase64Video(mediaData));
        } else if (isBase64Image(mediaData)) {
            ad.setMediaUrlOrImage(saveBase64Image(mediaData));
        } else {
            ad.setMediaUrlOrImage(mediaData);
        }

        DoctorWebAds saved = adsRepository.save(ad);
        return new DoctorWebAdsDTO(saved.getAdId().toHexString(), saved.getMediaUrlOrImage());
    }

    public Iterable<DoctorWebAdsDTO> getAllAds() {
        List<DoctorWebAdsDTO> list = new ArrayList<>();
        for (DoctorWebAds ad : adsRepository.findAll()) {
            list.add(new DoctorWebAdsDTO(ad.getAdId().toHexString(), ad.getMediaUrlOrImage()));
        }
        return list;
    }

    public Optional<DoctorWebAdsDTO> getAdById(String adId) {
        return adsRepository.findById(new ObjectId(adId)).map(
                ad -> new DoctorWebAdsDTO(ad.getAdId().toHexString(), ad.getMediaUrlOrImage())
        );
    }

    public Optional<DoctorWebAdsDTO> updateAd(String adId, DoctorWebAdsDTO dto) {
        return adsRepository.findById(new ObjectId(adId)).map(ad -> {
            String mediaData = dto.getMediaUrlOrImage();

            if (isVideoUrl(mediaData)) {
                ad.setMediaUrlOrImage(mediaData);
            } else if (isBase64Video(mediaData)) {
                ad.setMediaUrlOrImage(saveBase64Video(mediaData));
            } else if (isBase64Image(mediaData)) {
                ad.setMediaUrlOrImage(saveBase64Image(mediaData));
            } else {
                ad.setMediaUrlOrImage(mediaData);
            }

            DoctorWebAds updated = adsRepository.save(ad);
            return new DoctorWebAdsDTO(updated.getAdId().toHexString(), updated.getMediaUrlOrImage());
        });
    }

    public String deleteAd(String adId) {
        ObjectId id = new ObjectId(adId);
        if (adsRepository.existsById(id)) {
            adsRepository.deleteById(id);
            return "Delete successful";
        }
        return "Data not found";
    }

    // ---------- Helper Methods ----------

    private boolean isBase64Video(String data) {
        if (data == null) return false;
        if (data.startsWith("data:video") && data.contains("base64,")) return true;

        try {
            byte[] bytes = Base64.getDecoder().decode(data.replaceAll("\\s+", ""));
            if (bytes.length < 12) return false;
            String header = new String(bytes, 4, 4);
            return "ftyp".equals(header);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isBase64Image(String data) {
        if (data == null) return false;
        if (data.startsWith("data:image") && data.contains("base64,")) return true;

        try {
            Base64.getDecoder().decode(data.replaceAll("\\s+", ""));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isVideoUrl(String url) {
        return url != null && VIDEO_PATTERN.matcher(url).matches();
    }

    private String saveBase64Video(String base64Data) {
        try {
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            base64Data = base64Data.replaceAll("\\s+", "");

            byte[] videoBytes = Base64.getDecoder().decode(base64Data);
            String fileName = "video_" + System.currentTimeMillis() + ".mp4";

            File directory = new File(videoFolder);
            if (!directory.exists()) directory.mkdirs();

            File file = new File(directory, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(videoBytes);
            }

            return videoBaseUrl + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save base64 video: " + e.getMessage(), e);
        }
    }

    private String saveBase64Image(String base64Data) {
        try {
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            base64Data = base64Data.replaceAll("\\s+", "");

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            String fileName = "image_" + System.currentTimeMillis() + ".png";

            File directory = new File(imageFolder);
            if (!directory.exists()) directory.mkdirs();

            File file = new File(directory, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(imageBytes);
            }

            return imageBaseUrl + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save base64 image: " + e.getMessage(), e);
        }
    }
}
