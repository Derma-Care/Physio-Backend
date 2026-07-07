//package physiotherapydoctor.util;
//
//import java.util.List;
//import java.util.Map;
//
//import org.springframework.data.domain.Page;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Component;
//
//import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
//import io.github.resilience4j.retry.annotation.Retry;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import physiotherapydoctor.dto.BookingRequset;
//import physiotherapydoctor.dto.BookingResponse;
//import physiotherapydoctor.dto.BranchDTO;
//import physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
//import physiotherapydoctor.dto.ClinicInfoDTO;
//import physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
//import physiotherapydoctor.dto.DoctorsDTO;
//import physiotherapydoctor.dto.NotificationDTO;
//import physiotherapydoctor.dto.ResBody;
//import physiotherapydoctor.dto.Response;
//import physiotherapydoctor.dto.ResponseStructure;
//import physiotherapydoctor.dto.TherapistRecordDTO;
//import physiotherapydoctor.dto.TreatmentDTO;
//import physiotherapydoctor.dto.VitalsDTO;
//import physiotherapydoctor.feign.AdminFeignClient;
//import physiotherapydoctor.feign.BookingFeignClient;
//import physiotherapydoctor.feign.ClinicAdminFeign;
//import physiotherapydoctor.feign.NotificationFeign;
//
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class FeignImpl {
//	
//	    private final AdminFeignClient adminFeignClient;
//	    private final BookingFeignClient bookingFeignClient;
//	    private final ClinicAdminFeign clinicAdminFeign;
//	    private final KeyCloakTokenStore keyCloakTokenStore;
//	    private final NotificationFeign notificationFeign;
//	    
//	    private String token() {
//	        return keyCloakTokenStore.getAccess_token();
//	    }
//
//	    private Response buildRateLimitResponse(Exception ex) {
//	        Response response = new Response();
//	        response.setSuccess(false);
//	        response.setStatus(503);
//	        response.setMessage("Service temporarily unavailable. Please try again later.");
//	        response.setData(null);
//	        return response;
//	    }
//
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getClinicByIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getClinicByIdFallback")
//	    public ResponseEntity<Response> getClinicById(String clinicId) {
//
//	        return adminFeignClient.getClinicById(keyCloakTokenStore.getAccess_token(), clinicId);
//	            
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getBranchByIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getBranchByIdFallback")
//	    public ResponseEntity<ResponseStructure<BranchDTO>> getBranchById(String branchId) {
//
//	        return adminFeignClient.getBranchById(keyCloakTokenStore.getAccess_token(),branchId);
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="getBookedServiceFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="getBookedServiceFallback")
//	    public ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(String id){
//	        return bookingFeignClient.getBookedService(token(), id);
//	    }
//	    private ResponseEntity<ResponseStructure<BookingResponse>> getBookedServiceFallback(String id, Exception ex){
//	        throw new RuntimeException("physiotherapydoctorService unavailable", ex);
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="bookingByPatientIdFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="bookingByPatientIdFallback")
//	    public ResponseEntity<Page<BookingResponse>> bookingByPatientId(String clinicId,String patientId,int page,int size){
//	        return bookingFeignClient.bookingByPatientId(token(), clinicId, patientId, page, size);
//	    }
//	    private ResponseEntity<Page<BookingResponse>> bookingByPatientIdFallback(String clinicId,String patientId,int page,int size,Exception ex){
//	        throw new RuntimeException("physiotherapydoctorService unavailable", ex);
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="getAppointsByInputFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="getAppointsByInputFallback")
//	    public ResponseEntity<?> getAppointsByInput(String input){
//	        return bookingFeignClient.getAppointsByInput(token(), input);
//	    }
//	    private ResponseEntity<?> getAppointsByInputFallback(String input, Exception ex){
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildRateLimitResponse(ex));
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="getTodayDoctorAppointmentsByDoctorIdFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="getTodayDoctorAppointmentsByDoctorIdFallback")
//	    public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(String clinicId,String doctorId,int page,int size){
//	        return bookingFeignClient.getTodayDoctorAppointmentsByDoctorId(token(), clinicId, doctorId, page, size);
//	    }
//	    private ResponseEntity<?> getTodayDoctorAppointmentsByDoctorIdFallback(String clinicId,String doctorId,int page,int size,Exception ex){
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildRateLimitResponse(ex));
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="filterDoctorAppointmentsByDoctorIdFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="filterDoctorAppointmentsByDoctorIdFallback")
//	    public ResponseEntity<?> filterDoctorAppointmentsByDoctorId(String clinicId,String doctorId,String number){
//	        return bookingFeignClient.filterDoctorAppointmentsByDoctorId(token(), clinicId, doctorId, number);
//	    }
//	    private ResponseEntity<?> filterDoctorAppointmentsByDoctorIdFallback(String clinicId,String doctorId,String number,Exception ex){
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildRateLimitResponse(ex));
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="completedAppointmentsFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="completedAppointmentsFallback")
//	    public ResponseEntity<?> completedAppointments(String clinicId,String doctorId){
//	        return bookingFeignClient.filterDoctorAppointmentsByDoctorId(token(), clinicId, doctorId);
//	    }
//	    private ResponseEntity<?> completedAppointmentsFallback(String clinicId,String doctorId,Exception ex){
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildRateLimitResponse(ex));
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="getSizeOfConsultationTypesByDoctorIdFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="getSizeOfConsultationTypesByDoctorIdFallback")
//	    public ResponseEntity<?> getSizeOfConsultationTypesByDoctorId(String clinicId,String doctorId){
//	        return bookingFeignClient.getSizeOfConsultationTypesByDoctorId(token(), clinicId, doctorId);
//	    }
//	    private ResponseEntity<?> getSizeOfConsultationTypesByDoctorIdFallback(String clinicId,String doctorId,Exception ex){
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildRateLimitResponse(ex));
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="inProgressAppointmentsFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="inProgressAppointmentsFallback")
//	    public ResponseEntity<?> inProgressAppointments(String mobile){
//	        return bookingFeignClient.inProgressAppointments(token(), mobile);
//	    }
//	    private ResponseEntity<?> inProgressAppointmentsFallback(String mobile,Exception ex){
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildRateLimitResponse(ex));
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="getDoctorFutureAppointmentsFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="getDoctorFutureAppointmentsFallback")
//	    public ResponseEntity<?> getDoctorFutureAppointments(String doctorId,int page,int size){
//	        return bookingFeignClient.getDoctorFutureAppointments(token(), doctorId, page, size);
//	    }
//
//	    private ResponseEntity<?> getDoctorFutureAppointmentsFallback(String doctorId,int page,int size,Exception ex){
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildRateLimitResponse(ex));
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="bookingByDoctorIdFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="bookingByDoctorIdFallback")
//	    public ResponseEntity<?> bookingByDoctorId(String doctorId,int page,int size){
//	        return bookingFeignClient.bookingByDoctorId(token(), doctorId, page, size);
//	    }
//
//	    private ResponseEntity<?> bookingByDoctorIdFallback(String doctorId,int page,int size,Exception ex){
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildRateLimitResponse(ex));
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="bookServiceFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="bookServiceFallback")
//	    public ResponseEntity<?> bookService(BookingRequset request){
//	        return bookingFeignClient.bookService(request);
//	    }
//
//	    private ResponseEntity<?> bookServiceFallback(BookingRequset request,Exception ex){
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildRateLimitResponse(ex));
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="getInProgressAppointmentByPatientIdAndBookingIdFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="getInProgressAppointmentByPatientIdAndBookingIdFallback")
//	    public ResponseEntity<?> getInProgressAppointmentByPatientIdAndBookingId(String patientId,String bookingId){
//	        return bookingFeignClient.getInProgressAppointmentByPatientIdAndBookingId(token(), patientId, bookingId);
//	    }
//
//	    private ResponseEntity<?> getInProgressAppointmentByPatientIdAndBookingIdFallback(String patientId,String bookingId,Exception ex){
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildRateLimitResponse(ex));
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="updateAppointmentBasedOnBookingIdFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="updateAppointmentBasedOnBookingIdFallback")
//	    public ResponseEntity<?> updateAppointmentBasedOnBookingId(BookingResponse bookingResponse){
//	        return bookingFeignClient.updateAppointmentBasedOnBookingId(token(), bookingResponse);
//	    }
//
//	    private ResponseEntity<?> updateAppointmentBasedOnBookingIdFallback(BookingResponse bookingResponse,Exception ex){
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildRateLimitResponse(ex));
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatusFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatusFallback")
//	    public ResponseEntity<?> getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatus(String clinicId,String branchId,String doctorId,String status,int page,int size){
//	        return bookingFeignClient.getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatus(token(), clinicId, branchId, doctorId, status, page, size);
//	    }
//
//	    private ResponseEntity<?> getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatusFallback(String clinicId,String branchId,String doctorId,String status,int page,int size,Exception ex){
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildRateLimitResponse(ex));
//	    }
//
//	    @CircuitBreaker(name="physiotherapydoctorService", fallbackMethod="searchBookingsFallback")
//	    @Retry(name="physiotherapydoctorService", fallbackMethod="searchBookingsFallback")
//	    public ResponseEntity<ResponseStructure<List<Map<String,Object>>>> searchBookings(String clinicId,String input){
//	        return bookingFeignClient.searchBookings(clinicId, input);
//	    }
//
//	    private ResponseEntity<ResponseStructure<List<Map<String,Object>>>> searchBookingsFallback(String clinicId,String input,Exception ex){
//	        throw new RuntimeException("physiotherapydoctorService unavailable", ex);
//	    }
//
//	    
//	    
//	    
//	    private Response buildServiceUnavailableResponse(Exception ex) {
//	        Response response = new Response();
//	        response.setSuccess(false);
//	        response.setStatus(503);
//	        response.setMessage("Service temporarily unavailable. Please try again later.");
//	        response.setData(null);
//	        return response;
//	    }
//
//	    private <T> ResponseStructure<T> buildResponseStructureFallback() {
//	        return ResponseStructure.buildResponse(
//	                null,
//	                "Service temporarily unavailable. Please try again later.",
//	                HttpStatus.SERVICE_UNAVAILABLE,
//	                503);
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "doctorLoginFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "doctorLoginFallback")
//	    public ResponseEntity<Response> doctorLogin(Map<String, String> dto) {
//	        return clinicAdminFeign.doctorLogin(dto);
//	    }
//
//	    private ResponseEntity<Response> doctorLoginFallback(
//	            Map<String, String> dto,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "changePasswordFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "changePasswordFallback")
//	    public Response changePassword(
//	            String username,
//	            ChangeDoctorPasswordDTO updateDTO) {
//
//	        return clinicAdminFeign.changePassword(
//	                keyCloakTokenStore.getAccess_token(),
//	                username,
//	                updateDTO);
//	    }
//
//	    private Response changePasswordFallback(
//	            String username,
//	            ChangeDoctorPasswordDTO updateDTO,
//	            Exception ex) {
//
//	        return buildServiceUnavailableResponse(ex);
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "updateDoctorAvailabilityFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "updateDoctorAvailabilityFallback")
//	    public Response updateDoctorAvailability(
//	            String doctorId,
//	            DoctorAvailabilityStatusDTO availabilityDTO) {
//
//	        return clinicAdminFeign.updateDoctorAvailability(
//	                keyCloakTokenStore.getAccess_token(),
//	                doctorId,
//	                availabilityDTO);
//	    }
//
//	    private Response updateDoctorAvailabilityFallback(
//	            String doctorId,
//	            DoctorAvailabilityStatusDTO availabilityDTO,
//	            Exception ex) {
//
//	        return buildServiceUnavailableResponse(ex);
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getBookingByIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getBookingByIdFallback")
//	    public ResponseStructure<BookingResponse> getBookingById(
//	            String bookingId) {
//
//	        return clinicAdminFeign.getBookingById(
//	                keyCloakTokenStore.getAccess_token(),
//	                bookingId);
//	    }
//
//	    private ResponseStructure<BookingResponse> getBookingByIdFallback(
//	            String bookingId,
//	            Exception ex) {
//
//	        return buildResponseStructureFallback();
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "updateAppointmentFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "updateAppointmentFallback")
//	    public ResponseEntity<?> updateAppointment(
//	            BookingResponse bookingResponse) {
//
//	        return clinicAdminFeign.updateAppointment(
//	                keyCloakTokenStore.getAccess_token(),
//	                bookingResponse);
//	    }
//
//	    private ResponseEntity<?> updateAppointmentFallback(
//	            BookingResponse bookingResponse,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getByPatientIdAndBookingIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getByPatientIdAndBookingIdFallback")
//	    public ResponseStructure<List<TherapistRecordDTO>> getByPatientIdAndBookingId(
//	            String patientId,
//	            String bookingId) {
//
//	        return clinicAdminFeign.getByPatientIdAndBookingId(
//	                keyCloakTokenStore.getAccess_token(),
//	                patientId,
//	                bookingId);
//	    }
//
//	    private ResponseStructure<List<TherapistRecordDTO>> getByPatientIdAndBookingIdFallback(
//	            String patientId,
//	            String bookingId,
//	            Exception ex) {
//
//	        return buildResponseStructureFallback();
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getRecordBySessionFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getRecordBySessionFallback")
//	    public ResponseEntity<ResponseStructure<TherapistRecordDTO>> getRecordBySession(
//	            String clinicId,
//	            String branchId,
//	            String bookingId,
//	            String patientId,
//	            String sessionId) {
//
//	        return clinicAdminFeign.getRecordBySession(
//	                keyCloakTokenStore.getAccess_token(),
//	                clinicId,
//	                branchId,
//	                bookingId,
//	                patientId,
//	                sessionId);
//	    }
//
//	    private ResponseEntity<ResponseStructure<TherapistRecordDTO>> getRecordBySessionFallback(
//	            String clinicId,
//	            String branchId,
//	            String bookingId,
//	            String patientId,
//	            String sessionId,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildResponseStructureFallback());
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getTherapistWithRequiredFiledsFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getTherapistWithRequiredFiledsFallback")
//	    public ResponseEntity<Response> getTherapistWithRequiredFileds(
//	            String clinicId,
//	            String branchId) {
//
//	        return clinicAdminFeign.getTherapistWithRequiredFileds(
//	                keyCloakTokenStore.getAccess_token(),
//	                clinicId,
//	                branchId);
//	    }
//
//	    private ResponseEntity<Response> getTherapistWithRequiredFiledsFallback(
//	            String clinicId,
//	            String branchId,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getCompletedTherapyRecordFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getCompletedTherapyRecordFallback")
//	    public ResponseEntity<ResponseStructure<TherapistRecordDTO>> getCompletedTherapyRecord(
//	            String clinicId,
//	            String branchId,
//	            String therapistRecordId,
//	            String sessionId) {
//
//	        return clinicAdminFeign.getCompletedTherapyRecord(
//	                keyCloakTokenStore.getAccess_token(),
//	                clinicId,
//	                branchId,
//	                therapistRecordId,
//	                sessionId);
//	    }
//
//	    private ResponseEntity<ResponseStructure<TherapistRecordDTO>> getCompletedTherapyRecordFallback(
//	            String clinicId,
//	            String branchId,
//	            String therapistRecordId,
//	            String sessionId,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildResponseStructureFallback());
//	    }
//	    
//	    
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "updateDoctorByIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "updateDoctorByIdFallback")
//	    public ResponseEntity<Response> updateDoctorById(
//	            String doctorId,
//	            DoctorsDTO dto) {
//
//	        return clinicAdminFeign.updateDoctorById(
//	                keyCloakTokenStore.getAccess_token(),
//	                doctorId,
//	                dto);
//	    }
//
//	    private ResponseEntity<Response> updateDoctorByIdFallback(
//	            String doctorId,
//	            DoctorsDTO dto,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "addTreatmentFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "addTreatmentFallback")
//	    public ResponseEntity<Response> addTreatment(TreatmentDTO dto) {
//
//	        return clinicAdminFeign.addTreatment(dto);
//	    }
//
//	    private ResponseEntity<Response> addTreatmentFallback(
//	            TreatmentDTO dto,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getAllDoctorsFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getAllDoctorsFallback")
//	    public ResponseEntity<Response> getAllDoctors() {
//
//	        return clinicAdminFeign.getAllDoctors(
//	                keyCloakTokenStore.getAccess_token());
//	    }
//
//	    private ResponseEntity<Response> getAllDoctorsFallback(
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getDoctorByIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getDoctorByIdFallback")
//	    public ResponseEntity<Response> getDoctorById(
//	            String id) {
//
//	        return clinicAdminFeign.getDoctorById(
//	                keyCloakTokenStore.getAccess_token(),
//	                id);
//	    }
//
//	    private ResponseEntity<Response> getDoctorByIdFallback(
//	            String id,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getDoctorByClinicAndDoctorIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getDoctorByClinicAndDoctorIdFallback")
//	    public ResponseEntity<Response> getDoctorByClinicAndDoctorId(
//	            String clinicId,
//	            String doctorId) {
//
//	        return clinicAdminFeign.getDoctorByClinicAndDoctorId(
//	                keyCloakTokenStore.getAccess_token(),
//	                clinicId,
//	                doctorId);
//	    }
//
//	    private ResponseEntity<Response> getDoctorByClinicAndDoctorIdFallback(
//	            String clinicId,
//	            String doctorId,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getDoctorsByHospitalByIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getDoctorsByHospitalByIdFallback")
//	    public ResponseEntity<Response> getDoctorsByHospitalById(
//	            String hospitalId) {
//
//	        return clinicAdminFeign.getDoctorsByHospitalById(
//	                keyCloakTokenStore.getAccess_token(),
//	                hospitalId);
//	    }
//
//	    private ResponseEntity<Response> getDoctorsByHospitalByIdFallback(
//	            String hospitalId,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
////
////	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getClinicByIdFallback")
////	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getClinicByIdFallback")
////	    public ResponseEntity<Response> getClinicById(
////	            String clinicId) {
////
////	        return clinicAdminFeign.getClinicById(
////	                keyCloakTokenStore.getAccess_token(),
////	                clinicId);
////	    }
//
//	    private ResponseEntity<Response> getClinicByIdFallback(
//	            String clinicId,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getClinicInfoByDoctorIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getClinicInfoByDoctorIdFallback")
//	    public ClinicInfoDTO getClinicInfoByDoctorId(
//	            String doctorId) {
//
//	        return clinicAdminFeign.getClinicInfoByDoctorId(
//	                keyCloakTokenStore.getAccess_token(),
//	                doctorId);
//	    }
//
//	    private ClinicInfoDTO getClinicInfoByDoctorIdFallback(
//	            String doctorId,
//	            Exception ex) {
//
//	        throw new RuntimeException(
//	                "physiotherapydoctorService is temporarily unavailable",
//	                ex);
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "addVitalsFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "addVitalsFallback")
//	    public ResponseEntity<Response> addVitals(
//	            String bookingId,
//	            VitalsDTO dto) {
//
//	        return clinicAdminFeign.addVitals(
//	                keyCloakTokenStore.getAccess_token(),
//	                bookingId,
//	                dto);
//	    }
//
//	    private ResponseEntity<Response> addVitalsFallback(
//	            String bookingId,
//	            VitalsDTO dto,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getVitalsFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getVitalsFallback")
//	    public ResponseEntity<Response> getVitals(
//	            String bookingId,
//	            String patientId) {
//
//	        return clinicAdminFeign.getVitals(
//	                keyCloakTokenStore.getAccess_token(),
//	                bookingId,
//	                patientId);
//	    }
//
//	    private ResponseEntity<Response> getVitalsFallback(
//	            String bookingId,
//	            String patientId,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "delVitalsFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "delVitalsFallback")
//	    public ResponseEntity<Response> delVitals(
//	            String bookingId,
//	            String patientId) {
//
//	        return clinicAdminFeign.delVitals(
//	                keyCloakTokenStore.getAccess_token(),
//	                bookingId,
//	                patientId);
//	    }
//
//	    private ResponseEntity<Response> delVitalsFallback(
//	            String bookingId,
//	            String patientId,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "updateVitalsFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "updateVitalsFallback")
//	    public ResponseEntity<Response> updateVitals(
//	            String bookingId,
//	            String patientId,
//	            VitalsDTO dto) {
//
//	        return clinicAdminFeign.updateVitals(
//	                keyCloakTokenStore.getAccess_token(),
//	                bookingId,
//	                patientId,
//	                dto);
//	    }
//
//	    private ResponseEntity<Response> updateVitalsFallback(
//	            String bookingId,
//	            String patientId,
//	            VitalsDTO dto,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getDiseasesByHospitalIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getDiseasesByHospitalIdFallback")
//	    public ResponseEntity<Response> getDiseasesByHospitalId(
//	            String hospitalId) {
//
//	        return clinicAdminFeign.getDiseasesByHospitalId(
//	                keyCloakTokenStore.getAccess_token(),
//	                hospitalId);
//	    }
//
//	    private ResponseEntity<Response> getDiseasesByHospitalIdFallback(
//	            String hospitalId,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getSignedUrlFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getSignedUrlFallback")
//	    public ResponseEntity<String> getSignedUrl(
//	            String fileKey) {
//
//	        return clinicAdminFeign.getSignedUrl(
//	                keyCloakTokenStore.getAccess_token(),
//	                fileKey);
//	    }
//
//	    private ResponseEntity<String> getSignedUrlFallback(
//	            String fileKey,
//	            Exception ex) {
//
//	        throw new RuntimeException(
//	                "physiotherapydoctorService is temporarily unavailable",
//	                ex);
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getAllTreatmentsFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getAllTreatmentsFallback")
//	    public ResponseEntity<Response> getAllTreatments() {
//
//	        return clinicAdminFeign.getAllTreatments(
//	                keyCloakTokenStore.getAccess_token());
//	    }
//
//	    private ResponseEntity<Response> getAllTreatmentsFallback(
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getTreatmentByIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getTreatmentByIdFallback")
//	    public ResponseEntity<Response> getTreatmentById(
//	            String id,
//	            String hospitalId) {
//
//	        return clinicAdminFeign.getTreatmentById(
//	                keyCloakTokenStore.getAccess_token(),
//	                id,
//	                hospitalId);
//	    }
//
//	    private ResponseEntity<Response> getTreatmentByIdFallback(
//	            String id,
//	            String hospitalId,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "deleteTreatmentByIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "deleteTreatmentByIdFallback")
//	    public ResponseEntity<Response> deleteTreatmentById(
//	            String id,
//	            String hospitalId) {
//
//	        return clinicAdminFeign.deleteTreatmentById(
//	                keyCloakTokenStore.getAccess_token(),
//	                id,
//	                hospitalId);
//	    }
//
//	    private ResponseEntity<Response> deleteTreatmentByIdFallback(
//	            String id,
//	            String hospitalId,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "updateTreatmentByIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "updateTreatmentByIdFallback")
//	    public ResponseEntity<Response> updateTreatmentById(
//	            String id,
//	            String hospitalId,
//	            TreatmentDTO dto) {
//
//	        return clinicAdminFeign.updateTreatmentById(
//	                keyCloakTokenStore.getAccess_token(),
//	                id,
//	                hospitalId,
//	                dto);
//	    }
//
//	    private ResponseEntity<Response> updateTreatmentByIdFallback(
//	            String id,
//	            String hospitalId,
//	            TreatmentDTO dto,
//	            Exception ex) {
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(buildServiceUnavailableResponse(ex));
//	    }
//
//	    @CircuitBreaker(name = "physiotherapydoctorService", fallbackMethod = "getAllRecoverySupportsByClinicIdFallback")
//	    @Retry(name = "physiotherapydoctorService", fallbackMethod = "getAllRecoverySupportsByClinicIdFallback")
//	    public Response getAllRecoverySupportsByClinicId(
//	            String clinicId) {
//
//	        return clinicAdminFeign.getAllRecoverySupportsByClinicId(
//	                keyCloakTokenStore.getAccess_token(),
//	                clinicId);
//	    }
//
//	    private Response getAllRecoverySupportsByClinicIdFallback(
//	            String clinicId,
//	            Exception ex) {
//
//	        return buildServiceUnavailableResponse(ex);
//	    }
//	    
//	    
//	    @CircuitBreaker(
//	            name = "physiotherapydoctorService",
//	            fallbackMethod = "notificationtodoctorFallback")
//	    @Retry(
//	            name = "physiotherapydoctorService",
//	            fallbackMethod = "notificationtodoctorFallback")
//	    public ResponseEntity<ResBody<List<NotificationDTO>>> notificationtodoctor(
//	            String hospitalId,
//	            String doctorId) {
//
//	        return notificationFeign.notificationtodoctor(
//	                hospitalId,
//	                doctorId);
//	    }
//
//	    private ResponseEntity<ResBody<List<NotificationDTO>>> notificationtodoctorFallback(
//	            String hospitalId,
//	            String doctorId,
//	            Exception ex) {
//
//	        ResBody<List<NotificationDTO>> response = new ResBody<>();
//	        response.setStatus(503);
//	        response.setMessage("Notification Service is temporarily unavailable. Please try again later.");
//	        response.setData(null);
//
//	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(response);
//	    }
//	    
//	    @CircuitBreaker(
//	            name = "clinicAdminService",
//	            fallbackMethod = "getAssignedTherapistDetailsFallback")
//	    @Retry(
//	            name = "clinicAdminService",
//	            fallbackMethod = "getAssignedTherapistDetailsFallback")
//	    public ResponseEntity<Response> getAssignedTherapistDetails(          
//	            String therapistRecordId) {
//
//	        return clinicAdminFeign.getAssignedTherapistDetails(
//	        		token(),
//	                therapistRecordId);
//	    }
//
//	    /**
//	     * Fallback Method
//	     */
//	    public ResponseEntity<Response> getAssignedTherapistDetailsFallback(
//	            String token,
//	            String therapistRecordId,
//	            Exception ex) {
//
//	        Response response = new Response();
//	        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
//	        response.setMessage("Clinic Admin Service is currently unavailable. Please try again later.");
//	        response.setData(null);
//
//	        return ResponseEntity
//	                .status(HttpStatus.SERVICE_UNAVAILABLE)
//	                .body(response);
//	    }
//}
