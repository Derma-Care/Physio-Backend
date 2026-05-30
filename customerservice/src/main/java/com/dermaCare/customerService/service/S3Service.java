package com.dermaCare.customerService.service;

import java.time.Duration;
import java.util.Map;
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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3Service {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    // ── Step 1: Generate presigned PUT URL ──
    public Map<String, String> generatePresignedPutUrl(String folder, String extension) {

        String contentType = resolveContentType(extension);
        String fileName    = folder + "/" + UUID.randomUUID() + "." + extension.toLowerCase();

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

        // ── Replace global endpoint with region-specific endpoint ──
        uploadUrl = uploadUrl.replace(
                "https://" + bucketName + ".s3.amazonaws.com",
                "https://" + bucketName + ".s3." + region + ".amazonaws.com"
        );

        return Map.of(
                "uploadUrl",   uploadUrl,
                "fileKey",     fileName,
                "contentType", contentType
        );
    }

    // ── Step 2: Generate signed GET URL (1-day expiry) ──
    public String generateSignedUrl(String fileKey) {

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(1))
                .getObjectRequest(getRequest)
                .build();

        String signedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();

        // ── Replace global endpoint with region-specific endpoint ──
        signedUrl = signedUrl.replace(
                "https://" + bucketName + ".s3.amazonaws.com",
                "https://" + bucketName + ".s3." + region + ".amazonaws.com"
        );

        return signedUrl;
    }

    // ── Fetch real MIME type + size from S3 via HeadObject ──
    public Map<String, Object> getUploadedFileMeta(String fileKey) {
        try {
            HeadObjectResponse metadata = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileKey)
                            .build()
            );
            return Map.of(
                    "contentType",   metadata.contentType(),
                    "contentLength", metadata.contentLength()
            );
        } catch (NoSuchKeyException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fetch S3 metadata for key: " + fileKey, e);
        }
    }

    // ── Extension → Content-Type ──
    private String resolveContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"         -> "image/png";
            case "webp"        -> "image/webp";
            case "mp4"         -> "video/mp4";
            case "mov"         -> "video/quicktime";
            case "avi"         -> "video/x-msvideo";
            case "mkv"         -> "video/x-matroska";
            case "webm"        -> "video/webm";
            case "mp3"         -> "audio/mpeg";
            case "wav"         -> "audio/wav";
            case "ogg"         -> "audio/ogg";
            case "m4a"         -> "audio/mp4";
            case "aac"         -> "audio/aac";
            case "pdf"         -> "application/pdf";
            case "doc"         -> "application/msword";
            case "docx"        -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default            -> "application/octet-stream";
        };
    }
}