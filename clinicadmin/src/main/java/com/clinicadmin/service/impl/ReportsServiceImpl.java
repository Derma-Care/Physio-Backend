package com.clinicadmin.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.BookingResponse;
import com.clinicadmin.dto.ReportsDTO;
import com.clinicadmin.dto.ReportsDtoList;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.entity.Reports;
import com.clinicadmin.entity.ReportsList;
import com.clinicadmin.feignclient.BookingFeign;
import com.clinicadmin.repository.ReportsRepository;
import com.clinicadmin.service.ReportsService;
import com.clinicadmin.service.S3Service;

import feign.FeignException;

@Service
public class ReportsServiceImpl implements ReportsService {

    @Autowired
    private ReportsRepository reportsRepository;

    @Autowired
    private BookingFeign bookingFeign;

    @Autowired
    private S3Service s3Service; // ✅ S3 integration

    // ─────────────────────────────────────────────────────────────────
    // HELPER: Reports entity → ReportsDTO with fresh signed URLs
    // Stored reportFile has S3 keys → convert to signed GET URLs
    // ─────────────────────────────────────────────────────────────────
    private ReportsDTO toResponseDTO(Reports report) {
        List<String> signedUrls = new ArrayList<>();

        if (report.getReportFile() != null) {
            for (String key : report.getReportFile()) {
                if (key != null && !key.isBlank()) {
                    // ✅ Generate fresh 7-day signed URL from stored S3 key
                    signedUrls.add(s3Service.generateSignedUrl(key));
                }
            }
        }

        ReportsDTO dto = new ReportsDTO();
        dto.setBookingId(report.getBookingId());
        dto.setPatientId(report.getPatientId());
        dto.setCustomerMobileNumber(report.getCustomerMobileNumber());
        dto.setReportName(report.getReportName());
        dto.setReportDate(report.getReportDate());
        dto.setReportStatus(report.getReportStatus());
        dto.setReportType(report.getReportType());
        dto.setReportFile(signedUrls); // ✅ signed URLs returned to frontend
        return dto;
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER: ReportsList entity → ReportsDtoList with signed URLs
    // ─────────────────────────────────────────────────────────────────
    private ReportsDtoList toResponseDtoList(ReportsList entity) {
        List<ReportsDTO> dtos = new ArrayList<>();

        if (entity.getReportsList() != null) {
            for (Reports r : entity.getReportsList()) {
                dtos.add(toResponseDTO(r));
            }
        }

        ReportsDtoList dtoList = new ReportsDtoList();

        dtoList.setId(entity.getId());

        dtoList.setCustomerId(entity.getCustomerId());
        dtoList.setPatientId(entity.getPatientId());
        dtoList.setReportsList(dtos);
        return dtoList;
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER: Validate S3 keys sent by frontend
    // Returns error message string if invalid, null if all good
    // ─────────────────────────────────────────────────────────────────
    private String validateS3Keys(List<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) return null;

        for (String key : fileKeys) {
            if (key == null || key.isBlank()) {
                return "File key cannot be null or blank";
            }
            // ✅ Check file actually exists in S3 (HeadObject call)
            var meta = s3Service.getUploadedFileMeta(key);
            if (meta == null) {
                return "File not found in S3 for key: " + key
                        + ". Please upload via presigned URL first.";
            }
        }
        return null; // all valid
    }

    // ─────────────────────────────────────────────────────────────────
    // ADD REPORTS
    // Flow: Frontend → GET /api/s3/upload-url (fieldName=report)
    //              → PUT file to S3 presigned URL
    //              → POST here with fileKeys in reportFile field
    // ─────────────────────────────────────────────────────────────────
    @Override
    public Response saveReports(ReportsDtoList dto) {
        try {
            if (dto == null || dto.getReportsList() == null || dto.getReportsList().isEmpty()) {
                return Response.builder()
                        .success(false)
                        .message("Invalid request: no report data provided")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .data(null)
                        .build();
            }

            ReportsList reportsList = new ReportsList();
            List<Reports> reports = new ArrayList<>();
            String bookingId = null;

            for (ReportsDTO reportDTO : dto.getReportsList()) {

                // ── Validate bookingId ──────────────────────────────
                if (reportDTO.getBookingId() == null || reportDTO.getBookingId().isEmpty()) {
                    return Response.builder()
                            .success(false)
                            .message("Booking ID cannot be null or empty in report")
                            .status(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build();
                }

                bookingId = reportDTO.getBookingId();

                // ── Validate S3 keys (reportFile now holds S3 keys) ─
                String keyError = validateS3Keys(reportDTO.getReportFile());
                if (keyError != null) {
                    return Response.builder()
                            .success(false)
                            .message(keyError)
                            .status(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build();
                }

                // ✅ Build entity — store S3 keys directly (no base64 decode)
                Reports report = Reports.builder()
                        .bookingId(reportDTO.getBookingId())
                        .patientId(reportDTO.getPatientId())
                        .reportName(reportDTO.getReportName())
                        .reportDate(reportDTO.getReportDate())
                        .reportStatus(reportDTO.getReportStatus())
                        .reportType(reportDTO.getReportType())
                        .customerMobileNumber(reportDTO.getCustomerMobileNumber())
                        .reportFile(reportDTO.getReportFile()) // ✅ S3 keys stored as-is
                        .build();

                reports.add(report);
            }

            // ── Fetch booking & sync ────────────────────────────────
            ResponseEntity<ResponseStructure<BookingResponse>> response =
                    bookingFeign.getBookedService(bookingId);

            BookingResponse bookingData =
                    response.getBody() != null ? response.getBody().getData() : null;

            if (bookingData != null) {
                List<ReportsDtoList> existingReports = bookingData.getReports();
                if (existingReports == null) existingReports = new ArrayList<>();
                existingReports.add(dto);
                bookingData.setReports(existingReports);

                bookingData.setCurrentStatus(null);
                bookingData.setListOfConsultationFee(null);
                // Set IDs from booking service

                dto.setCustomerId(bookingData.getCustomerId());
                dto.setPatientId(bookingData.getPatientId());

                bookingFeign.updateAppointmentBasedOnBookingId(bookingData);
            }

            // ── Save to MongoDB ─────────────────────────────────────
            reportsList.setCustomerId(dto.getCustomerId());
            reportsList.setPatientId(dto.getPatientId());
            reportsList.setReportsList(reports);

            ReportsList saved = reportsRepository.save(reportsList);

            // ✅ Return with signed URLs in reportFile
            return Response.builder()
                    .success(true)
                    .data(toResponseDtoList(saved))
                    .message("Report uploaded successfully")
                    .status(HttpStatus.CREATED.value())
                    .build();

        } catch (FeignException e) {
            return Response.builder()
                    .success(false)
                    .data(null)
                    .message("Error communicating with Booking Service: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        } catch (Exception e) {
            return Response.builder()
                    .success(false)
                    .data(null)
                    .message("Unexpected error: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET REPORTS BY BOOKING ID
    // ─────────────────────────────────────────────────────────────────
    @Override
    public Response getReportsByBookingId(String bookingId) {
        Response res = new Response();
        try {
            List<ReportsList> reportsListData =
                    reportsRepository.findByReportsListBookingId(bookingId);

            if (reportsListData != null && !reportsListData.isEmpty()) {
                List<ReportsDtoList> result = reportsListData.stream()
                        .map(this::toResponseDtoList) // ✅ signed URLs generated here
                        .collect(Collectors.toList());

                res.setSuccess(true);
                res.setStatus(HttpStatus.OK.value());
                res.setMessage("Reports fetched successfully for given bookingId");
                res.setData(result);
            } else {
                res.setSuccess(true);
                res.setStatus(HttpStatus.OK.value());
                res.setMessage("No reports found for given bookingId");
                res.setData(Collections.emptyList());
            }

        } catch (Exception e) {
            res.setSuccess(false);
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            res.setMessage("Error fetching reports: " + e.getMessage());
            res.setData(null);
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────
    // GET ALL REPORTS
    // ─────────────────────────────────────────────────────────────────
    @Override
    public Response getAllReports() {
        Response res = new Response();
        try {
            List<ReportsList> reportList = reportsRepository.findAll();

            if (reportList != null && !reportList.isEmpty()) {
                List<ReportsDtoList> result = reportList.stream()
                        .map(this::toResponseDtoList) // ✅ signed URLs generated here
                        .collect(Collectors.toList());

                res.setSuccess(true);
                res.setStatus(HttpStatus.OK.value());
                res.setMessage("All reports fetched successfully");
                res.setData(result);
            } else {
                res.setSuccess(true);
                res.setStatus(HttpStatus.OK.value());
                res.setMessage("No reports found");
                res.setData(Collections.emptyList());
            }

        } catch (Exception e) {
            res.setSuccess(false);
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            res.setMessage("Error fetching all reports: " + e.getMessage());
            res.setData(null);
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────
    // GET REPORTS BY CUSTOMER ID
    // ─────────────────────────────────────────────────────────────────
    @Override
    public Response getReportsByCustomerId(String customerId) {
        Response res = new Response();
        try {
            List<ReportsList> reportsListData =
                    reportsRepository.findByCustomerId(customerId);

            if (reportsListData != null && !reportsListData.isEmpty()) {
                List<ReportsDtoList> result = reportsListData.stream()
                        .map(this::toResponseDtoList) // ✅ signed URLs generated here
                        .collect(Collectors.toList());

                res.setSuccess(true);
                res.setStatus(HttpStatus.OK.value());
                res.setMessage("Reports fetched successfully for given customerId");
                res.setData(result);
            } else {
                res.setSuccess(true);
                res.setStatus(HttpStatus.OK.value());
                res.setMessage("No reports found for given customerId");
                res.setData(Collections.emptyList());
            }

        } catch (Exception e) {
            res.setSuccess(false);
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            res.setMessage("Error fetching reports: " + e.getMessage());
            res.setData(null);
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────
    // GET REPORTS BY PATIENT ID + BOOKING ID
    // ─────────────────────────────────────────────────────────────────
    @Override
    public Response getReportsByPatientIdAndBookingId(String patientId, String bookingId) {
        Response res = new Response();
        try {
            List<ReportsList> reportsListData =
                    reportsRepository.findByReportsListPatientIdAndReportsListBookingId(
                            patientId, bookingId);

            if (reportsListData != null && !reportsListData.isEmpty()) {
                List<ReportsDtoList> result = reportsListData.stream()
                        .map(this::toResponseDtoList) // ✅ signed URLs generated here
                        .collect(Collectors.toList());

                res.setSuccess(true);
                res.setStatus(HttpStatus.OK.value());
                res.setMessage("Reports fetched successfully for given patient and booking");
                res.setData(result);
            } else {
                res.setSuccess(true);
                res.setStatus(HttpStatus.OK.value());
                res.setMessage("No reports found for given patient and booking");
                res.setData(Collections.emptyList());
            }

        } catch (Exception e) {
            res.setSuccess(false);
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            res.setMessage("Error fetching reports: " + e.getMessage());
            res.setData(null);
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────
    // UPDATE REPORT
    // Accepts new S3 fileKeys in reportFile field
    // ─────────────────────────────────────────────────────────────────
    @Override
    public Response updateReport(String reportId, ReportsDtoList dto) {
        try {
            Optional<ReportsList> optional = reportsRepository.findById(reportId);
            if (optional.isEmpty()) {
                return Response.builder()
                        .success(false)
                        .data(null)
                        .message("Report not found")
                        .status(HttpStatus.NOT_FOUND.value())
                        .build();
            }

            ReportsList existing = optional.get();

            if (dto.getPatientId() != null)  existing.setPatientId(dto.getPatientId());
            if (dto.getCustomerId() != null) existing.setCustomerId(dto.getCustomerId());

            if (dto.getReportsList() == null || dto.getReportsList().isEmpty()) {
                ReportsList savedNoNested = reportsRepository.save(existing);
                return Response.builder()
                        .success(true)
                        .data(toResponseDtoList(savedNoNested))
                        .message("Report updated successfully (top-level only)")
                        .status(HttpStatus.OK.value())
                        .build();
            }

            List<Reports> existingReports = existing.getReportsList();
            if (existingReports == null) existingReports = new ArrayList<>();

            for (ReportsDTO incoming : dto.getReportsList()) {

                // ── Validate S3 keys if provided ────────────────────
                if (incoming.getReportFile() != null && !incoming.getReportFile().isEmpty()) {
                    String keyError = validateS3Keys(incoming.getReportFile());
                    if (keyError != null) {
                        return Response.builder()
                                .success(false)
                                .message(keyError)
                                .status(HttpStatus.BAD_REQUEST.value())
                                .data(null)
                                .build();
                    }
                }

                boolean updated = false;

                for (int i = 0; i < existingReports.size(); i++) {
                    Reports r = existingReports.get(i);

                    boolean bookingMatches = incoming.getBookingId() != null
                            && incoming.getBookingId().equals(r.getBookingId());
                    boolean patientMatches = incoming.getPatientId() != null
                            && incoming.getPatientId().equals(r.getPatientId());

                    boolean isMatch = false;
                    if (incoming.getBookingId() != null && incoming.getPatientId() != null) {
                        isMatch = bookingMatches && patientMatches;
                    } else if (incoming.getBookingId() != null) {
                        isMatch = bookingMatches;
                    } else if (incoming.getPatientId() != null) {
                        isMatch = patientMatches;
                    }

                    if (isMatch) {
                        if (incoming.getCustomerMobileNumber() != null)
                            r.setCustomerMobileNumber(incoming.getCustomerMobileNumber());
                        if (incoming.getReportName() != null)
                            r.setReportName(incoming.getReportName());
                        if (incoming.getReportDate() != null)
                            r.setReportDate(incoming.getReportDate());
                        if (incoming.getReportStatus() != null)
                            r.setReportStatus(incoming.getReportStatus());
                        if (incoming.getReportType() != null)
                            r.setReportType(incoming.getReportType());

                        // ✅ Replace file keys if new ones provided
                        if (incoming.getReportFile() != null && !incoming.getReportFile().isEmpty())
                            r.setReportFile(incoming.getReportFile());

                        existingReports.set(i, r);
                        updated = true;
                        break;
                    }
                }

                // ── No match found → add as new report entry ────────
                if (!updated) {
                    Reports newReport = Reports.builder()
                            .bookingId(incoming.getBookingId())
                            .patientId(incoming.getPatientId())
                            .customerMobileNumber(incoming.getCustomerMobileNumber())
                            .reportName(incoming.getReportName())
                            .reportDate(incoming.getReportDate())
                            .reportStatus(incoming.getReportStatus())
                            .reportType(incoming.getReportType())
                            .reportFile(incoming.getReportFile()) // ✅ S3 keys
                            .build();
                    existingReports.add(newReport);
                }
            }

            existing.setReportsList(existingReports);
            ReportsList updatedEntity = reportsRepository.save(existing);

            return Response.builder()
                    .success(true)
                    .data(toResponseDtoList(updatedEntity)) // ✅ signed URLs returned
                    .message("Report updated successfully")
                    .status(HttpStatus.OK.value())
                    .build();

        } catch (Exception e) {
            return Response.builder()
                    .success(false)
                    .data(null)
                    .message("Error updating report: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE FULL REPORT DOCUMENT
    // ─────────────────────────────────────────────────────────────────
    @Override
    public Response deleteReport(String reportId) {
        try {
            Optional<ReportsList> optional = reportsRepository.findById(reportId);

            if (optional.isEmpty()) {
                return Response.builder()
                        .success(false)
                        .message("Report not found")
                        .status(HttpStatus.NOT_FOUND.value())
                        .data(null)
                        .build();
            }

            ReportsList reportsList = optional.get();

            // ✅ No S3 delete here — S3 keys are shared with booking service
            // Booking feign call to sync deletion
            if (reportsList.getReportsList() != null
                    && !reportsList.getReportsList().isEmpty()) {
                Reports reports = reportsList.getReportsList().get(0);
                bookingFeign.deleteReport(reports.getBookingId(), "null");
            }

            reportsRepository.deleteById(reportId);

            return Response.builder()
                    .success(true)
                    .message("Report deleted successfully")
                    .status(HttpStatus.OK.value())
                    .data("Deleted ID: " + reportId)
                    .build();

        } catch (Exception e) {
            return Response.builder()
                    .success(false)
                    .message("Error while deleting report: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .data(null)
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE SINGLE REPORT FILE BY INDEX
    // Removes S3 key at given index from the stored list
    // ─────────────────────────────────────────────────────────────────
    @Override
    public Response deleteReportFile(String reportId, String bookingId, int fileIndex) {
        try {
            Optional<ReportsList> optional = reportsRepository.findById(reportId);
            if (optional.isEmpty()) {
                return Response.builder()
                        .success(false)
                        .message("No report found with ID: " + reportId)
                        .status(HttpStatus.NOT_FOUND.value())
                        .data(null)
                        .build();
            }

            ReportsList reportsList = optional.get();
            List<Reports> reportEntries = reportsList.getReportsList();
            boolean updated = false;

            for (int i = 0; i < reportEntries.size(); i++) {
                Reports report = reportEntries.get(i);

                if (report.getBookingId().equals(bookingId)) {
                    List<String> fileKeys = report.getReportFile();

                    if (fileKeys == null || fileIndex < 0 || fileIndex >= fileKeys.size()) {
                        return Response.builder()
                                .success(false)
                                .message("Invalid file index: " + fileIndex)
                                .status(HttpStatus.BAD_REQUEST.value())
                                .data(null)
                                .build();
                    }

                    // ✅ Remove the S3 key at this index from MongoDB
                    // Note: actual S3 object is NOT deleted (may be referenced by booking service)
                    fileKeys.remove(fileIndex);

                    // Sync deletion with booking service
                    bookingFeign.deleteReport(bookingId, String.valueOf(fileIndex));

                    if (fileKeys.isEmpty()) {
                        reportEntries.remove(i);
                        updated = true;
                        break;
                    } else {
                        report.setReportFile(fileKeys);
                        reportEntries.set(i, report);
                        updated = true;
                        break;
                    }
                }
            }

            if (!updated) {
                return Response.builder()
                        .success(false)
                        .message("No report found for bookingId: " + bookingId)
                        .status(HttpStatus.NOT_FOUND.value())
                        .data(null)
                        .build();
            }

            // If all report entries gone → delete the document
            if (reportEntries.isEmpty()) {
                reportsRepository.deleteById(reportId);
                return Response.builder()
                        .success(true)
                        .message("All files deleted — report entry removed from database.")
                        .status(HttpStatus.OK.value())
                        .data("Deleted report document ID: " + reportId)
                        .build();
            }

            reportsList.setReportsList(reportEntries);
            ReportsList updatedList = reportsRepository.save(reportsList);

            return Response.builder()
                    .success(true)
                    .message("Report file deleted successfully for bookingId: " + bookingId)
                    .status(HttpStatus.OK.value())
                    .data(toResponseDtoList(updatedList)) // ✅ signed URLs in response
                    .build();

        } catch (Exception e) {
            return Response.builder()
                    .success(false)
                    .message("Error while deleting report file: " + e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .data(null)
                    .build();
        }
    }
}