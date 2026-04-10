package com.pharmacyManagement.service.impl;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.pharmacyManagement.dto.BranchDTO;
import com.pharmacyManagement.dto.ClinicDTO;
import com.pharmacyManagement.dto.OrderDTO;
import com.pharmacyManagement.dto.ProductDTO;
import com.pharmacyManagement.dto.Response;
import com.pharmacyManagement.dto.StatusHistoryDTO;
import com.pharmacyManagement.dto.SuppliersDTO;
import com.pharmacyManagement.entity.Branch;
import com.pharmacyManagement.entity.Clinic;
import com.pharmacyManagement.entity.Order;
import com.pharmacyManagement.entity.Product;
import com.pharmacyManagement.entity.StatusHistory;
import com.pharmacyManagement.entity.Suppliers;
import com.pharmacyManagement.repository.OrderRepository;
import com.pharmacyManagement.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository repository;

    @Override
    public Response createOrder(OrderDTO dto) {

        Response response = new Response();

        try {

            
            String orderId = "RO-" + System.currentTimeMillis();

         
            Order order = mapToEntity(dto);
            order.setOrderId(orderId);

      
            Order savedOrder = repository.save(order);

          
            OrderDTO responseDto = mapToDTO(savedOrder);

        
            response.setSuccess(true);
            response.setMessage("Order created successfully");
            response.setStatus(HttpStatus.CREATED.value());
            response.setData(responseDto);
            response.setUpdatedAt(LocalDateTime.now());

        } catch (Exception e) {

            response.setSuccess(false);
            response.setMessage("Failed to create order: " + e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setUpdatedAt(LocalDateTime.now());
        }

        return response;
    }
    
    // ================= GET BY CLINIC + BRANCH =================
    @Override
    public Response getOrdersByClinicAndBranch(String clinicId, String branchId) {

        Response res = new Response();

        List<Order> orders =
                repository.findByClinic_ClinicIdAndBranch_BranchId(clinicId, branchId);

        if (orders.isEmpty()) {
            res.setSuccess(false);
            res.setMessage("No orders found");
            res.setStatus(404);
            return res;
        }

        List<OrderDTO> dtoList = orders.stream()
                .map(this::mapToDTO)
                .toList();

        res.setSuccess(true);
        res.setData(dtoList);
        res.setStatus(200);
        return res;
    }
    
    @Override
    public Response getOrderByClinicBranchAndOrderId(String clinicId, String branchId, String orderId) {

        Response res = new Response();

        Optional<Order> optional =
                repository.findByClinic_ClinicIdAndBranch_BranchIdAndOrderId(
                        clinicId, branchId, orderId);

        if (optional.isEmpty()) {
            res.setSuccess(false);
            res.setMessage("Order not found");
            res.setStatus(404);
            return res;
        }

        res.setSuccess(true);
        res.setData(mapToDTO(optional.get()));
        res.setStatus(200);

        return res;
    }
    
    @Override
    public Response updateOrder(String orderId, OrderDTO dto) {

        Response res = new Response();

        Optional<Order> optional = repository.findByOrderId(orderId);

        if (optional.isEmpty()) {
            res.setSuccess(false);
            res.setMessage("Order not found");
            res.setStatus(404);
            return res;
        }

        Order order = optional.get();

     
        if (dto.getClinic() != null) {
            if (order.getClinic() == null) order.setClinic(new Clinic());

            if (dto.getClinic().getClinicId() != null)
                order.getClinic().setClinicId(dto.getClinic().getClinicId());

            if (dto.getClinic().getClinicName() != null)
                order.getClinic().setClinicName(dto.getClinic().getClinicName());
        }

   
        if (dto.getBranch() != null) {
            if (order.getBranch() == null) order.setBranch(new Branch());

            if (dto.getBranch().getBranchId() != null)
                order.getBranch().setBranchId(dto.getBranch().getBranchId());

            if (dto.getBranch().getBranchName() != null)
                order.getBranch().setBranchName(dto.getBranch().getBranchName());
        }

    
        if (dto.getSupplier() != null) {
            if (order.getSupplier() == null) order.setSupplier(new Suppliers());

            if (dto.getSupplier().getSupplierId() != null)
                order.getSupplier().setSupplierId(dto.getSupplier().getSupplierId());

            if (dto.getSupplier().getSupplierName() != null)
                order.getSupplier().setSupplierName(dto.getSupplier().getSupplierName());

            if (dto.getSupplier().getSupplierEmail() != null)
                order.getSupplier().setSupplierEmail(dto.getSupplier().getSupplierEmail());
        }

    
        if (dto.getExpectedDeliveryDate() != null)
            order.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());

        if (dto.getExpectedDeliveryDays() != 0)
            order.setExpectedDeliveryDays(dto.getExpectedDeliveryDays());

   
        if (dto.getStatusHistory() != null && !dto.getStatusHistory().isEmpty()) {

            if (order.getStatusHistory() == null) {
                order.setStatusHistory(new ArrayList<>());
            }

            for (StatusHistoryDTO shDto : dto.getStatusHistory()) {

                StatusHistory sh = new StatusHistory();
                sh.setStatus(shDto.getStatus());

                sh.setTimestamp(
                        shDto.getTimestamp() != null
                                ? shDto.getTimestamp()
                                : LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toString()
                );

                order.getStatusHistory().add(sh);
            }

        
            StatusHistory latest =
                    order.getStatusHistory().get(order.getStatusHistory().size() - 1);

            order.setOverallStatus(latest.getStatus());
        }

        
        if (dto.getOverallReason() != null)
            order.setOverallReason(dto.getOverallReason());

   
        if (dto.getProducts() != null) {

            List<Product> products = dto.getProducts().stream().map(p -> {
                Product pr = new Product();
                pr.setProductId(p.getProductId());
                pr.setProductName(p.getProductName());
                pr.setHsnCode(p.getHsnCode());
                pr.setPackSize(p.getPackSize());
                pr.setQuantityRequested(p.getQuantityRequested());
                pr.setCategory(p.getCategory());
                pr.setMrp(p.getMrp());
                pr.setGst(p.getGst());
                pr.setStatus(p.getStatus());
                pr.setRejectionReason(p.getRejectionReason());

                return pr;
            }).toList();

            order.setProducts(products);
        }

 
        if (dto.getUpdatedBy() != null)
            order.setUpdatedBy(dto.getUpdatedBy());

        order.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toString());

        repository.save(order);

        res.setSuccess(true);
        res.setMessage("Order updated successfully");
        res.setStatus(200);
        res.setData(mapToDTO(order));

        return res;
    }
    
    
    @Override
    public Response getOrderByClinicBranchAndSupplierId(
            String clinicId, String branchId, String supplierId) {

        Response res = new Response();

        List<Order> orders =
                repository.findByClinic_ClinicIdAndBranch_BranchIdAndSupplier_SupplierId(
                        clinicId, branchId, supplierId);

        if (orders.isEmpty()) {
            res.setSuccess(false);
            res.setMessage("Orders not found");
            res.setStatus(404);
            return res;
        }

        List<OrderDTO> dtoList = orders.stream()
                .map(this::mapToDTO)
                .toList();

        res.setSuccess(true);
        res.setMessage("Orders fetched successfully");
        res.setStatus(200);
        res.setData(dtoList);

        return res;
    }
    // ================= DELETE =================
    @Override
    public Response deleteOrder(String orderId) {

        Response res = new Response();

        Optional<Order> optional = repository.findByOrderId(orderId);

        if (optional.isEmpty()) {
            res.setSuccess(false);
            res.setMessage("Order not found");
            res.setStatus(404);
            return res;
        }

        repository.deleteByOrderId(orderId);

        res.setSuccess(true);
        res.setMessage("Order deleted successfully");
        res.setStatus(200);

        return res;
    }

    // ===============================
    //  DTO → ENTITY
    // ===============================
    private Order mapToEntity(OrderDTO dto) {

        Order order = new Order();

        order.setOrderId(dto.getOrderId());

  
        if (dto.getClinic() != null) {
            Clinic clinic = new Clinic();
            clinic.setClinicId(dto.getClinic().getClinicId());
            clinic.setClinicName(dto.getClinic().getClinicName());
            order.setClinic(clinic);
        }


        if (dto.getBranch() != null) {
            Branch branch = new Branch();
            branch.setBranchId(dto.getBranch().getBranchId());
            branch.setBranchName(dto.getBranch().getBranchName());
            order.setBranch(branch);
        }

      
        if (dto.getSupplier() != null) {
            Suppliers supplier = new Suppliers();
            supplier.setSupplierId(dto.getSupplier().getSupplierId());
            supplier.setSupplierName(dto.getSupplier().getSupplierName());
            supplier.setSupplierEmail(dto.getSupplier().getSupplierEmail());
            order.setSupplier(supplier);
        }

        order.setExpectedDeliveryDays(dto.getExpectedDeliveryDays());
        order.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());

      
        List<StatusHistory> historyList = new ArrayList<>();

   
      

        if (dto.getStatusHistory() != null && !dto.getStatusHistory().isEmpty()) {

            historyList = dto.getStatusHistory().stream().map(h -> {

                StatusHistory sh = new StatusHistory();
                sh.setStatus(h.getStatus());

                sh.setTimestamp(
                        h.getTimestamp() != null
                                ? h.getTimestamp()
                                : LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toString()
                );

                return sh; 

            }).toList();

        } else {

           
            StatusHistory sh = new StatusHistory();
            sh.setStatus("PENDING");
            sh.setTimestamp(
                    LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toString()
            );

            historyList.add(sh);
        }

        order.setStatusHistory(historyList);

        order.setStatusHistory(historyList);

   
        order.setOverallStatus(historyList.get(historyList.size() - 1).getStatus());

     
        if (dto.getProducts() != null) {

            List<Product> productList = dto.getProducts().stream().map(p -> {

                Product product = new Product();
                product.setProductId(p.getProductId());
                product.setProductName(p.getProductName());
                product.setHsnCode(p.getHsnCode());
                product.setPackSize(p.getPackSize());
                product.setCategory(p.getCategory());
                product.setMrp(p.getMrp());
                product.setGst(p.getGst());
                product.setPackSize(p.getPackSize());
                product.setQuantityRequested(p.getQuantityRequested());

              
                product.setStatus(p.getStatus() != null ? p.getStatus() : "PENDING");
                product.setRejectionReason(p.getRejectionReason());

                return product;

            }).toList();

            order.setProducts(productList);
        }

    
        order.setCreatedBy(dto.getCreatedBy());

        order.setCreatedAt(
                LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toString()
        );

        return order;
    }
    // ===============================
    //  ENTITY → DTO
    // ===============================
    private OrderDTO mapToDTO(Order order) {

        OrderDTO dto = new OrderDTO();

        dto.setOrderId(order.getOrderId());
        dto.setExpectedDeliveryDays(order.getExpectedDeliveryDays());
        dto.setExpectedDeliveryDate(order.getExpectedDeliveryDate());

        dto.setOverallStatus(order.getOverallStatus());
        dto.setOverallReason(order.getOverallReason());

        dto.setCreatedBy(order.getCreatedBy());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedBy(order.getUpdatedBy());
        dto.setUpdatedAt(order.getUpdatedAt());

     
        if (order.getClinic() != null) {
            ClinicDTO clinic = new ClinicDTO();
            clinic.setClinicId(order.getClinic().getClinicId());
            clinic.setClinicName(order.getClinic().getClinicName());
            dto.setClinic(clinic);
        }

   
        if (order.getBranch() != null) {
            BranchDTO branch = new BranchDTO();
            branch.setBranchId(order.getBranch().getBranchId());
            branch.setBranchName(order.getBranch().getBranchName());
            dto.setBranch(branch);
        }

     
        if (order.getSupplier() != null) {
            SuppliersDTO supplier = new SuppliersDTO();
            supplier.setSupplierId(order.getSupplier().getSupplierId());
            supplier.setSupplierName(order.getSupplier().getSupplierName());
            supplier.setSupplierEmail(order.getSupplier().getSupplierEmail());
            dto.setSupplier(supplier);
        }

     
        if (order.getStatusHistory() != null) {

            List<StatusHistoryDTO> historyList = order.getStatusHistory().stream().map(h -> {
                StatusHistoryDTO sh = new StatusHistoryDTO();
                sh.setStatus(h.getStatus());
                sh.setTimestamp(
                        h.getTimestamp() != null
                                ? h.getTimestamp()
                                : ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toString()
                );
                return sh;
            }).toList();

            dto.setStatusHistory(historyList);
        }

  
        if (order.getProducts() != null) {

            List<ProductDTO> productDTOList = order.getProducts().stream().map(p -> {

                ProductDTO pdto = new ProductDTO();
                pdto.setProductId(p.getProductId());
                pdto.setProductName(p.getProductName());
                pdto.setHsnCode(p.getHsnCode());
                pdto.setPackSize(p.getPackSize());
                pdto.setQuantityRequested(p.getQuantityRequested());
                pdto.setCategory(p.getCategory());
                pdto.setMrp(p.getMrp());
                pdto.setGst(p.getGst());
                pdto.setStatus(p.getStatus());
                pdto.setRejectionReason(p.getRejectionReason());

                return pdto;

            }).toList();

            dto.setProducts(productDTOList);
        }

        return dto;
    }
}