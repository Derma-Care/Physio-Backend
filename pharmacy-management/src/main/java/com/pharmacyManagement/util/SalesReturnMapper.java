package com.pharmacyManagement.util;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
//import org.springframework.stereotype.Component;
import com.pharmacyManagement.dto.SalesReturnCreateResponse;
import com.pharmacyManagement.dto.SalesReturnRequest;
import com.pharmacyManagement.dto.SalesReturnResponse;
import com.pharmacyManagement.dto.SalesReturnSummaryResponse;
import com.pharmacyManagement.dto.SalesReturnUpdateRequest;
import com.pharmacyManagement.entity.SalesReturn;
import com.pharmacyManagement.entity.SalesReturnItem;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SalesReturnMapper {

    // ── Entity → Create Response ──────────────────────────────────────────
    @Mapping(target = "netRefund", source = "netRefundAmount")
    SalesReturnCreateResponse toCreateResponse(SalesReturn entity);

    // ── Entity → Full Response ────────────────────────────────────────────
    @Mapping(target = "patientDetails.fileNo",      source = "fileNo")
    @Mapping(target = "patientDetails.patientName", source = "patientName")
    @Mapping(target = "patientDetails.mobileNo",    source = "mobileNo")
    @Mapping(target = "returnDetails.returnType",   source = "returnType")
    @Mapping(target = "returnDetails.refundMode",   source = "refundMode")
    @Mapping(target = "returnDetails.returnDate",   source = "returnDate")
    @Mapping(target = "summary.totalReturnAmount",  source = "totalReturnAmount")
    @Mapping(target = "summary.totalDiscount",      source = "totalDiscount")
    @Mapping(target = "summary.totalGST",           source = "totalGst")
    @Mapping(target = "summary.netRefundAmount",    source = "netRefundAmount")
    SalesReturnResponse toResponse(SalesReturn entity);

    List<SalesReturnResponse.ReturnItemDto> toItemDtoList(List<SalesReturnItem> items);

    SalesReturnResponse.ReturnItemDto toItemDto(SalesReturnItem item);

    // ── Entity → Summary Response ─────────────────────────────────────────
    @Mapping(target = "returnDate",
             expression = "java(entity.getReturnDate() != null ? entity.getReturnDate().toLocalDate() : null)")
    SalesReturnSummaryResponse toSummaryResponse(SalesReturn entity);

    // ── Request → Item Entity ─────────────────────────────────────────────
    SalesReturnItem toItemEntity(SalesReturnRequest.ReturnItemRequest dto);

    SalesReturnItem toItemEntityFromUpdate(SalesReturnUpdateRequest.ReturnItemDto dto);
}
