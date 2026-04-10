package com.pharmacyManagement.util;



import com.pharmacyManagement.dto.MedicineDTO;
import com.pharmacyManagement.entity.Medicine;

public class MedicineMapper {

    // DTO ➝ Entity
    public static Medicine toEntity(MedicineDTO dto) {

        if (dto == null) return null;

        return Medicine.builder()
                .id(dto.getId())
                .barcode(dto.getBarcode())
                .productName(dto.getProductName())
                .brandName(dto.getBrandName())
                .category(dto.getCategory())
                .composition(dto.getComposition())
                .manufacturer(dto.getManufacturer())
                .packSize(dto.getPackSize())
                .hsnCode(dto.getHsnCode())
                .gstPercent(dto.getGstPercent())
                .mrp(dto.getMrp())
                .minStock(dto.getMinStock())
                .status(dto.getStatus())
                .clinicId(dto.getClinicId())
                .branchId(dto.getBranchId())
                .createdAt(dto.getCreatedAt())
                .build();
    }

    // Entity ➝ DTO
    public static MedicineDTO toDTO(Medicine entity) {

        if (entity == null) return null;

        return MedicineDTO.builder()
                .id(entity.getId())
                .barcode(entity.getBarcode())
                .productName(entity.getProductName())
                .brandName(entity.getBrandName())
                .category(entity.getCategory())
                .composition(entity.getComposition())
                .manufacturer(entity.getManufacturer())
                .packSize(entity.getPackSize())
                .hsnCode(entity.getHsnCode())
                .gstPercent(entity.getGstPercent())
                .mrp(entity.getMrp())
                .minStock(entity.getMinStock())
                .status(entity.getStatus())
                .clinicId(entity.getClinicId())
                .branchId(entity.getBranchId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
