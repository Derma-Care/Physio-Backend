//package com.dermacare.doctorservice.service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import com.dermacare.doctorservice.dto.BookingResponse;
//import com.dermacare.doctorservice.dto.DoctorNotesDTO;
//import com.dermacare.doctorservice.dto.ExtractFeignMessage;
//import com.dermacare.doctorservice.dto.NotificationDTO;
//import com.dermacare.doctorservice.dto.Response;
//import com.dermacare.doctorservice.dto.ResponseStructure;
//import com.dermacare.doctorservice.feignclient.BookingFeignClient;
//import com.dermacare.doctorservice.feignclient.NotificationFeign;
//import com.dermacare.doctorservice.model.DoctorNotes;
//import com.dermacare.doctorservice.repository.DoctorNotesRepository;
//import feign.FeignException;
//
//@Service
//public class DoctorNotesServiceImpl implements DoctorNotesService {
//
//    @Autowired
//    private DoctorNotesRepository repository;
//
//    @Autowired
//    private BookingFeignClient bookingFeignClient;
//    
//    @Autowired
//    private NotificationFeign notificationFeign;
//   
//    @Override
//    public Response addDoctorNote(DoctorNotesDTO dto) {
//        try {
//            ResponseEntity<ResponseStructure<BookingResponse>> bookingResponseEntity =
//                bookingFeignClient.getBookedService(dto.getBookingId());     
//            NotificationDTO notification = notificationFeign.getNotificationByBookingId(dto.getBookingId());
//            if (bookingResponseEntity.getStatusCode().is2xxSuccessful()
//                && bookingResponseEntity.getBody() != null
//                && bookingResponseEntity.getBody().getData() != null) {
//                BookingResponse bookingData = bookingResponseEntity.getBody().getData();
//                DoctorNotes note = new DoctorNotes();
//                note.setBookingId(dto.getBookingId());
//                note.setDoctorId(bookingData.getDoctorId());
//                note.setPatientPhoneNumber(bookingData.getMobileNumber());
//                note.setNotes(dto.getNotes());
//                DoctorNotes saved = repository.save(note);
//               
//                bookingData.setStatus("Completed");
//                bookingData.setNotes(saved.getNotes());            
//                bookingFeignClient.updateAppointment(bookingData); 
//                
//                if(notification != null){
//                	notification.getData().setStatus("Completed");
//                	notificationFeign.updateNotification(notification);}
//                
//                DoctorNotes responseNote = new DoctorNotes();
//                responseNote.setBookingId(saved.getBookingId());
//                responseNote.setDoctorId(saved.getDoctorId());
//                responseNote.setPatientPhoneNumber(saved.getPatientPhoneNumber());
//                if (saved.getNotes() == null || saved.getNotes().isBlank()) {
//                    responseNote.setNotes("No notes provided by the doctor");
//                } else {
//                    responseNote.setNotes(saved.getNotes());
//                }
//                return new Response(true, responseNote, "Doctor note added and booking marked as COMPLETED", 201);
//            } else {
//                return new Response(false, null, "Invalid booking ID", 404);
//            }
//
//        } catch (FeignException fe) {           
//            return new Response(false, null, ExtractFeignMessage.clearMessage(fe), fe.status());
//        }
//    }
//
//
//
//    @Override
//    public Response getAllDoctorNotes() {
//        List<DoctorNotes> notes = repository.findAll();
//
//        List<DoctorNotesDTO> dtos = notes.stream().map(note -> {
//            DoctorNotesDTO dto = new DoctorNotesDTO();          
//            dto.setBookingId(note.getBookingId());
//            dto.setDoctorId(note.getDoctorId());
//            dto.setPatientPhoneNumber(note.getPatientPhoneNumber());
//            dto.setNotes(note.getNotes());
//            return dto;
//        }).collect(Collectors.toList());
//        if(dtos != null) {
//        return new Response(true, dtos, "Fetched all doctor notes successfully", 200);}
//        else {
//        	 return new Response(true, dtos, "Doctors are Not Found", 200);}
//        }
//    
//    
//    @Override
//    public  Response getNoteByDoctorId(String doctorId ) {
//    	try {
//    	DoctorNotes note = repository.findByDoctorId( doctorId );
//    	 DoctorNotesDTO dto = new DoctorNotesDTO();          
//         dto.setBookingId(note.getBookingId());
//         dto.setDoctorId(note.getDoctorId());
//         dto.setPatientPhoneNumber(note.getPatientPhoneNumber());
//         dto.setNotes(note.getNotes());
//         if(dto != null) {
//             return new Response(true, dto, "Fetched doctor notes successfully", 200);
//             }else{
//             	 return new Response(true, null, "doctors are Not Found", 200);}
//             }catch(Exception e) {
//            	 return new Response(true, null,e.getMessage(), 500);
//             }
//    }}



