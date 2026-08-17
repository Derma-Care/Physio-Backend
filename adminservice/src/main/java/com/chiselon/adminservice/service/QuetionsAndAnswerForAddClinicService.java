package com.chiselon.adminservice.service;

import com.chiselon.adminservice.dto.QuetionsAndAnswerForAddClinicDTO;
import com.chiselon.adminservice.util.Response;

public interface QuetionsAndAnswerForAddClinicService {

	Response saveQuetions(QuetionsAndAnswerForAddClinicDTO dto);

	Response updateQuetions(QuetionsAndAnswerForAddClinicDTO dto);

	Response getQuetions();



}