package com.clinicadmin.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicadmin.dto.Response;
import com.clinicadmin.service.FeedbackDetailsServcie;

@RestController
@RequestMapping("/clinic-admin")

public class FeedbackDetailsController {

    @Autowired
    private FeedbackDetailsServcie service;

    @GetMapping("/getFeedbackDetails/{clinicId}/{branchId}")
    public ResponseEntity<Response> getFeedbackDetails(
            @PathVariable String clinicId,
            @PathVariable String branchId) {

        try {

            Response response =
                    service.getFeedbackDetails(
                            clinicId,
                            branchId);

            return ResponseEntity
                    .status(response.getStatus())
                    .body(response);

        } catch (Exception e) {

            Response response = new Response();

            response.setSuccess(false);
            response.setStatus(400);
            response.setMessage(e.getMessage());
            response.setData(null);

            return ResponseEntity
                    .status(response.getStatus())
                    .body(response);
        }
    }
}
