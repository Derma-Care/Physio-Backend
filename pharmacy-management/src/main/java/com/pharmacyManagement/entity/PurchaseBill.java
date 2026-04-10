package com.pharmacyManagement.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.pharmacyManagement.dto.Dates;
import com.pharmacyManagement.dto.PaymentDetails;
import com.pharmacyManagement.dto.Summary;
import com.pharmacyManagement.dto.SuplierInPurchaseDTO;
import com.pharmacyManagement.dto.TaxDetails;

import lombok.Data;

@Data
@Document(collection = "purchase_bills")
public class PurchaseBill {

    @Id
    private String purchaseId;
    private String purchaseBillNo;
    private String orderId;
    private String invoiceNo;
    private String financialYear;
    private Dates dates;
    private TaxDetails taxDetails;
    private SuplierInPurchaseDTO supplierDetails;
    private PaymentDetails paymentDetails;
    private List<PurchaseItem> items;
    private Summary summary;
    private String status; // "ACTIVE | CANCELLED",
    private String createdAt;
    private String updatedAt;
    private String clinicId;
    private String branchId;
    
}
//  private String date;
//  private String time;
//  private String supplierName;
//  private String invoiceDate;
//  private String receivingDate;
  

//  private String taxType; // IGST / CGST-SGST
//    private double totalAmount;
//    private double discountPercentage;
//    private double discountAmountTotal;
//    private double netAmount;
//
//    private double totalIGST;
//    private double totalCGST;
//    private double totalSGST;
//
//    private double totalTax;
//    private double finalTotal;
//
//    private double paidAmount;
//    private double balanceAmount;
//
//    private double previousAdjustment;
//    private double postDiscount;
//    private double netPayable;
//    
//    private String paymentMode;
//    private String billDueDate;
//    private String creditDays;
//    private String duePaidBillNo;
//    private String department;
//}
