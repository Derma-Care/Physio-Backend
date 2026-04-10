package com.AdminService.service;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.AdminService.dto.DoctorVerticalAdsDto;
import com.AdminService.dto.DoctorWebAdsDTO;
import com.AdminService.entity.DoctorWebVerticalAdsEntity;
import com.AdminService.repository.DoctorWebVerticalAdsRepository;
@Service
public class DoctorWebVerticalAdsServiceImpl {

    @Autowired
    private DoctorWebVerticalAdsRepository doctorWebVerticalAdsRepository;

    @Value("${app.video.folder}")
    private String videoFolder;

    @Value("${app.video.base-url}")
    private String videoBaseUrl;

    @Value("${app.image.folder}")
    private String imageFolder;

    @Value("${app.image.base-url}")
    private String imageBaseUrl;

    private static final Pattern VIDEO_PATTERN = Pattern.compile(
            ".*\\.(mp4|avi|mov|wmv|flv|mkv|webm|ogg|m4v)$",
            Pattern.CASE_INSENSITIVE
    );

    public DoctorWebAdsDTO createAd(DoctorVerticalAdsDto dto) {
        DoctorWebVerticalAdsEntity ad = new DoctorWebVerticalAdsEntity();
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

        DoctorWebVerticalAdsEntity saved = doctorWebVerticalAdsRepository.save(ad);
        return new DoctorWebAdsDTO(saved.getAdId(), saved.getMediaUrlOrImage());
    }

    public Iterable<DoctorVerticalAdsDto> getAllAds() {
        List<DoctorVerticalAdsDto> list = new ArrayList<>();
        for (DoctorWebVerticalAdsEntity ad : doctorWebVerticalAdsRepository.findAll()) {
            list.add(new DoctorVerticalAdsDto(ad.getAdId(), ad.getMediaUrlOrImage()));
        }
        return list;
    }

    public Optional<DoctorVerticalAdsDto> getAdById(String adId) {
        return doctorWebVerticalAdsRepository.findById(adId).map(
                ad -> new DoctorVerticalAdsDto(ad.getAdId(), ad.getMediaUrlOrImage())
        );
    }

    public Optional<DoctorVerticalAdsDto> updateAd(String adId, DoctorVerticalAdsDto dto) {
        return doctorWebVerticalAdsRepository.findById(adId).map(ad -> {
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

            DoctorWebVerticalAdsEntity updated = doctorWebVerticalAdsRepository.save(ad);
            return new DoctorVerticalAdsDto(updated.getAdId(), updated.getMediaUrlOrImage());
        });
    }

    public String deleteAd(String adId) {
        if (doctorWebVerticalAdsRepository.existsById(adId)) {
            doctorWebVerticalAdsRepository.deleteById(adId);
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
            try (FileOutputStream fos = new FileOutputStream(file)){
                fos.write(imageBytes);
            }
            return imageBaseUrl + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save base64 image: " + e.getMessage(), e);
        }
    }
}
