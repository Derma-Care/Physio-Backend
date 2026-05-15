package com.clinicadmin.service;

import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service {

    private static final int MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("png", "jpg", "jpeg", "mp4", "mp3", "wav", "pdf");

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    public String uploadFile(String folder, String base64Data, String extension) {

        // Return null if no file is provided
        if (base64Data == null || base64Data.isBlank()) {
            return null;
        }

        // Validate file extension
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new RuntimeException("File type not allowed: " + extension);
        }

        // Strip data URI prefix if present (e.g. "data:image/png;base64,...")
        String cleanBase64 = base64Data.contains(",")
                ? base64Data.substring(base64Data.indexOf(',') + 1)
                : base64Data;

        // Decode Base64 string into bytes
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(cleanBase64);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid Base64 data for file upload", e);
        }

        // Enforce file size limit
        if (bytes.length > MAX_FILE_SIZE_BYTES) {
            throw new RuntimeException("File exceeds maximum allowed size of 10MB");
        }

        // Map extension to correct Content-Type
        String contentType = switch (extension.toLowerCase()) {
            case "png"         -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "mp4"         -> "video/mp4";
            case "mp3"         -> "audio/mpeg";
            case "wav"         -> "audio/wav";
            case "pdf"         -> "application/pdf";
            default            -> "application/octet-stream";
        };

        // Generate unique file name
        String fileName = folder + "/"
                + UUID.randomUUID() + "." + extension.toLowerCase();

        // Create S3 upload request
        // NOTE:
        // Do NOT set ACL because this bucket has
        // "Object Ownership = Bucket owner enforced",
        // which disables all ACLs.
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .build();

        // Upload file to S3
        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(bytes)
        );

        // Return public URL
        return "https://" + bucketName
                + ".s3."
                + region
                + ".amazonaws.com/"
                + fileName;
    }
}