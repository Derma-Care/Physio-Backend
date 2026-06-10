package physiotherapydoctor.controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import physiotherapydoctor.service.S3Service;

@RestController
@RequestMapping("/physiotherapy-doctor")
public class S3Controller {

    @Autowired
    private S3Service s3Service;

    // ── size limits ────────────────────────────────────────────────────────────
    private static final long MB             = 1024 * 1024L;
    private static final long MAX_IMAGE_SIZE =   5 * MB;
    private static final long MAX_PDF_SIZE   =  10 * MB;

    // ── allowed extension / MIME sets ──────────────────────────────────────────
    private static final Set<String> IMAGE_EXTS  = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> DOC_EXTS    = Set.of("pdf", "doc", "docx");
    private static final Set<String> IMAGE_MIMES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> DOC_MIMES   = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    // ── field config ───────────────────────────────────────────────────────────
    private record FieldConfig(
            String      folder,
            long        maxAllowedSize,
            String      readableLimit,
            Set<String> allowedExtensions,
            Set<String> allowedMimes
    ) {}

    private FieldConfig resolveConfig(String fieldName) {
        return switch (fieldName) {

            // Prescription PDF / image (the main use-case from PhysiotherapyRecord.prescriptionPdf)
            case "prescriptionPdf" -> new FieldConfig(
                    "physio-prescriptions",
                    MAX_PDF_SIZE,
                    "10 MB",
                    Stream.concat(IMAGE_EXTS.stream(), DOC_EXTS.stream())
                          .collect(Collectors.toUnmodifiableSet()),
                    Stream.concat(IMAGE_MIMES.stream(), DOC_MIMES.stream())
                          .collect(Collectors.toUnmodifiableSet())
            );

            // Patient photo stored on the record
            case "patientPhoto" -> new FieldConfig(
                    "physio-patient-photos",
                    MAX_IMAGE_SIZE,
                    "5 MB",
                    IMAGE_EXTS,
                    IMAGE_MIMES
            );

            // Consent document
            case "consentPdf" -> new FieldConfig(
                    "physio-consent-pdfs",
                    MAX_PDF_SIZE,
                    "10 MB",
                    DOC_EXTS,
                    DOC_MIMES
            );

            // Exercise / therapy media (images only – extend to video if needed)
            case "exerciseImage" -> new FieldConfig(
                    "physio-exercise-images",
                    MAX_IMAGE_SIZE,
                    "5 MB",
                    IMAGE_EXTS,
                    IMAGE_MIMES
            );

            // Investigation / report uploads
            case "report" -> new FieldConfig(
                    "physio-reports",
                    MAX_PDF_SIZE,
                    "10 MB",
                    Stream.concat(IMAGE_EXTS.stream(), DOC_EXTS.stream())
                          .collect(Collectors.toUnmodifiableSet()),
                    Stream.concat(IMAGE_MIMES.stream(), DOC_MIMES.stream())
                          .collect(Collectors.toUnmodifiableSet())
            );

            default -> null;
        };
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /physio-doctor/api/s3/upload-url
    //   ?fieldName=prescriptionPdf
    //   &fileSize=2048000          (bytes, optional)
    //   &extension=pdf             (optional, defaults to first allowed)
    //
    // Returns { uploadUrl, fileKey, contentType }
    // Frontend PUT-uploads the file to uploadUrl, then saves fileKey
    // in PhysiotherapyRecord.prescriptionPdf.
    // ──────────────────────────────────────────────────────────────────────────
    @GetMapping("/s3/upload-url")
    public ResponseEntity<?> getUploadUrl(
            @RequestParam String fieldName,
            @RequestParam(required = false, defaultValue = "0") long fileSize,
            @RequestParam(required = false) String extension) {

        FieldConfig config = resolveConfig(fieldName);
        if (config == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "valid", false,
                    "error", "Unknown fieldName: '" + fieldName + "'."
                           + " Allowed: prescriptionPdf, patientPhoto, consentPdf, exerciseImage, report"
            ));
        }

        // Extension validation
        if (extension != null && !extension.isBlank()) {
            String ext = extension.toLowerCase().trim();
            if (!config.allowedExtensions().contains(ext)) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of(
                        "valid",        false,
                        "uploadedType", ext,
                        "allowedTypes", config.allowedExtensions(),
                        "error", String.format(
                                "Wrong file type '.%s' for '%s'. Accepted: %s",
                                ext, fieldName, config.allowedExtensions())
                ));
            }
            extension = ext;
        } else {
            extension = config.allowedExtensions().iterator().next();
        }

        // File size validation
        if (fileSize > 0 && fileSize > config.maxAllowedSize()) {
            String uploadedMB = String.format("%.2f MB", fileSize / (double) MB);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                    "valid",        false,
                    "uploadedSize", uploadedMB,
                    "allowedSize",  config.readableLimit(),
                    "error", String.format(
                            "File %s exceeds the maximum allowed size of %s for '%s'.",
                            uploadedMB, config.readableLimit(), fieldName)
            ));
        }

        Map<String, String> response = s3Service.generatePresignedPutUrl(config.folder(), extension);
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /physio-doctor/api/s3/validate-upload
    //   ?fileKey=physio-prescriptions/uuid.pdf
    //   &fieldName=prescriptionPdf
    //
    // Call AFTER the frontend finishes uploading to S3.
    // Verifies MIME type and size from S3 HeadObject.
    // ──────────────────────────────────────────────────────────────────────────
    @GetMapping("/s3/validate-upload")
    public ResponseEntity<?> validateUpload(
            @RequestParam String fileKey,
            @RequestParam String fieldName) {

        FieldConfig config = resolveConfig(fieldName);
        if (config == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "valid", false,
                    "error", "Unknown fieldName: '" + fieldName + "'."
            ));
        }

        // Extension from fileKey
        String uploadedExt = fileKey.contains(".")
                ? fileKey.substring(fileKey.lastIndexOf('.') + 1).toLowerCase().trim()
                : "";

        if (uploadedExt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "valid", false,
                    "error", "Could not determine file extension from key: " + fileKey
            ));
        }

        if (!config.allowedExtensions().contains(uploadedExt)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of(
                    "valid",        false,
                    "uploadedType", uploadedExt,
                    "allowedTypes", config.allowedExtensions(),
                    "error", String.format(
                            "'.%s' is not allowed for '%s'. Expected: %s",
                            uploadedExt, fieldName, config.allowedExtensions())
            ));
        }

        // Fetch S3 metadata
        Map<String, Object> s3Meta = s3Service.getUploadedFileMeta(fileKey);
        if (s3Meta == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "valid", false,
                    "error", "File not found in S3 for key: " + fileKey
            ));
        }

        String contentType  = (String) s3Meta.getOrDefault("contentType",   "");
        long   uploadedSize = (long)   s3Meta.getOrDefault("contentLength", 0L);

        if (contentType == null || contentType.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "valid", false,
                    "error", "Could not read content-type from S3 for key: " + fileKey
            ));
        }

        String mimeOnly = contentType.split(";")[0].trim().toLowerCase();

        if (!config.allowedMimes().contains(mimeOnly)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of(
                    "valid",        false,
                    "uploadedMime", mimeOnly,
                    "allowedMimes", config.allowedMimes(),
                    "error", String.format(
                            "MIME type '%s' is not allowed for '%s'. Expected: %s",
                            mimeOnly, fieldName, config.allowedMimes())
            ));
        }

        if (uploadedSize > config.maxAllowedSize()) {
            String uploadedMB = String.format("%.2f MB", uploadedSize / (double) MB);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                    "valid",        false,
                    "uploadedSize", uploadedMB,
                    "allowedSize",  config.readableLimit(),
                    "error", String.format(
                            "File %s exceeds maximum allowed size of %s for '%s'.",
                            uploadedMB, config.readableLimit(), fieldName)
            ));
        }

        String uploadedMB = String.format("%.2f MB", uploadedSize / (double) MB);
        return ResponseEntity.ok(Map.of(
                "valid",        true,
                "uploadedType", uploadedExt,
                "uploadedMime", mimeOnly,
                "uploadedSize", uploadedMB,
                "allowedSize",  config.readableLimit(),
                "message", String.format(
                        "File '.%s' (%s, %s) is valid for '%s'.",
                        uploadedExt, mimeOnly, uploadedMB, fieldName)
        ));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /physio-doctor/api/s3/signed-url
    //   ?fileKey=physio-prescriptions/uuid.pdf
    //
    // Returns a 1-hour signed GET URL.
    // Call this when the frontend needs to display / download the prescription.
    // ──────────────────────────────────────────────────────────────────────────
    @GetMapping("/s3/signed-url")
    public ResponseEntity<String> getSignedUrl(@RequestParam String fileKey) {
        String signedUrl = s3Service.generateSignedUrl(fileKey);
        return ResponseEntity.ok(signedUrl);
    }
}