package physiotherapydoctor.service;

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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3Service {

    private static final Duration PUT_URL_EXPIRY = Duration.ofMinutes(15);
    private static final Duration GET_URL_EXPIRY = Duration.ofHours(1);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp",
            "mp4", "mov", "avi", "mkv", "webm",
            "mp3", "wav", "ogg", "m4a", "aac",
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

    /**
     * Step 1 of the presigned upload flow.
     * Returns { uploadUrl, fileKey, contentType }.
     * Frontend uses uploadUrl to PUT the file directly to S3,
     * then saves fileKey in PhysiotherapyRecord.prescriptionPdf.
     */
    public Map<String, String> generatePresignedPutUrl(String folder, String extension) {
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("File extension must not be null or blank");
        }

        String ext         = extension.toLowerCase().trim();
        String contentType = resolveContentType(ext);
        String fileName    = folder + "/" + UUID.randomUUID() + "." + ext;

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PUT_URL_EXPIRY)
                .putObjectRequest(putRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

        return Map.of(
                "uploadUrl",   uploadUrl,
                "fileKey",     fileName,
                "contentType", contentType
        );
    }

    /**
     * Step 2: generate a 1-hour signed GET URL for a stored file key.
     * Use this when returning prescriptionPdf to the frontend so it can render/download.
     */
    public String generateSignedUrl(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key must not be null or blank");
        }

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(GET_URL_EXPIRY)
                .getObjectRequest(getRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * Fetch MIME type and size from S3 via HeadObject.
     * Returns null if the file does not exist.
     */
    public Map<String, Object> getUploadedFileMeta(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("File key must not be null or blank");
        }

        try {
            HeadObjectResponse metadata = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileKey)
                            .build()
            );

            return Map.of(
                    "contentType",    metadata.contentType(),
                    "contentLength",  metadata.contentLength(),
                    "isEncrypted",    metadata.serverSideEncryption() != null,
                    "encryptionType", metadata.serverSideEncryptionAsString() != null
                            ? metadata.serverSideEncryptionAsString()
                            : "NONE"
            );

        } catch (NoSuchKeyException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch metadata from S3 for key: " + fileKey, e);
        }
    }

    // ── content-type resolver ──────────────────────────────────────────────────
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