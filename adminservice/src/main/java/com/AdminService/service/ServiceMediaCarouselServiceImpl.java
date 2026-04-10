package com.AdminService.service;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.AdminService.dto.ServiceMediaCarouselDTO;
import com.AdminService.entity.ServiceMediaCarousel;
import com.AdminService.repository.ServiceMediaCarousalRepository;

@Service
public class ServiceMediaCarouselServiceImpl {

    private final ServiceMediaCarousalRepository mediaRepository;

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

    public ServiceMediaCarouselServiceImpl(ServiceMediaCarousalRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    public ServiceMediaCarouselDTO createMedia(ServiceMediaCarouselDTO mediaDTO) {
        ServiceMediaCarousel media = new ServiceMediaCarousel();
        String mediaData = mediaDTO.getMediaUrlOrImage();

        if (isVideoUrl(mediaData)) {
            media.setMediaUrlOrImage(mediaData);
        } else if (isBase64Video(mediaData)) {
            media.setMediaUrlOrImage(saveBase64Video(mediaData));
        } else if (isBase64Image(mediaData)) {
            media.setMediaUrlOrImage(saveBase64Image(mediaData));
        } else {
            media.setMediaUrlOrImage(mediaData);
        }

        ServiceMediaCarousel saved = mediaRepository.save(media);
        return new ServiceMediaCarouselDTO(
            saved.getCarouselId().toHexString(),
            saved.getMediaUrlOrImage()
        );
    }

    public Iterable<ServiceMediaCarouselDTO> getAllMedia() {
        List<ServiceMediaCarouselDTO> list = new ArrayList<>();
        for (ServiceMediaCarousel media : mediaRepository.findAll()) {
            list.add(new ServiceMediaCarouselDTO(
                media.getCarouselId().toHexString(),
                media.getMediaUrlOrImage()
            ));
        }
        return list;
    }

    public Optional<ServiceMediaCarouselDTO> getMediaById(String carouselId) {
        return mediaRepository.findById(new ObjectId(carouselId)).map(
            media -> new ServiceMediaCarouselDTO(
                media.getCarouselId().toHexString(),
                media.getMediaUrlOrImage()
            )
        );
    }

    public Optional<ServiceMediaCarouselDTO> updateMediaOptional(String carouselId, ServiceMediaCarouselDTO mediaDTO) {
        return mediaRepository.findById(new ObjectId(carouselId)).map(media -> {
            String mediaData = mediaDTO.getMediaUrlOrImage();

            if (isVideoUrl(mediaData)) {
                media.setMediaUrlOrImage(mediaData);
            } else if (isBase64Video(mediaData)) {
                media.setMediaUrlOrImage(saveBase64Video(mediaData));
            } else if (isBase64Image(mediaData)) {
                media.setMediaUrlOrImage(saveBase64Image(mediaData));
            } else {
                media.setMediaUrlOrImage(mediaData);
            }

            ServiceMediaCarousel updated = mediaRepository.save(media);
            return new ServiceMediaCarouselDTO(
                updated.getCarouselId().toHexString(),
                updated.getMediaUrlOrImage()
            );
        });
    }

    public String deleteMedia(String carouselId) {
        ObjectId id = new ObjectId(carouselId);
        if (mediaRepository.existsById(id)) {
            mediaRepository.deleteById(id);
            return "Delete successful";
        }
        return "Data not found";
    }

    private boolean isVideoUrl(String url) {
        return url != null && VIDEO_PATTERN.matcher(url).matches();
    }

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
