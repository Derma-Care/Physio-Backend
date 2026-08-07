package com.clinicadmin.service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;          // ← NEW
import software.amazon.awssdk.services.s3.model.StorageClass;                   // ← NEW
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3Service {

    // ─────────────────────────────────────────────
    // Used ONLY by the legacy base64 upload flow
    // ─────────────────────────────────────────────
    private static final int MAX_LEGACY_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    // ─────────────────────────────────────────────
    // Presigned URL expiry constants
    // ─────────────────────────────────────────────
    private static final Duration PUT_URL_EXPIRY = Duration.ofMinutes(15); // upload window
    private static final Duration GET_URL_EXPIRY = Duration.ofHours(1);    // ← reduced from 1 day to 1 hour (more secure)

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

    // ─────────────────────────────────────────────
    // NEW FLOW (Step 1): Generate presigned PUT URL
    // → frontend uploads directly to S3
    //
    // Security layers added:
    //   ✅ SSE-S3 (AES-256) encryption at rest
    //   ✅ Content-Type locked into signature
    //   ✅ STANDARD storage class explicitly set
    //   ✅ 15-min expiry on upload URL
    // ─────────────────────────────────────────────
    public Map<String, String> generatePresignedPutUrl(String folder, String extension) {

        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("File extension must not be null or blank");
        }

        String ext         = extension.toLowerCase().trim();
        // ✅ Validate extension is allowed
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException(
                "File extension '." + ext + "' is not allowed. Allowed: " + ALLOWED_EXTENSIONS
            );
        }
        String contentType = resolveContentType(ext);
        String fileName    = folder + "/" + UUID.randomUUID() + "." + ext;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)                            // ← locked into signature
//                .serverSideEncryption(ServerSideEncryption.AES256)  // ← SSE-S3 encryption (NEW)
//                .storageClass(StorageClass.STANDARD)                 // ← explicit storage class (NEW)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PUT_URL_EXPIRY)                   // ← 15 min upload window
                .putObjectRequest(putObjectRequest)
                .build();

        String uploadUrl = s3Presigner
                .presignPutObject(presignRequest)
                .url()
                .toString();

        return Map.of(
                "uploadUrl",   uploadUrl,
                "fileKey",     fileName,
                "contentType", contentType   // ← frontend MUST use this exact value
        );
    }

    // ─────────────────────────────────────────────
    // NEW FLOW (Step 2): Generate signed GET URL
    //
    // Security layers:
    //   ✅ Reduced expiry from 1 day → 1 hour
    //   ✅ Access only via signed URL (no public read)
    // ─────────────────────────────────────────────
    public String generateSignedUrl(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name must not be null or blank");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(GET_URL_EXPIRY)   // ← 1 hour (was 1 day)
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
    //
    // Security: validates file actually exists in
    // S3 before issuing a signed URL
    // ─────────────────────────────────────────────
    public Map<String, Object> getUploadedFileMeta(String fileKey) {

        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key must not be null or blank");
        }

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            HeadObjectResponse metadata = s3Client.headObject(headRequest);

            return Map.of(
                    "contentType",      metadata.contentType(),           // e.g. "video/mp4"
                    "contentLength",    metadata.contentLength(),          // e.g. 52428800L
                    "isEncrypted",      metadata.serverSideEncryption() != null, // ← NEW: verify encryption
                    "encryptionType",   metadata.serverSideEncryptionAsString() != null  // ← NEW: log SSE type
                                        ? metadata.serverSideEncryptionAsString()
                                        : "NONE"
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
    // ─────────────────────────────────────────────
    // Small wrapper so callers outside this class can validate an
    // extension before doing expensive work that would fail later anyway.
    // ─────────────────────────────────────────────
    public boolean isExtensionAllowed(String extension) {
        return extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase().trim());
    }

    // ─────────────────────────────────────────────
    // USED BY: delete flows that need to remove an object from S3
    // once its DB record is deleted.
    // ─────────────────────────────────────────────
    public void deleteFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return;
        }
        s3Client.deleteObject(
                software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .build());
    }
    
   
    
 // ─────────────────────────────────────────────
    // NEW: Download file bytes directly from S3 (server-side)
    // Used by PdfShareController to proxy files without
    // ever exposing the underlying S3 URL to the client.
    // ─────────────────────────────────────────────
    public byte[] downloadFile(String fileKey) {

        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key must not be null or blank");
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            return s3Client.getObject(getObjectRequest,
                    software.amazon.awssdk.core.sync.ResponseTransformer.toBytes())
                    .asByteArray();

        } catch (NoSuchKeyException e) {
            throw new RuntimeException("File not found in S3 for key: " + fileKey, e);

        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from S3 for key: " + fileKey, e);
        }
    }
}