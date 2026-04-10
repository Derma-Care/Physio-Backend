package com.pharmacyManagement.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.dto.SalesReturnCreateResponse;
import com.pharmacyManagement.dto.SalesReturnFilterRequest;
import com.pharmacyManagement.dto.SalesReturnRequest;
import com.pharmacyManagement.dto.SalesReturnResponse;
import com.pharmacyManagement.dto.SalesReturnSummaryResponse;
import com.pharmacyManagement.dto.SalesReturnUpdateRequest;
import com.pharmacyManagement.entity.SalesReturn;
import com.pharmacyManagement.entity.SalesReturnItem;
import com.pharmacyManagement.enums.ReturnStatus;
import com.pharmacyManagement.repository.OpSalesRepository;
import com.pharmacyManagement.repository.SalesReturnRepository;
import com.pharmacyManagement.service.SalesReturnService;
import com.pharmacyManagement.util.BusinessException;
import com.pharmacyManagement.util.ResourceNotFoundException;
import com.pharmacyManagement.util.SalesReturnCustomRepository;
import com.pharmacyManagement.util.SalesReturnMapper;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SalesReturnServiceImpl implements SalesReturnService {

	@Autowired
    public SalesReturnRepository repository;
	@Autowired
    public SalesReturnCustomRepository customRepository;
    @Autowired
    public SalesReturnMapper mapper;
    @Autowired
    public OpSalesRepository opSalesRepository;
    
    @Autowired
    private InventoryServiceImpl inventoryServiceImpl;


    // ── CREATE ─────────────────────────────────────────────────────────────
    @Override
    public SalesReturnCreateResponse createReturn(SalesReturnRequest request) {
        log.info("Creating sales return for bill: {}", request.getReturnDetails().getOriginalBillNo());
      if(opSalesRepository.findByBillNo(request.getOriginalBillNo()).isPresent()&&opSalesRepository.findByBillNo(request.getOriginalBillNo()).get().getIncludeReturns()) {
        validateItemQuantities(request.getItems());

        SalesReturnRequest.PatientDetailsRequest pd = request.getPatientDetails();
        SalesReturnRequest.ReturnDetailsRequest  rd = request.getReturnDetails();
        SalesReturnRequest.SummaryRequest        sm = request.getSummary();

        List<SalesReturnItem> items = request.getItems().stream()
                .map(mapper::toItemEntity)
                .collect(Collectors.toList());

        SalesReturn entity = SalesReturn.builder()
                .returnNo(generateReturnNo())
                .originalBillNo(rd.getOriginalBillNo())
                .fileNo(pd.getFileNo())
                .patientName(pd.getPatientName())
                .mobileNo(pd.getMobileNo())
                .returnType(rd.getReturnType())
                .refundMode(rd.getRefundMode())
                .returnDate(LocalDateTime.now())
                .totalReturnAmount(sm.getTotal())
                .totalDiscount(sm.getDiscount())
                .totalGst(sm.getGst())
                .netRefundAmount(sm.getNetRefund())
                .items(items)
                .clinicId(request.getClinicId())
                .branchId(request.getBranchId())
                .status(ReturnStatus.ACTIVE)
                .build();

        SalesReturn saved = repository.save(entity);
        updateInventory(entity);
        log.info("Sales return created: {}", saved.getReturnNo());
        return mapper.toCreateResponse(saved);
      }else{
    	  return null;
     }}
    
      
    public void updateInventory(SalesReturn opsale) {
    	try {
    		for(SalesReturnItem m : opsale.getItems()) {
    		inventoryServiceImpl.updateInventoryByMedicineId(m.getMedicineId(), m.getBatchNo(), "+", m.getReturnQty()
    				);
      }}catch(Exception e) {}}
    
    
    // ── GET ────────────────────────────────────────────────────────────────
    @Override
    public SalesReturnResponse getByReturnNo(String returnNo) {
        SalesReturn entity = findActiveReturn(returnNo);
        SalesReturnResponse response = mapper.toResponse(entity);
        response.setItems(mapper.toItemDtoList(entity.getItems()));
        return response;
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────
    @Override
    public void updateReturn(String returnNo, SalesReturnUpdateRequest request) {
        log.info("Updating sales return: {}", returnNo);

        SalesReturn entity = findActiveReturn(returnNo);

        validateUpdateItemQuantities(request.getItems());

        SalesReturnUpdateRequest.PatientDetailsDto pd = request.getPatientDetails();
        SalesReturnUpdateRequest.ReturnDetailsDto  rd = request.getReturnDetails();

        entity.setOriginalBillNo(request.getOriginalBillNo());
        entity.setFileNo(pd.getFileNo());
        entity.setPatientName(pd.getPatientName());
        entity.setMobileNo(pd.getMobileNo());
        entity.setReturnType(rd.getReturnType());
        entity.setRefundMode(rd.getRefundMode());

        List<SalesReturnItem> newItems = request.getItems().stream()
                .map(mapper::toItemEntityFromUpdate)
                .collect(Collectors.toList());
        entity.setItems(newItems);

        repository.save(entity);
        log.info("Sales return updated: {}", returnNo);
    }

    // ── CANCEL (soft delete) ───────────────────────────────────────────────
    @Override
    public void cancelReturn(String returnNo) {
        log.info("Cancelling sales return: {}", returnNo);
        SalesReturn entity = findActiveReturn(returnNo);
        entity.setStatus(ReturnStatus.CANCELLED);
        repository.save(entity);
        log.info("Sales return cancelled: {}", returnNo);
    }

    // ── FILTER ─────────────────────────────────────────────────────────────
    @Override
    public List<SalesReturnSummaryResponse> filterReturns(SalesReturnFilterRequest filter) {
        return customRepository.filterReturns(filter).stream()
                .map(mapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private SalesReturn findActiveReturn(String returnNo) {
        return repository.findByReturnNoAndStatus(returnNo, ReturnStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sales return not found or already cancelled: " + returnNo));
    }

    private void validateItemQuantities(List<SalesReturnRequest.ReturnItemRequest> items) {
        items.forEach(item -> {
            if (item.getReturnQty() > item.getSoldQty()) {
                throw new BusinessException(
                        "Return quantity (" + item.getReturnQty()
                        + ") cannot exceed sold quantity (" + item.getSoldQty()
                        + ") for medicine: " + item.getMedicineName());
            }
        });
    }

    private void validateUpdateItemQuantities(List<SalesReturnUpdateRequest.ReturnItemDto> items) {
        items.forEach(item -> {
            if (item.getReturnQty() > item.getSoldQty()) {
                throw new BusinessException(
                        "Return quantity (" + item.getReturnQty()
                        + ") cannot exceed sold quantity (" + item.getSoldQty()
                        + ") for medicine: " + item.getMedicineName());
            }
        });
    }

    private String generateReturnNo() {
        return "SRN" + System.currentTimeMillis();
    }
    
    @Override
    public ResponseEntity<Response> getAllSalesReturns() {
     Response res = new Response();
      List<SalesReturnResponse> list = new LinkedList<>();
      try {
       List<SalesReturn> entity = repository.findAll();
       if(entity != null || !entity.isEmpty()) {
       for(SalesReturn s : entity) {
       SalesReturnResponse response = mapper.toResponse(s);
       response.setItems(mapper.toItemDtoList(s.getItems()));
       list.add(response);}
       res.setMessage("salesreturns retrieved succesfully");
       res.setStatus(200);
       res.setSuccess(true);
       res.setData(list);
       }else {
    	   res.setMessage("salesreturns not found");
           res.setStatus(404);
           res.setSuccess(false);
	}}catch(Exception e) {}
        return ResponseEntity.status(res.getStatus()).body(res);
    }
    
    
    @Override
    public ResponseEntity<Response> getAllSalesReturnsByClinicAndBranchId(String clinicId, String branchId) {
     Response res = new Response();
      List<SalesReturnResponse> list = new LinkedList<>();
      try {
       List<SalesReturn> entity = repository.findByClinicIdAndBranchId(clinicId, branchId);
       if(entity != null || !entity.isEmpty()) {
       for(SalesReturn s : entity) {
       SalesReturnResponse response = mapper.toResponse(s);
       response.setItems(mapper.toItemDtoList(s.getItems()));
       list.add(response);}
       res.setMessage("salesreturns retrieved succesfully");
       res.setStatus(200);
       res.setSuccess(true);
       res.setData(list);
       }else {
    	   res.setMessage("salesreturns not found");
           res.setStatus(404);
           res.setSuccess(false);
	}}catch(Exception e) {}
        return ResponseEntity.status(res.getStatus()).body(res);
    }
    
}


