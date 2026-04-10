package com.pharmacyManagement.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.pharmacyManagement.dto.ExpiryMedicineReportDTO;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.entity.Inventory;
import com.pharmacyManagement.repository.InventoryRepository;
import com.pharmacyManagement.service.ReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final InventoryRepository inventoryRepository;

    @Override
    public Response getExpiredMedicineReport(String clinicId, String branchId) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Inventory> inventoryList =
                inventoryRepository.findByClinicIdAndBranchId(clinicId, branchId);

        List<ExpiryMedicineReportDTO> reportData = inventoryList.stream()

                .filter(inv -> {
                    LocalDate expiry = LocalDate.parse(inv.getExpiryDate(), formatter);
                    return expiry.isBefore(LocalDate.now());
                })

                .map(inv -> {

                    ExpiryMedicineReportDTO dto = new ExpiryMedicineReportDTO();

                    dto.setMedicineName(inv.getMedicineName());
                    dto.setBatchNumber(inv.getBatchNo());
                    dto.setExpiryDate(inv.getExpiryDate());
                    dto.setStatus("Expired");
                    dto.setQuantity(inv.getAvailableQty());
                    dto.setMedicineId(inv.getMedicineId());

                    return dto;

                }).toList();

        return Response.builder()
                .success(true)
                .message("Expiry report fetched successfully")
                .data(reportData)
                .status(HttpStatus.OK.value())
                .build();
    }
}