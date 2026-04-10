//package com.AdminService.service;
//
//import com.AdminService.dto.DoctorSlotDTO;
//import com.AdminService.util.Response;
//
//public interface DoctorSlotService {
//
//    Response addDoctorSlot(String hospitalId, String branchId, String doctorId, DoctorSlotDTO slotDto);
//
//    Response getDoctorSlots(String hospitalId, String branchId, String doctorId);
//}


package com.AdminService.service;

import com.AdminService.dto.DoctorSlotDTO;
import com.AdminService.dto.UpdateSlotRequestDTO;
import com.AdminService.util.Response;

public interface DoctorSlotService {

    // Existing methods
    Response addDoctorSlot(String hospitalId, String branchId, String doctorId, DoctorSlotDTO slotDto);

    Response getDoctorSlots(String hospitalId, String branchId, String doctorId);

    // -------------------- New methods -------------------- //
    Response updateDoctorSlot(UpdateSlotRequestDTO request);

    Response deleteDoctorSlot(String doctorId, String branchId, String date, String slot);

    Response deleteDoctorSlot(String doctorId, String date, String slot);

    Response deleteDoctorSlotsByDate(String doctorId, String date);

    Response deleteDoctorSlotsByDate(String doctorId, String branchId, String date);

    boolean updateSlotWhileBooking(String doctorId, String branchId, String date, String time);

    boolean makingFalseSlot(String doctorId, String branchId, String date, String time);

    Response generateDoctorSlots(String doctorId, String branchId, String date,
                                 int intervalMinutes, String openingTime, String closingTime);
}
