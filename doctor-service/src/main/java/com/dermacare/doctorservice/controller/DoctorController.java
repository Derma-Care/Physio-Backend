package com.dermacare.doctorservice.controller;

import static com.dermacare.doctorservice.dermacaredoctorutils.DermaCareUtils.DOCTOR;
import static com.dermacare.doctorservice.dermacaredoctorutils.DermaCareUtils.LOGIN_DOCTOR;
import static com.dermacare.doctorservice.dermacaredoctorutils.DermaCareUtils.UPDATE_DOCTOR_AVAILABILITY;
import static com.dermacare.doctorservice.dermacaredoctorutils.DermaCareUtils.UPDATE_PASSWORD;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dermacare.doctorservice.dto.ChangeDoctorPasswordDTO;
import com.dermacare.doctorservice.dto.DoctorAvailabilityStatusDTO;
import com.dermacare.doctorservice.dto.DoctorLoginDTO;
import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.service.DoctorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value=DOCTOR)
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DoctorController {
	@Autowired
    private final DoctorService doctorService;

    
    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }


    @PutMapping(value = UPDATE_PASSWORD)
    public Response updatePassword(@PathVariable String username,
            @RequestBody ChangeDoctorPasswordDTO updatePasswordDTO) {       
    	Response response = doctorService.changePassword(username, updatePasswordDTO);
//    	System.out.println(response);
        return response;
   }
    @PostMapping(value=LOGIN_DOCTOR)
    public ResponseEntity<Response> login(@Valid @RequestBody DoctorLoginDTO dto) {
		Response res = doctorService.login(dto);
		 if(res!=null) {
			 return ResponseEntity.status(res.getStatus()).body(res);
		 }
		return null;}
    
    @PutMapping(value = UPDATE_DOCTOR_AVAILABILITY)
    
    public Response updateDoctorAvailability(@PathVariable String doctorId,@RequestBody  DoctorAvailabilityStatusDTO availabilityDTO) {
    	 
    	return doctorService.updateDoctorAvailability(doctorId, availabilityDTO);
    	
    }
    
    
 //New Apis
    
    @GetMapping("/getAllDoctors")
    public ResponseEntity<?> getAllDoctors(){
    	return doctorService.getAllDoctors();
    }
    
    
    @GetMapping("/getDoctorById/{id}")
    public ResponseEntity<?> getDoctorById(@PathVariable String id){
    	return doctorService.getDoctorById(id);
    }
    
    @GetMapping("/getDoctorByClinicAndDoctorId/{clinicId}/{doctorId}")
    public ResponseEntity<?> getDoctorByClinicAndDoctorId(@PathVariable String clinicId,
    		@PathVariable String doctorId){
    	return doctorService.getDoctorByClinicAndDoctorId(clinicId, doctorId);
    }
    
    
    @GetMapping("/getDoctorsByHospitalById/{clinicId}")
    public ResponseEntity<?> getDoctorsByHospitalById(@PathVariable String clinicId){
    	return doctorService.getDoctorsByHospitalById(clinicId);
    }
    
    
    @GetMapping("/getDoctorsBySubServiceId/{hsptlId}/{subServiceId}")
    public ResponseEntity<?> getDoctorsBySubServiceId(@PathVariable String hsptlId,@PathVariable String subServiceId){
    	return doctorService.getDoctorsBySubServiceId(hsptlId, subServiceId);
    }
    
    
    @GetMapping("/getAllDoctorsBySubServiceId/{subServiceId}")
    public ResponseEntity<?> getAllDoctorsBySubServiceId(@PathVariable String subServiceId){
    	return doctorService.getAllDoctorsBySubServiceId(subServiceId);
    }
    
    @GetMapping("/getDoctorFutureAppointments/{doctorId}")
    public ResponseEntity<?> getDoctorFutureAppointments(@PathVariable String doctorId) {
        return doctorService.getDoctorFutureAppointments(doctorId);
    }
    
    @GetMapping("/getDiseasesByHospitalId/{hospitalId}")
    public ResponseEntity<Response> getDiseases(@PathVariable String hospitalId) {
        return doctorService.getDiseasesFromClinicAdmin(hospitalId);
    }
    
    @GetMapping("/getLabTestsByHospitalId/{hospitalId}")
    public ResponseEntity<Response> getLabTests(
            @PathVariable String hospitalId) {

        return doctorService.getLabTestsFromClinicAdmin(hospitalId);
    }
}
