package com.chiselon.clinicadmin.service;

import org.springframework.http.ResponseEntity;

import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.dto.TherophyProgramsDTO;

public interface TherophyProgramService {

	 ResponseEntity<Response> create(TherophyProgramsDTO dto);

	    ResponseEntity<Response> getById(String id);

	    ResponseEntity<Response> getAll();

	    ResponseEntity<Response> update(String id, TherophyProgramsDTO dto);

	    ResponseEntity<Response> delete(String id);
	    public ResponseEntity<Response> getByclinicAndBranchIdAndId(String cid,String bid,String id);
	    public ResponseEntity<Response> getByclinicAndBranchId(String cid,String bid);
	    	     
}
