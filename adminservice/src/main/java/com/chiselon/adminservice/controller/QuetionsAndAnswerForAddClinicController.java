package com.chiselon.adminservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chiselon.adminservice.dto.QuetionsAndAnswerForAddClinicDTO;
import com.chiselon.adminservice.service.QuetionsAndAnswerForAddClinicService;
import com.chiselon.adminservice.util.Response;

@RestController
@RequestMapping("/admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class QuetionsAndAnswerForAddClinicController {

    @Autowired
    private QuetionsAndAnswerForAddClinicService quetionsAndAnswerForAddClinicService;

    // ------------------------Add questions & answers---------------------------------
    @PostMapping("/clinicQuestions/addQuestions")
    public ResponseEntity<Response> addQuestions(@RequestBody QuetionsAndAnswerForAddClinicDTO clinicQA) {
        Response res = quetionsAndAnswerForAddClinicService.saveQuetions(clinicQA);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // -----------------------Get all questions----------------------------------------------
    @GetMapping("/clinicQuestions/getAll")
    public ResponseEntity<Response> getAllQuestions() {
        Response res = quetionsAndAnswerForAddClinicService.getQuetions();
        return ResponseEntity.status(res.getStatus()).body(res);
    }


    //------------------------- Update questions by Id ----------------------------------------------
    @PutMapping("/clinicQuestions/update")
    public ResponseEntity<Response> updateQuestions(@RequestBody QuetionsAndAnswerForAddClinicDTO clinicQA) {

        Response res = quetionsAndAnswerForAddClinicService.updateQuetions(clinicQA);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}