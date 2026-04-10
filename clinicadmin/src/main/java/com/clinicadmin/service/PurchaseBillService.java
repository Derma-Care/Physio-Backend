package com.clinicadmin.service;

import com.clinicadmin.dto.PurchaseBillDTO;
import com.clinicadmin.dto.Response;

public interface PurchaseBillService {

    Response savePurchase(PurchaseBillDTO dto);

    Response updatePurchase(String id, PurchaseBillDTO dto);

    Response getPurchaseById(String id);

    Response getPurchaseByBillNo(String billNo);

    Response getAllPurchases();

    Response deletePurchase(String id);

    Response getByDateRange(String from, String to);
}
