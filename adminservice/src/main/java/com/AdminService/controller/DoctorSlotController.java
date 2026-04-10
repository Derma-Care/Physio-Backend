package com.AdminService.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.AdminService.dto.DoctorSlotDTO;
import com.AdminService.dto.UpdateSlotRequestDTO;
import com.AdminService.service.DoctorSlotService;
import com.AdminService.util.Response;

@RestController
@RequestMapping("/admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DoctorSlotController {

    @Autowired
    private DoctorSlotService doctorSlotService;

    // ---------------------- Existing APIs ---------------------- //
    @PostMapping("/addDoctorSlots/{hospitalId}/{branchId}/{doctorId}")
    public ResponseEntity<Response> addDoctorSlot(@PathVariable String hospitalId,
                                                  @PathVariable String branchId,
                                                  @PathVariable String doctorId,
                                                  @RequestBody DoctorSlotDTO slotDto) {
        Response response = doctorSlotService.addDoctorSlot(hospitalId, branchId, doctorId, slotDto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/getDoctorSlots/{hospitalId}/{branchId}/{doctorId}")
    public ResponseEntity<Response> getDoctorSlots(@PathVariable String hospitalId,
                                                   @PathVariable String branchId,
                                                   @PathVariable String doctorId) {
        Response response = doctorSlotService.getDoctorSlots(hospitalId, branchId, doctorId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ---------------------- Update Doctor Slot ---------------------- //
    @PutMapping("/updateDoctorSlot")
    public ResponseEntity<Response> updateDoctorSlot(@RequestBody UpdateSlotRequestDTO request) {
        Response response = doctorSlotService.updateDoctorSlot(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ---------------------- Delete Doctor Slot by Slot ---------------------- //
    @DeleteMapping("/deleteDoctorSlot/{doctorId}/{branchId}/{date}/{slot}")
    public ResponseEntity<Response> deleteDoctorSlot(@PathVariable String doctorId,
                                                     @PathVariable String branchId,
                                                     @PathVariable String date,
                                                     @PathVariable String slot) {
        Response response = doctorSlotService.deleteDoctorSlot(doctorId, branchId, date, slot);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ---------------------- Delete Doctor Slot by Slot (no branch) ---------------------- //
    @DeleteMapping("/deleteDoctorSlot/{doctorId}/{date}/{slot}")
    public ResponseEntity<Response> deleteDoctorSlot(@PathVariable String doctorId,
                                                     @PathVariable String date,
                                                     @PathVariable String slot) {
        Response response = doctorSlotService.deleteDoctorSlot(doctorId, date, slot);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ---------------------- Delete All Doctor Slots by Date ---------------------- //
    @DeleteMapping("/deleteDoctorSlotsByDate/{doctorId}/{date}")
    public ResponseEntity<Response> deleteDoctorSlotsByDate(@PathVariable String doctorId,
                                                            @PathVariable String date) {
        Response response = doctorSlotService.deleteDoctorSlotsByDate(doctorId, date);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ---------------------- Delete All Doctor Slots by Date and Branch ---------------------- //
    @DeleteMapping("/deleteDoctorSlotsByDate/{doctorId}/{branchId}/{date}")
    public ResponseEntity<Response> deleteDoctorSlotsByDate(@PathVariable String doctorId,
                                                            @PathVariable String branchId,
                                                            @PathVariable String date) {
        Response response = doctorSlotService.deleteDoctorSlotsByDate(doctorId, branchId, date);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ---------------------- Update Slot While Booking ---------------------- //
    @PutMapping("/updateSlotWhileBooking/{doctorId}/{branchId}/{date}/{time}")
    public boolean updateSlotWhileBooking(@PathVariable String doctorId,
                                          @PathVariable String branchId,
                                          @PathVariable String date,
                                          @PathVariable String time) {
        return doctorSlotService.updateSlotWhileBooking(doctorId, branchId, date, time);
    }

    // ---------------------- Make Slot False ---------------------- //
    @PutMapping("/makingFalseSlot/{doctorId}/{branchId}/{date}/{time}")
    public boolean makingFalseSlot(@PathVariable String doctorId,
                                   @PathVariable String branchId,
                                   @PathVariable String date,
                                   @PathVariable String time) {
        return doctorSlotService.makingFalseSlot(doctorId, branchId, date, time);
    }

    // ---------------------- Generate Doctor Slots ---------------------- //
    @GetMapping("/generateDoctorSlots/{doctorId}/{branchId}/{date}/{intervalMinutes}/{openingTime}/{closingTime}")
    public Response generateDoctorSlots(@PathVariable String doctorId,
                                        @PathVariable String branchId,
                                        @PathVariable String date,
                                        @PathVariable int intervalMinutes,
                                        @PathVariable String openingTime,
                                        @PathVariable String closingTime) {
        return doctorSlotService.generateDoctorSlots(doctorId, branchId, date, intervalMinutes, openingTime, closingTime);
    }
}
