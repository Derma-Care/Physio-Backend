package com.clinicadmin.service.impl;
	
	import java.time.LocalDate;
	import java.util.ArrayList;
	import java.util.List;
	import java.util.Map;
	
	import org.springframework.stereotype.Service;
	
	import com.clinicadmin.dto.ExerciseResponseDTO;
	import com.clinicadmin.dto.PhysiotherapyRecordDTO;
	import com.clinicadmin.dto.ProgramResponseDTO;
	import com.clinicadmin.dto.Response;
	import com.clinicadmin.dto.SessionDTO;
	import com.clinicadmin.dto.TherapyResponseDTO;
	import com.clinicadmin.feignclient.PhysiotherapyFeignClient;
	import com.clinicadmin.service.GenerateTableService;
	
	import lombok.RequiredArgsConstructor;
	
	@Service
	@RequiredArgsConstructor
	public class GenerateTableServiceImpl implements GenerateTableService {
	
	    private final PhysiotherapyFeignClient feignClient;
	
	    
	    @Override
	    public Response generateTable(PhysiotherapyRecordDTO request) {

	        Response response = new Response();

	        try {

	            Response doctorResponse = feignClient.getRecord(
	                    request.getClinicId(),
	                    request.getBranchId(),
	                    request.getPatientId(),
	                    request.getBookingId(),
	                    request.getTherapistRecordId()
	            );

	            if (!doctorResponse.isSuccess()) {
	                return doctorResponse;
	            }

	            Map<String, Object> record =
	                    (Map<String, Object>) doctorResponse.getData();

	            List<Map<String, Object>> therapySessions =
	                    (List<Map<String, Object>>) record.get("therapySessions");

	            if (therapySessions == null || therapySessions.isEmpty()) {
	                therapySessions =
	                        (List<Map<String, Object>>) record.get("therapyWithSessions");
	            }

	            if (therapySessions == null || therapySessions.isEmpty()) {
	                throw new RuntimeException("No therapySessions found");
	            }

	            LocalDate startDate = LocalDate.parse(request.getStartDate());

	            List<ProgramResponseDTO> result = new ArrayList<>();

	            for (Map<String, Object> session : therapySessions) {

	                // ==================================================
	                // PROGRAMS
	                // ==================================================
	                List<Map<String, Object>> programs =
	                        (List<Map<String, Object>>) session.get("programs");

	                if (programs != null && !programs.isEmpty()) {

	                    for (Map<String, Object> program : programs) {

	                        List<Map<String, Object>> therapyDataList =
	                                (List<Map<String, Object>>) program.get("therapyData");

	                        if (therapyDataList != null && !therapyDataList.isEmpty()) {
	                            result.add(buildProgramDTO(therapyDataList, startDate));
	                        }
	                    }
	                }

	                // ==================================================
	                // PACKAGE
	                // ==================================================
	                List<Map<String, Object>> packageData =
	                        (List<Map<String, Object>>) session.get("packageData");

	                if (packageData != null && !packageData.isEmpty()) {
	                    result.add(buildProgramDTO(packageData, startDate));
	                }

	                // ==================================================
	                // DIRECT THERAPY DATA
	                // ==================================================
	                List<Map<String, Object>> therapyData =
	                        (List<Map<String, Object>>) session.get("therapyData");

	                if (therapyData != null && !therapyData.isEmpty()) {
	                    result.add(buildProgramDTO(therapyData, startDate));
	                }
	            }

	            response.setSuccess(true);
	            response.setData(result);
	            response.setStatus(200);

	        } catch (Exception e) {

	            response.setSuccess(false);
	            response.setMessage(e.getMessage());
	            response.setStatus(500);
	        }

	        return response;
	    }

	    private ProgramResponseDTO buildProgramDTO(
	            List<Map<String, Object>> therapyDataList,
	            LocalDate startDate) {

	        ProgramResponseDTO programDTO = new ProgramResponseDTO();
	        programDTO.setTherapyData(new ArrayList<>());

	        for (Map<String, Object> therapyData : therapyDataList) {

	            TherapyResponseDTO therapyDTO = new TherapyResponseDTO();

	            therapyDTO.setTherapyId(
	                    therapyData.get("therapyId") != null
	                            ? therapyData.get("therapyId").toString()
	                            : ""
	            );

	            therapyDTO.setTherapyName(
	                    therapyData.get("therapyName") != null
	                            ? therapyData.get("therapyName").toString()
	                            : ""
	            );

	            List<Map<String, Object>> exercises =
	                    (List<Map<String, Object>>) therapyData.get("exercises");

	            List<ExerciseResponseDTO> exerciseList = new ArrayList<>();

	            if (exercises != null && !exercises.isEmpty()) {

	                for (Map<String, Object> ex : exercises) {

	                    ExerciseResponseDTO exDTO = new ExerciseResponseDTO();

	                    exDTO.setExerciseId(
	                            ex.get("exerciseId") != null
	                                    ? ex.get("exerciseId").toString()
	                                    : "EX"
	                    );

	                    exDTO.setExerciseName(
	                            ex.get("exerciseName") != null
	                                    ? ex.get("exerciseName").toString()
	                                    : ""
	                    );

	                    exDTO.setSets(
	                            ex.get("sets") != null
	                                    ? ((Number) ex.get("sets")).intValue()
	                                    : 0
	                    );

	                    exDTO.setRepetitions(
	                            ex.get("repetitions") != null
	                                    ? ((Number) ex.get("repetitions")).intValue()
	                                    : 0
	                    );

	                    String frequency =
	                            ex.get("frequency") != null
	                                    ? ex.get("frequency").toString()
	                                    : "1";

	                    frequency = frequency.trim();

	                    if (frequency.matches("\\d+")) {
	                        frequency = frequency + "day";
	                    }

	                    frequency = frequency.toLowerCase().replace(" ", "");

	                    exDTO.setFrequency(frequency);

	                    int totalSessions =
	                            ex.get("noOfSessions") != null
	                                    ? Integer.parseInt(
	                                    ex.get("noOfSessions").toString())
	                                    : 1;

	                    exDTO.setNoOfSessions(totalSessions);

	                    List<SessionDTO> sessions = new ArrayList<>();

	                    List<Map<String, Object>> existingSessions =
	                            (List<Map<String, Object>>) ex.get("sessions");

	                    if (existingSessions != null && !existingSessions.isEmpty()) {

	                        for (Map<String, Object> s : existingSessions) {

	                            SessionDTO sessionDTO = new SessionDTO();

	                            sessionDTO.setSessionId(
	                                    s.get("sessionId") != null
	                                            ? s.get("sessionId").toString()
	                                            : ""
	                            );

	                            sessionDTO.setDate(
	                                    s.get("date") != null
	                                            ? s.get("date").toString()
	                                            : ""
	                            );

	                            sessionDTO.setStatus(
	                                    s.get("status") != null
	                                            ? s.get("status").toString()
	                                            : "Pending"
	                            );

	                            sessionDTO.setPaymentStatus(
	                                    s.get("paymentStatus") != null
	                                            ? s.get("paymentStatus").toString()
	                                            : "unpaid"
	                            );

	                            sessions.add(sessionDTO);
	                        }

	                    } else {

	                        sessions = generateSessions(
	                                startDate,
	                                frequency,
	                                totalSessions
	                        );
	                    }

	                    exDTO.setSessions(sessions);
	                    exerciseList.add(exDTO);
	                }
	            }

	            therapyDTO.setExercises(exerciseList);
	            programDTO.getTherapyData().add(therapyDTO);
	        }

	        return programDTO;
	    }
	
	    // ==================================================
	    // SESSION GENERATION
	    // ==================================================
	    private List<SessionDTO> generateSessions(
	            LocalDate startDate,
	            String frequency,
	            int total
	    ) {
	
	        List<SessionDTO> list = new ArrayList<>();
	
	        String number = frequency.replaceAll("[^0-9]", "");
	        String unit = frequency.replaceAll("[0-9]", "");
	
	        int interval = number.isEmpty()
	                ? 1
	                : Integer.parseInt(number);
	
	        LocalDate date = startDate;
	
	        for (int i = 1; i <= total; i++) {
	
	            SessionDTO s = new SessionDTO();
	
	            s.setDate(
	                    date.getMonthValue() + "/"
	                            + date.getDayOfMonth() + "/"
	                            + date.getYear()
	            );
	
	            s.setStatus("Pending");
	            s.setPaymentStatus("unpaid");
	            s.setSessionId(generateSessionId(date, i));
	
	            list.add(s);
	
	            if (unit.equalsIgnoreCase("day")
	                    || unit.equalsIgnoreCase("days")) {
	
	                date = date.plusDays(interval);
	
	            } else if (unit.equalsIgnoreCase("week")
	                    || unit.equalsIgnoreCase("weeks")) {
	
	                date = date.plusWeeks(interval);
	
	            } else if (unit.equalsIgnoreCase("month")
	                    || unit.equalsIgnoreCase("months")) {
	
	                date = date.plusMonths(interval);
	
	            } else {
	                date = date.plusDays(interval);
	            }
	        }
	
	        return list;
	    }
	
	    // ==================================================
	    // SESSION ID GENERATOR
	    // ==================================================
	    private String generateSessionId(LocalDate date, int index) {
	
	        String month =
	                date.getMonth().toString().substring(0, 3);
	
	        String day =
	                String.format("%02d", date.getDayOfMonth());
	
	        return "S"
	                + String.format("%02d", index)
	                + "-"
	                + day
	                + month
	                + "-"
	                + generateShortCode();
	    }
	
	    private String generateShortCode() {
	
	        String chars =
	                "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	
	        StringBuilder code = new StringBuilder();
	
	        for (int i = 0; i < 4; i++) {
	
	            int idx =
	                    (int) (Math.random() * chars.length());
	
	            code.append(chars.charAt(idx));
	        }
	
	        return code.toString();
	    }
	}