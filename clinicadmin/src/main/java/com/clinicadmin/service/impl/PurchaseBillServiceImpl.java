package com.clinicadmin.service.impl;

import org.springframework.stereotype.Service;

import com.clinicadmin.dto.PurchaseBillDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.feignclient.PharmacyManagementFeignClient;
import com.clinicadmin.service.PurchaseBillService;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseBillServiceImpl implements PurchaseBillService {

    private final PharmacyManagementFeignClient purchaseFeignClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Response savePurchase(PurchaseBillDTO dto) {
        try {
            return purchaseFeignClient.savePurchase(dto);
        } catch (FeignException ex) {
            return handleFeignError(ex);
        }
    }

    @Override
    public Response updatePurchase(String id, PurchaseBillDTO dto) {
        try {
            return purchaseFeignClient.updatePurchase(id, dto);
        } catch (FeignException ex) {
            return handleFeignError(ex);
        }
    }

    @Override
    public Response getPurchaseById(String id) {
        try {
            return purchaseFeignClient.getPurchaseById(id);
        } catch (FeignException ex) {
            return handleFeignError(ex);
        }
    }

    @Override
    public Response getPurchaseByBillNo(String billNo) {
        try {
            return purchaseFeignClient.getPurchaseByBillNo(billNo);
        } catch (FeignException ex) {
            return handleFeignError(ex);
        }
    }

    @Override
    public Response getAllPurchases() {
        try {
            return purchaseFeignClient.getAllPurchases();
        } catch (FeignException ex) {
            return handleFeignError(ex);
        }
    }

    @Override
    public Response deletePurchase(String id) {
        try {
            return purchaseFeignClient.deletePurchase(id);
        } catch (FeignException ex) {
            return handleFeignError(ex);
        }
    }

    @Override
    public Response getByDateRange(String from, String to) {
        try {
            return purchaseFeignClient.getByDateRange(from, to);
        } catch (FeignException ex) {
            return handleFeignError(ex);
        }
    }

    // ⭐ Common Feign Error Handler
    private Response handleFeignError(FeignException ex) {
        try {
            return objectMapper.readValue(ex.contentUTF8(), Response.class);
        } catch (Exception parseEx) {

            Response fallback = new Response();
            fallback.setStatus(500);
            fallback.setMessage("Unexpected error while calling pharmacy service: " + ex.getMessage());
            fallback.setData(null);
            return fallback;
        }
    }
}
