package com.clinicadmin.service;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3Service {

    // ─────────────────────────────────────────────
    // Used ONLY by the legacy base64 upload flow
    // ─────────────────────────────────────────────
    private static final int MAX_LEGACY_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // images
            "jpg", "jpeg", "png", "webp",
            // videos
            "mp4", "mov", "avi", "mkv", "webm",
            // audio
            "mp3", "wav", "ogg", "m4a", "aac",
            // docs
            "pdf", "doc", "docx"
    );

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Autowired
    private S3Presigner s3Presigner;
//
//    // ─────────────────────────────────────────────
//    // LEGACY FLOW: frontend sends base64 → server uploads
//    // ─────────────────────────────────────────────
//    public String uploadFile(String folder, String base64Data, String extension) {
//
//        if (base64Data == null || base64Data.isBlank()) {
//            return null;
//        }
//
//        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
//            throw new RuntimeException("File type not allowed: " + extension);
//        }
//
//        String cleanBase64 = base64Data.contains(",")
//                ? base64Data.substring(base64Data.indexOf(',') + 1)
//                : base64Data;
//
//        byte[] bytes;
//        try {
//            bytes = Base64.getDecoder().decode(cleanBase64);
//        } catch (IllegalArgumentException e) {
//            throw new RuntimeException("Invalid Base64 data for file upload", e);
//        }
//
//        if (bytes.length > MAX_LEGACY_FILE_SIZE) {
//            throw new RuntimeException("File exceeds maximum allowed size of 10 MB");
//        }
//
//        String contentType = resolveContentType(extension);
//        String fileName    = folder + "/" + UUID.randomUUID() + "." + extension.toLowerCase();
//
//        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                .bucket(bucketName)
//                .key(fileName)
//                .contentType(contentType)
//                .build();
//
//        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
//
//        return generateSignedUrl(fileName);
//    }

    // ─────────────────────────────────────────────
    // NEW FLOW (Step 1): Generate presigned PUT URL
    // → frontend uploads directly to S3
    // FIX: now returns contentType so frontend uses
    // the EXACT same value in Content-Type header
    // preventing SignatureDoesNotMatch error
    // ─────────────────────────────────────────────
    public Map<String, String> generatePresignedPutUrl(String folder, String extension) {

        String contentType = resolveContentType(extension);
        String fileName    = folder + "/" + UUID.randomUUID() + "." + extension.toLowerCase();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)   // ← lock content-type back into signature
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest)
                .build();

        String uploadUrl = s3Presigner
                .presignPutObject(presignRequest)
                .url()
                .toString();

        return Map.of(
                "uploadUrl",   uploadUrl,
                "fileKey",     fileName,
                "contentType", contentType  // ← frontend MUST use this exact value
        );
    }

    // ─────────────────────────────────────────────
    // NEW FLOW (Step 2): Generate signed GET URL
    // ─────────────────────────────────────────────
    public String generateSignedUrl(String fileName) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(1))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner
                .presignGetObject(presignRequest)
                .url()
                .toString();
    }

    // ─────────────────────────────────────────────
    // Fetch real MIME type + size from S3 via
    // a single HeadObject call.
    // Returns null if the file does not exist.
    // ─────────────────────────────────────────────
    public Map<String, Object> getUploadedFileMeta(String fileKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            HeadObjectResponse metadata = s3Client.headObject(headRequest);

            return Map.of(
                    "contentType",   metadata.contentType(),    // e.g. "video/mp4"
                    "contentLength", metadata.contentLength()   // e.g. 52428800L
            );

        } catch (NoSuchKeyException e) {
            // File does not exist in S3 — caller handles with 404
            return null;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fetch metadata from S3 for key: " + fileKey, e
            );
        }
    }

    // ─────────────────────────────────────────────
    // SHARED HELPER: extension → Content-Type
    // ─────────────────────────────────────────────
    private String resolveContentType(String extension) {
        return switch (extension.toLowerCase()) {
            // images
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"         -> "image/png";
            case "webp"        -> "image/webp";
            // videos
            case "mp4"         -> "video/mp4";
            case "mov"         -> "video/quicktime";
            case "avi"         -> "video/x-msvideo";
            case "mkv"         -> "video/x-matroska";
            case "webm"        -> "video/webm";
            // audio
            case "mp3"         -> "audio/mpeg";
            case "wav"         -> "audio/wav";
            case "ogg"         -> "audio/ogg";
            case "m4a"         -> "audio/mp4";
            case "aac"         -> "audio/aac";
            // docs
            case "pdf"         -> "application/pdf";
            case "doc"         -> "application/msword";
            case "docx"        -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            // fallback
            default            -> "application/octet-stream";
        };
    }
}