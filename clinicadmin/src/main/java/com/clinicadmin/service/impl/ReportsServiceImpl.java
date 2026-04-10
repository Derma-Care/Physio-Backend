package com.clinicadmin.service.impl;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import java.util.Optional;


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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;

@Service
public class ReportsServiceImpl implements ReportsService {

    @Autowired
    private ReportsRepository reportsRepository;

    @Autowired
    private BookingFeign bookingFeign;

    // ----------------------------------------- Add Reports -----------------------------------------------------
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

            // ✅ Convert DTO to Entity and decode Base64 files
            for (ReportsDTO reportDTO : dto.getReportsList()) {
                if (reportDTO.getBookingId() == null || reportDTO.getBookingId().isEmpty()) {
                    return Response.builder()
                            .success(false)
                            .message("Booking ID cannot be null or empty in report")
                            .status(HttpStatus.BAD_REQUEST.value())
                            .data(null)
                            .build();
                }

                bookingId = reportDTO.getBookingId();
                List<byte[]> decodedFiles = new ArrayList<>();

                if (reportDTO.getReportFile() != null) {
                    for (String encoded : reportDTO.getReportFile()) {
                        decodedFiles.add(Base64.getDecoder().decode(encoded));
                    }
                }

                Reports report = Reports.builder()
                        .bookingId(reportDTO.getBookingId())
                        .patientId(reportDTO.getPatientId())
                        .reportName(reportDTO.getReportName())
                        .reportDate(reportDTO.getReportDate())
                        .reportStatus(reportDTO.getReportStatus())
                        .reportType(reportDTO.getReportType())
                        .customerMobileNumber(reportDTO.getCustomerMobileNumber())
                        .reportFile(decodedFiles)
                        .build();

                reports.add(report);
            }

            // ✅ Fetch booking details via Feign Client
            ResponseEntity<ResponseStructure<BookingResponse>> response = bookingFeign.getBookedService(bookingId);
            BookingResponse bookingData = response.getBody() != null ? response.getBody().getData() : null;

            if (bookingData != null) {
                // Update booking with new reports
                List<ReportsDtoList> existingReports = bookingData.getReports();
                if (existingReports == null) existingReports = new ArrayList<>();
                existingReports.add(dto);
                bookingData.setReports(existingReports);

                // Set IDs from booking service
                dto.setCustomerId(bookingData.getCustomerId());
                dto.setPatientId(bookingData.getPatientId());

                // Update booking service record
                bookingFeign.updateAppointment(bookingData);
            }

            // ✅ Save report in MongoDB
            reportsList.setCustomerId(dto.getCustomerId());
            reportsList.setPatientId(dto.getPatientId());
            reportsList.setReportsList(reports);

            ReportsList saved = reportsRepository.save(reportsList);

            return Response.builder()
                    .success(true)
                    .data(saved)
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

    // --------------------------------------- Get Reports By BookingId --------------------------------------------
    @Override
    public Response getReportsByBookingId(String bookingId) {
        Response res = new Response();
        try {
            List<ReportsList> reportsListData = reportsRepository.findByReportsListBookingId(bookingId);
            List<ReportsDtoList> toDTO = new ObjectMapper().convertValue(
                    reportsListData, new TypeReference<List<ReportsDtoList>>() {});

            if (toDTO != null && !toDTO.isEmpty()) {
                res.setSuccess(true);
                res.setStatus(HttpStatus.OK.value());
                res.setMessage("Reports fetched successfully for given bookingId");
                res.setData(toDTO);
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

    // ---------------------------------------- Get All Reports -------------------------------------------------------
    @Override
    public Response getAllReports() {
        Response res = new Response();
        try {
            List<ReportsList> reportList = reportsRepository.findAll();
            List<ReportsDtoList> toDTO = new ObjectMapper().convertValue(
                    reportList, new TypeReference<List<ReportsDtoList>>() {});

            if (toDTO != null && !toDTO.isEmpty()) {
                res.setSuccess(true);
                res.setStatus(HttpStatus.OK.value());
                res.setMessage("All reports fetched successfully");
                res.setData(toDTO);
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

    // --------------------------------------- Get Reports By CustomerId --------------------------------------------
    @Override
    public Response getReportsByCustomerId(String customerId) {
        Response res = new Response();
        try {
            List<ReportsList> reportsListData = reportsRepository.findByCustomerId(customerId);
            List<ReportsDtoList> toDTO = new ObjectMapper().convertValue(
                    reportsListData, new TypeReference<List<ReportsDtoList>>() {});

            if (toDTO != null && !toDTO.isEmpty()) {
                res.setSuccess(true);
                res.setStatus(HttpStatus.OK.value());
                res.setMessage("Reports fetched successfully for given customerId");
                res.setData(toDTO);
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

            // Update top-level fields if provided
            if (dto.getPatientId() != null) existing.setPatientId(dto.getPatientId());
            if (dto.getCustomerId() != null) existing.setCustomerId(dto.getCustomerId());

            // If no nested list provided, just save top-level changes
            if (dto.getReportsList() == null || dto.getReportsList().isEmpty()) {
                ReportsList savedNoNested = reportsRepository.save(existing);
                return Response.builder()
                        .success(true)
                        .data(savedNoNested)
                        .message("Report updated successfully (top-level only)")
                        .status(HttpStatus.OK.value())
                        .build();
            }

            List<Reports> existingReports = existing.getReportsList();
            if (existingReports == null) existingReports = new ArrayList<>();

            // For each incoming ReportsDTO, update or add
            for (ReportsDTO incoming : dto.getReportsList()) {
                boolean updated = false;

                // Decode Base64 files if present
                List<byte[]> decodedFiles = null;
                if (incoming.getReportFile() != null) {
                    decodedFiles = new ArrayList<>();
                    for (String enc : incoming.getReportFile()) {
                        decodedFiles.add(Base64.getDecoder().decode(enc));
                    }
                }

                for (int i = 0; i < existingReports.size(); i++) {
                    Reports r = existingReports.get(i);

                    boolean bookingMatches = incoming.getBookingId() != null && incoming.getBookingId().equals(r.getBookingId());
                    boolean patientMatches = incoming.getPatientId() != null && incoming.getPatientId().equals(r.getPatientId());

                    boolean isMatch = false;
                    if (incoming.getBookingId() != null && incoming.getPatientId() != null) {
                        isMatch = bookingMatches && patientMatches;
                    } else if (incoming.getBookingId() != null) {
                        isMatch = bookingMatches;
                    } else if (incoming.getPatientId() != null) {
                        isMatch = patientMatches;
                    }

                    if (isMatch) {
                        // Update only non-null fields
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
                        if (decodedFiles != null)
                            r.setReportFile(decodedFiles);

                        existingReports.set(i, r);
                        updated = true;
                        break;
                    }
                }

                // Add new report if not matched
                if (!updated) {
                    Reports newReport = Reports.builder()
                            .bookingId(incoming.getBookingId())
                            .patientId(incoming.getPatientId())
                            .customerMobileNumber(incoming.getCustomerMobileNumber())
                            .reportName(incoming.getReportName())
                            .reportDate(incoming.getReportDate())
                            .reportStatus(incoming.getReportStatus())
                            .reportType(incoming.getReportType())
                            .reportFile(decodedFiles)
                            .build();

                    existingReports.add(newReport);
                }
            }

            existing.setReportsList(existingReports);
            ReportsList updated = reportsRepository.save(existing);

            return Response.builder()
                    .success(true)
                    .data(updated)
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



    // --------------------------------------- Get Reports By PatientId + BookingId -----------------------------------
    @Override
    public Response getReportsByPatientIdAndBookingId(String patientId, String bookingId) {
        Response res = new Response();
        try {
            List<ReportsList> reportsListData =
                    reportsRepository.findByReportsListPatientIdAndReportsListBookingId(patientId, bookingId);

            List<ReportsDtoList> toDTO = new ObjectMapper().convertValue(
                    reportsListData, new TypeReference<List<ReportsDtoList>>() {});

            if (toDTO != null && !toDTO.isEmpty()) {
                res.setSuccess(true);
                res.setStatus(HttpStatus.OK.value());
                res.setMessage("Reports fetched successfully for given patient and booking");
                res.setData(toDTO);
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
    
    @Override
    public Response deleteReport(String reportId) {
        try {
            boolean exists = reportsRepository.existsById(reportId);
            if (!exists) {
                return Response.builder()
                        .success(false)
                        .message("Report not found")
                        .status(HttpStatus.NOT_FOUND.value())
                        .data(null)
                        .build();
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

            // iterate through the reports list
            for (int i = 0; i < reportEntries.size(); i++) {
                Reports report = reportEntries.get(i);

                if (report.getBookingId().equals(bookingId)) {
                    List<byte[]> files = report.getReportFile();

                    if (files == null || fileIndex < 0 || fileIndex >= files.size()) {
                        return Response.builder()
                                .success(false)
                                .message("Invalid file index: " + fileIndex)
                                .status(HttpStatus.BAD_REQUEST.value())
                                .data(null)
                                .build();
                    }

              
                    files.remove(fileIndex);

               
                    if (files.isEmpty()) {
                        reportEntries.remove(i);
                        updated = true;
                        break;
                    } else {
                        report.setReportFile(files);
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
                    .data(updatedList)
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
