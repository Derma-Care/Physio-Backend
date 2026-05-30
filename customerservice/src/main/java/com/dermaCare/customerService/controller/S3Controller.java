package com.dermaCare.customerService.controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dermaCare.customerService.service.S3Service;

@RestController
@RequestMapping("/customer-service")
public class S3Controller {

    @Autowired
    private S3Service s3Service;

    private static final long MB             = 1024 * 1024L;
    private static final long MAX_IMAGE_SIZE =   5 * MB;
    private static final long MAX_VIDEO_SIZE = 100 * MB;

    private static final Set<String> IMAGE_EXTS  = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> VIDEO_EXTS  = Set.of("mp4", "mov", "avi", "mkv", "webm");
    private static final Set<String> IMAGE_MIMES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> VIDEO_MIMES = Set.of(
            "video/mp4", "video/quicktime", "video/x-msvideo",
            "video/x-matroska", "video/webm"
    );

    private record FieldConfig(
            String folder,
            long maxAllowedSize,
            String readableLimit,
            Set<String> allowedExtensions,
            Set<String> allowedMimes
    ) {}

    private FieldConfig resolveConfig(String fieldName) {
        return switch (fieldName) {
            case "beforeImage" -> new FieldConfig("therapy/before-images", MAX_IMAGE_SIZE, "5 MB",   IMAGE_EXTS, IMAGE_MIMES);
            case "afterImage"  -> new FieldConfig("therapy/after-images",  MAX_IMAGE_SIZE, "5 MB",   IMAGE_EXTS, IMAGE_MIMES);
            case "beforeVideo" -> new FieldConfig("therapy/before-videos", MAX_VIDEO_SIZE, "100 MB", VIDEO_EXTS, VIDEO_MIMES);
            case "afterVideo"  -> new FieldConfig("therapy/after-videos",  MAX_VIDEO_SIZE, "100 MB", VIDEO_EXTS, VIDEO_MIMES);
            default            -> null;
        };
    }

    // ── GET /customer-service/api/s3/upload-url
    //      ?fieldName=beforeImage&fileSize=204800&extension=jpg
    @GetMapping("/s3/upload-url")
    public ResponseEntity<?> getUploadUrl(
            @RequestParam String fieldName,
            @RequestParam(required = false, defaultValue = "0") long fileSize,
            @RequestParam(required = false) String extension) {

        FieldConfig config = resolveConfig(fieldName);
        if (config == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "valid", false,
                    "error", "Unknown fieldName: '" + fieldName + "'. Allowed: beforeImage, afterImage, beforeVideo, afterVideo"
            ));
        }

        // Extension check
        if (extension != null && !extension.isBlank()) {
            extension = extension.toLowerCase().trim();
            if (!config.allowedExtensions().contains(extension)) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of(
                        "valid", false,
                        "uploadedType", extension,
                        "allowedTypes", config.allowedExtensions(),
                        "error", String.format("Extension '.%s' not allowed for '%s'. Accepted: %s",
                                extension, fieldName, config.allowedExtensions())
                ));
            }
        } else {
            extension = config.allowedExtensions().iterator().next();
        }

        // Size check
        if (fileSize > 0 && fileSize > config.maxAllowedSize()) {
            String uploadedMB = String.format("%.2f MB", fileSize / (double) MB);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                    "valid", false,
                    "uploadedSize", uploadedMB,
                    "allowedSize",  config.readableLimit(),
                    "error", String.format("File %s exceeds max %s for '%s'.",
                            uploadedMB, config.readableLimit(), fieldName)
            ));
        }

        Map<String, String> result = s3Service.generatePresignedPutUrl(config.folder(), extension);
        return ResponseEntity.ok(result);
    }

    // ── GET /customer-service/api/s3/validate-upload
    //      ?fileKey=therapy/before-images/uuid.jpg&fieldName=beforeImage
    @GetMapping("/s3/validate-upload")
    public ResponseEntity<?> validateUpload(
            @RequestParam String fileKey,
            @RequestParam String fieldName) {

        FieldConfig config = resolveConfig(fieldName);
        if (config == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "valid", false, "error", "Unknown fieldName: '" + fieldName + "'"
            ));
        }

        String uploadedExt = fileKey.contains(".")
                ? fileKey.substring(fileKey.lastIndexOf('.') + 1).toLowerCase().trim()
                : "";

        if (uploadedExt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "valid", false, "error", "Cannot determine extension from key: " + fileKey
            ));
        }

        if (!config.allowedExtensions().contains(uploadedExt)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of(
                    "valid", false,
                    "uploadedType", uploadedExt,
                    "allowedTypes", config.allowedExtensions(),
                    "error", String.format("'.%s' not allowed for '%s'. Expected: %s",
                            uploadedExt, fieldName, config.allowedExtensions())
            ));
        }

        Map<String, Object> s3Meta = s3Service.getUploadedFileMeta(fileKey);
        if (s3Meta == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "valid", false, "error", "File not found in S3: " + fileKey
            ));
        }

        String contentType  = (String) s3Meta.getOrDefault("contentType", "");
        long   uploadedSize = (long)   s3Meta.getOrDefault("contentLength", 0L);
        String mimeOnly     = contentType.split(";")[0].trim().toLowerCase();

        if (!config.allowedMimes().contains(mimeOnly)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of(
                    "valid", false,
                    "uploadedMime", mimeOnly,
                    "allowedMimes", config.allowedMimes(),
                    "error", String.format("MIME '%s' not allowed for '%s'.", mimeOnly, fieldName)
            ));
        }

        if (uploadedSize > config.maxAllowedSize()) {
            String uploadedMB = String.format("%.2f MB", uploadedSize / (double) MB);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                    "valid", false,
                    "uploadedSize", uploadedMB,
                    "allowedSize",  config.readableLimit(),
                    "error", String.format("File %s exceeds max %s for '%s'.",
                            uploadedMB, config.readableLimit(), fieldName)
            ));
        }

        String uploadedMB = String.format("%.2f MB", uploadedSize / (double) MB);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "uploadedType", uploadedExt,
                "uploadedMime", mimeOnly,
                "uploadedSize", uploadedMB,
                "allowedSize",  config.readableLimit(),
                "message", String.format("File '.%s' (%s, %s) is valid for '%s'.",
                        uploadedExt, mimeOnly, uploadedMB, fieldName)
        ));
    }

    // ── GET /customer-service/api/s3/signed-url?fileKey=therapy/before-images/uuid.jpg
    @GetMapping("/s3/signed-url")
    public ResponseEntity<String> getSignedUrl(@RequestParam String fileKey) {
        return ResponseEntity.ok(s3Service.generateSignedUrl(fileKey));
    }
}