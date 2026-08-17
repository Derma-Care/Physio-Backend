package com.chiselon.customerservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chiselon.customerservice.dto.MutiplePartsDto;
import com.chiselon.customerservice.dto.QuestionsByPartDTO;
import com.chiselon.customerservice.dto.QuestionsDTO;
import com.chiselon.customerservice.service.PhysiotherapyService;
import com.chiselon.customerservice.util.PysioQuestionsRes;
import com.chiselon.customerservice.util.Response;

@RestController
@RequestMapping("/customer/physiotherapy/questions")
public class PhysiotherapyController {

    @Autowired
    private PhysiotherapyService service;
    
    @PostMapping("/create")
    public ResponseEntity<Response> create(@RequestBody QuestionsByPartDTO dto) {
        return service.create(dto);
    }

    @GetMapping("/getAll")
    public ResponseEntity<PysioQuestionsRes> getAll() {
        return service.getAll();
    }

    @PutMapping("/updateByKey/{key}")
    public ResponseEntity<Response> updateByKey(@PathVariable String key,
                                                @RequestBody QuestionsDTO dto) {
        return service.updateByKey(key, dto);
    }

    @DeleteMapping("/deleteByKey/{key}/{qId}")
    public ResponseEntity<Response> deleteByKey(@PathVariable String key,@PathVariable long qId) {
        return service.deleteQuestionByKeyAndId(key, qId );
    }

    @PostMapping("/getByKey")
    public ResponseEntity<Response> getByKey(@RequestBody MutiplePartsDto keys) {
        return service.getByKeys(keys);
    }
      
  }