package com.clinicadmin.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.clinicadmin.dto.AdditionalDetailsDTO;
import com.clinicadmin.dto.BillingDTO;
import com.clinicadmin.dto.PatientDTO;
import com.clinicadmin.dto.PaymentDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ServiceItemDTO;

import com.clinicadmin.entity.AdditionalDetails;
import com.clinicadmin.entity.Billing;
import com.clinicadmin.entity.Patient;
import com.clinicadmin.entity.Payment;
import com.clinicadmin.entity.ServiceItem;

import com.clinicadmin.repository.BillingRepository;
import com.clinicadmin.service.BillingService;

@Service
public class BillingServiceImpl implements BillingService {

    @Autowired
    private BillingRepository billingRepository;


    // ================= CREATE =================

    @Override
    public Response createBilling(BillingDTO billingDTO) {

        Response response = new Response();

        try {

            // DTO -> Entity
            Billing billing =
                    convertToEntity(billingDTO);

            // Generate custom unique Billing ID
            billing.setBillingId(
                    generateBillingId());

            LocalDateTime now =
                    LocalDateTime.now();

            billing.setCreatedAt(now);
            billing.setUpdatedAt(now);

            // Save into MongoDB
            Billing savedBilling =
                    billingRepository.save(billing);

            // Entity -> DTO
            BillingDTO responseDTO =
                    convertToDTO(savedBilling);

            response.setSuccess(true);
            response.setData(responseDTO);
            response.setMessage(
                    "Billing created successfully");
            response.setStatus(
                    HttpStatus.CREATED.value());

        } catch (Exception e) {

            response.setSuccess(false);
            response.setData(null);
            response.setMessage(
                    "Failed to create billing: "
                            + e.getMessage());
            response.setStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }


    // ================= GET BY BILLING ID =================

    @Override
    public Response getBillingById(
            String billingId) {

        Response response = new Response();

        try {

            Billing billing =
                    billingRepository
                            .findById(billingId)
                            .orElse(null);

            if (billing == null) {

                response.setSuccess(false);
                response.setData(null);
                response.setMessage(
                        "Billing not found");
                response.setStatus(
                        HttpStatus.NOT_FOUND.value());

                return response;
            }

            // Entity -> DTO
            BillingDTO billingDTO =
                    convertToDTO(billing);

            response.setSuccess(true);
            response.setData(billingDTO);
            response.setMessage(
                    "Billing fetched successfully");
            response.setStatus(
                    HttpStatus.OK.value());

        } catch (Exception e) {

            response.setSuccess(false);
            response.setData(null);
            response.setMessage(
                    "Failed to fetch billing: "
                            + e.getMessage());
            response.setStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }


    // ================= GET ALL =================

    @Override
    public Response getAllBillings(
            String clinicId,
            String branchId) {

        Response response = new Response();

        try {

            List<Billing> billings =
                    billingRepository
                            .findByClinicIdAndBranchId(
                                    clinicId,
                                    branchId);

            List<BillingDTO> billingDTOs =
                    billings.stream()
                            .map(this::convertToDTO)
                            .collect(
                                    Collectors.toList());

            response.setSuccess(true);
            response.setData(billingDTOs);
            response.setMessage(
                    "Billings fetched successfully");
            response.setStatus(
                    HttpStatus.OK.value());

        } catch (Exception e) {

            response.setSuccess(false);
            response.setData(null);
            response.setMessage(
                    "Failed to fetch billings: "
                            + e.getMessage());
            response.setStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }


    // ================= UPDATE =================

    @Override
    public Response updateBilling(
            String billingId,
            BillingDTO billingDTO) {

        Response response = new Response();

        try {

            Billing existingBilling =
                    billingRepository
                            .findById(billingId)
                            .orElse(null);

            if (existingBilling == null) {

                response.setSuccess(false);
                response.setData(null);
                response.setMessage("Billing not found");
                response.setStatus(
                        HttpStatus.NOT_FOUND.value());

                return response;
            }

            // ================= BASIC DETAILS =================

            if (billingDTO.getClinicId() != null) {
                existingBilling.setClinicId(
                        billingDTO.getClinicId());
            }

            if (billingDTO.getBranchId() != null) {
                existingBilling.setBranchId(
                        billingDTO.getBranchId());
            }

            if (billingDTO.getDoctorId() != null) {
                existingBilling.setDoctorId(
                        billingDTO.getDoctorId());
            }

            if (billingDTO.getVisitType() != null) {
                existingBilling.setVisitType(
                        billingDTO.getVisitType());
            }

            if (billingDTO.getBillDate() != null) {
                existingBilling.setBillDate(
                        billingDTO.getBillDate());
            }

            if (billingDTO.getInvoiceDate() != null) {
                existingBilling.setInvoiceDate(
                        billingDTO.getInvoiceDate());
            }

            if (billingDTO.getInvoiceStatus() != null) {
                existingBilling.setInvoiceStatus(
                        billingDTO.getInvoiceStatus());
            }


            // ================= PATIENT =================

            if (billingDTO.getPatient() != null) {

                Patient patient =
                        existingBilling.getPatient();

                if (patient == null) {
                    patient = new Patient();
                }

                if (billingDTO.getPatient()
                        .getPatientId() != null) {

                    patient.setPatientId(
                            billingDTO.getPatient()
                                    .getPatientId());
                }

                if (billingDTO.getPatient()
                        .getPatientName() != null) {

                    patient.setPatientName(
                            billingDTO.getPatient()
                                    .getPatientName());
                }

                if (billingDTO.getPatient()
                        .getMobileNumber() != null) {

                    patient.setMobileNumber(
                            billingDTO.getPatient()
                                    .getMobileNumber());
                }

                if (billingDTO.getPatient()
                        .getAge() != null) {

                    patient.setAge(
                            billingDTO.getPatient()
                                    .getAge());
                }

                if (billingDTO.getPatient()
                        .getGender() != null) {

                    patient.setGender(
                            billingDTO.getPatient()
                                    .getGender());
                }

                existingBilling.setPatient(patient);
            }


            // ================= SERVICES =================

            if (billingDTO.getServices() != null) {

                List<ServiceItem> services =
                        billingDTO.getServices()
                                .stream()
                                .map(serviceDTO -> {

                                    ServiceItem service =
                                            new ServiceItem();

                                    service.setServiceId(
                                            serviceDTO.getServiceId());

                                    service.setServiceName(
                                            serviceDTO.getServiceName());

                                    service.setQty(
                                            serviceDTO.getQty());

                                    service.setUnitPrice(
                                            serviceDTO.getUnitPrice());

                                    service.setDiscountPercent(
                                            serviceDTO
                                                    .getDiscountPercent());

                                    service.setTaxPercent(
                                            serviceDTO.getTaxPercent());

                                    return service;
                                })
                                .collect(Collectors.toList());

                existingBilling.setServices(services);
            }


            // ================= PAYMENT =================

            if (billingDTO.getPayment() != null) {

                Payment payment =
                        existingBilling.getPayment();

                if (payment == null) {
                    payment = new Payment();
                }

                if (billingDTO.getPayment()
                        .getPaymentMode() != null) {

                    payment.setPaymentMode(
                            billingDTO.getPayment()
                                    .getPaymentMode());
                }

                if (billingDTO.getPayment()
                        .getTransactionId() != null) {

                    payment.setTransactionId(
                            billingDTO.getPayment()
                                    .getTransactionId());
                }

                if (billingDTO.getPayment()
                        .getPaidAmount() != null) {

                    payment.setPaidAmount(
                            billingDTO.getPayment()
                                    .getPaidAmount());
                }

                if (billingDTO.getPayment()
                        .getDueAmount() != null) {

                    payment.setDueAmount(
                            billingDTO.getPayment()
                                    .getDueAmount());
                }

                if (billingDTO.getPayment()
                        .getRemarks() != null) {

                    payment.setRemarks(
                            billingDTO.getPayment()
                                    .getRemarks());
                }

                existingBilling.setPayment(payment);
            }


            // ================= ADDITIONAL DETAILS =================

            if (billingDTO.getAdditionalDetails() != null) {

                AdditionalDetails additionalDetails =
                        existingBilling.getAdditionalDetails();

                if (additionalDetails == null) {
                    additionalDetails =
                            new AdditionalDetails();
                }

                if (billingDTO.getAdditionalDetails()
                        .getBillingStaff() != null) {

                    additionalDetails.setBillingStaff(
                            billingDTO.getAdditionalDetails()
                                    .getBillingStaff());
                }

                if (billingDTO.getAdditionalDetails()
                        .getNotes() != null) {

                    additionalDetails.setNotes(
                            billingDTO.getAdditionalDetails()
                                    .getNotes());
                }

                if (billingDTO.getAdditionalDetails()
                        .getInternalComments() != null) {

                    additionalDetails.setInternalComments(
                            billingDTO.getAdditionalDetails()
                                    .getInternalComments());
                }

                existingBilling.setAdditionalDetails(
                        additionalDetails);
            }


            // ================= UPDATE TIME =================

            existingBilling.setUpdatedAt(
                    LocalDateTime.now());


            // ================= SAVE =================

            Billing savedBilling =
                    billingRepository.save(
                            existingBilling);


            // ================= ENTITY -> DTO =================

            BillingDTO responseDTO =
                    convertToDTO(savedBilling);

            response.setSuccess(true);
            response.setData(responseDTO);
            response.setMessage(
                    "Billing updated successfully");
            response.setStatus(
                    HttpStatus.OK.value());

        } catch (Exception e) {

            response.setSuccess(false);
            response.setData(null);
            response.setMessage(
                    "Failed to update billing: "
                            + e.getMessage());
            response.setStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }
    
    
 // ================= GET ALL BY CLINIC ID =================

    @Override
    public Response getAllBillingsByClinicId(
            String clinicId) {

        Response response = new Response();

        try {

            List<Billing> billings =
                    billingRepository
                            .findByClinicId(clinicId);

            List<BillingDTO> billingDTOs =
                    billings.stream()
                            .map(this::convertToDTO)
                            .collect(Collectors.toList());

            response.setSuccess(true);
            response.setData(billingDTOs);
            response.setMessage(
                    "Billings fetched successfully");
            response.setStatus(
                    HttpStatus.OK.value());

        } catch (Exception e) {

            response.setSuccess(false);
            response.setData(null);
            response.setMessage(
                    "Failed to fetch billings: "
                            + e.getMessage());
            response.setStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }


    // ================= GET ALL BILLINGS =================

    @Override
    public Response getAllBillings() {

        Response response = new Response();

        try {

            List<Billing> billings =
                    billingRepository.findAll();

            List<BillingDTO> billingDTOs =
                    billings.stream()
                            .map(this::convertToDTO)
                            .collect(Collectors.toList());

            response.setSuccess(true);
            response.setData(billingDTOs);
            response.setMessage(
                    "All billings fetched successfully");
            response.setStatus(
                    HttpStatus.OK.value());

        } catch (Exception e) {

            response.setSuccess(false);
            response.setData(null);
            response.setMessage(
                    "Failed to fetch billings: "
                            + e.getMessage());
            response.setStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }

    // ================= DELETE =================

    @Override
    public Response deleteBilling(
            String billingId) {

        Response response = new Response();

        try {

            Billing billing =
                    billingRepository
                            .findById(billingId)
                            .orElse(null);

            if (billing == null) {

                response.setSuccess(false);
                response.setData(null);
                response.setMessage(
                        "Billing not found");
                response.setStatus(
                        HttpStatus.NOT_FOUND.value());

                return response;
            }

            billingRepository.delete(billing);

            response.setSuccess(true);
            response.setData(null);
            response.setMessage(
                    "Billing deleted successfully");
            response.setStatus(
                    HttpStatus.OK.value());

        } catch (Exception e) {

            response.setSuccess(false);
            response.setData(null);
            response.setMessage(
                    "Failed to delete billing: "
                            + e.getMessage());
            response.setStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }


    // =========================================================
    // DTO -> ENTITY CONVERSION
    // =========================================================

    private Billing convertToEntity(
            BillingDTO dto) {

        if (dto == null) {
            return null;
        }

        Billing billing =
                new Billing();

        billing.setBillingId(
                dto.getBillingId());

        billing.setClinicId(
                dto.getClinicId());

        billing.setBranchId(
                dto.getBranchId());

        billing.setDoctorId(
                dto.getDoctorId());

        billing.setVisitType(
                dto.getVisitType());

        billing.setBillDate(
                dto.getBillDate());

        billing.setInvoiceDate(
                dto.getInvoiceDate());

        billing.setInvoiceStatus(
                dto.getInvoiceStatus());


        // ================= PATIENT =================

        if (dto.getPatient() != null) {

            Patient patient =
                    new Patient();

            patient.setPatientId(
                    dto.getPatient()
                            .getPatientId());

            patient.setPatientName(
                    dto.getPatient()
                            .getPatientName());

            patient.setMobileNumber(
                    dto.getPatient()
                            .getMobileNumber());

            patient.setAge(
                    dto.getPatient()
                            .getAge());

            patient.setGender(
                    dto.getPatient()
                            .getGender());

            billing.setPatient(patient);
        }


        // ================= SERVICES =================

        if (dto.getServices() != null) {

            List<ServiceItem> services =
                    dto.getServices()
                            .stream()
                            .map(serviceDTO -> {

                                ServiceItem service =
                                        new ServiceItem();

                                service.setServiceId(
                                        serviceDTO
                                                .getServiceId());

                                service.setServiceName(
                                        serviceDTO
                                                .getServiceName());

                                service.setQty(
                                        serviceDTO
                                                .getQty());

                                service.setUnitPrice(
                                        serviceDTO
                                                .getUnitPrice());

                                service.setDiscountPercent(
                                        serviceDTO
                                                .getDiscountPercent());

                                service.setTaxPercent(
                                        serviceDTO
                                                .getTaxPercent());

                                return service;
                            })
                            .collect(
                                    Collectors.toList());

            billing.setServices(services);
        }


        // ================= PAYMENT =================

        if (dto.getPayment() != null) {

            Payment payment =
                    new Payment();

            payment.setPaymentMode(
                    dto.getPayment()
                            .getPaymentMode());

            payment.setTransactionId(
                    dto.getPayment()
                            .getTransactionId());

            payment.setPaidAmount(
                    dto.getPayment()
                            .getPaidAmount());

            payment.setDueAmount(
                    dto.getPayment()
                            .getDueAmount());

            payment.setRemarks(
                    dto.getPayment()
                            .getRemarks());

            billing.setPayment(payment);
        }


        // ================= ADDITIONAL DETAILS =================

        if (dto.getAdditionalDetails()
                != null) {

            AdditionalDetails additionalDetails =
                    new AdditionalDetails();

            additionalDetails.setBillingStaff(
                    dto.getAdditionalDetails()
                            .getBillingStaff());

            additionalDetails.setNotes(
                    dto.getAdditionalDetails()
                            .getNotes());

            additionalDetails.setInternalComments(
                    dto.getAdditionalDetails()
                            .getInternalComments());

            billing.setAdditionalDetails(
                    additionalDetails);
        }

        return billing;
    }


    // =========================================================
    // ENTITY -> DTO CONVERSION
    // =========================================================

    private BillingDTO convertToDTO(
            Billing billing) {

        if (billing == null) {
            return null;
        }

        BillingDTO dto =
                new BillingDTO();

        dto.setBillingId(
                billing.getBillingId());

        dto.setClinicId(
                billing.getClinicId());

        dto.setBranchId(
                billing.getBranchId());

        dto.setDoctorId(
                billing.getDoctorId());

        dto.setVisitType(
                billing.getVisitType());

        dto.setBillDate(
                billing.getBillDate());

        dto.setInvoiceDate(
                billing.getInvoiceDate());

        dto.setInvoiceStatus(
                billing.getInvoiceStatus());


        // ================= PATIENT =================

        if (billing.getPatient()
                != null) {

            PatientDTO patientDTO =
                    new PatientDTO();

            patientDTO.setPatientId(
                    billing.getPatient()
                            .getPatientId());

            patientDTO.setPatientName(
                    billing.getPatient()
                            .getPatientName());

            patientDTO.setMobileNumber(
                    billing.getPatient()
                            .getMobileNumber());

            patientDTO.setAge(
                    billing.getPatient()
                            .getAge());

            patientDTO.setGender(
                    billing.getPatient()
                            .getGender());

            dto.setPatient(patientDTO);
        }


        // ================= SERVICES =================

        if (billing.getServices()
                != null) {

            List<ServiceItemDTO> services =
                    billing.getServices()
                            .stream()
                            .map(service -> {

                                ServiceItemDTO serviceDTO =
                                        new ServiceItemDTO();

                                serviceDTO.setServiceId(
                                        service
                                                .getServiceId());

                                serviceDTO.setServiceName(
                                        service
                                                .getServiceName());

                                serviceDTO.setQty(
                                        service
                                                .getQty());

                                serviceDTO.setUnitPrice(
                                        service
                                                .getUnitPrice());

                                serviceDTO.setDiscountPercent(
                                        service
                                                .getDiscountPercent());

                                serviceDTO.setTaxPercent(
                                        service
                                                .getTaxPercent());

                                return serviceDTO;
                            })
                            .collect(
                                    Collectors.toList());

            dto.setServices(services);
        }


        // ================= PAYMENT =================

        if (billing.getPayment()
                != null) {

            PaymentDTO paymentDTO =
                    new PaymentDTO();

            paymentDTO.setPaymentMode(
                    billing.getPayment()
                            .getPaymentMode());

            paymentDTO.setTransactionId(
                    billing.getPayment()
                            .getTransactionId());

            paymentDTO.setPaidAmount(
                    billing.getPayment()
                            .getPaidAmount());

            paymentDTO.setDueAmount(
                    billing.getPayment()
                            .getDueAmount());

            paymentDTO.setRemarks(
                    billing.getPayment()
                            .getRemarks());

            dto.setPayment(paymentDTO);
        }


        // ================= ADDITIONAL DETAILS =================

        if (billing.getAdditionalDetails()
                != null) {

            AdditionalDetailsDTO additionalDetailsDTO =
                    new AdditionalDetailsDTO();

            additionalDetailsDTO.setBillingStaff(
                    billing.getAdditionalDetails()
                            .getBillingStaff());

            additionalDetailsDTO.setNotes(
                    billing.getAdditionalDetails()
                            .getNotes());

            additionalDetailsDTO.setInternalComments(
                    billing.getAdditionalDetails()
                            .getInternalComments());

            dto.setAdditionalDetails(
                    additionalDetailsDTO);
        }

        return dto;
    }


    // =========================================================
    // GENERATE CUSTOM BILLING ID
    // =========================================================

    private String generateBillingId() {

        String uniquePart =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();

        return "BILL-" + uniquePart;
    }
}