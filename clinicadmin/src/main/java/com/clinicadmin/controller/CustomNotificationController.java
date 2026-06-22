package com.clinicadmin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.clinicadmin.dto.CustomNotificationRequest;
import com.clinicadmin.service.impl.CustomWhatsAppService;


@RestController
@RequestMapping("/clinic-admin")
@RequiredArgsConstructor
@Slf4j
public class CustomNotificationController {

    private final CustomWhatsAppService customWhatsAppService;

    @PostMapping("/notifications/whatsapp/custom")
    public ResponseEntity<String> sendCustomWhatsApp(
            @RequestBody CustomNotificationRequest request) {

        log.info("CustomWhatsApp request received clinicName={} branchName={} patients={}",
                request.getClinicName(),
                request.getBranchName(),
                request.getList() != null ? request.getList().size() : 0);

        new Thread(() -> customWhatsAppService.sendToAll(request)).start();

        return ResponseEntity.ok("Notification dispatch initiated");
    }
}