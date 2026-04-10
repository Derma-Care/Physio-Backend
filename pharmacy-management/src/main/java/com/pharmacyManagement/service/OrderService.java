package com.pharmacyManagement.service;

import com.pharmacyManagement.dto.OrderDTO;
import com.pharmacyManagement.dto.Response;

public interface OrderService {
    Response createOrder(OrderDTO dto);

	Response getOrdersByClinicAndBranch(String clinicId, String branchId);

	Response getOrderByClinicBranchAndOrderId(String clinicId, String branchId, String orderId);

	Response updateOrder(String orderId, OrderDTO dto);

	Response deleteOrder(String orderId);

	Response getOrderByClinicBranchAndSupplierId(String clinicId, String branchId, String supplierId);
}