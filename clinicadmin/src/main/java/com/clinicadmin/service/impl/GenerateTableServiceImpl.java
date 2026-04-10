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
                throw new RuntimeException("No therapySessions found");
            }

            LocalDate startDate = LocalDate.parse(request.getStartDate());

            // 🔥 FINAL RESULT LIST
            List<ProgramResponseDTO> result = new ArrayList<>();

            for (Map<String, Object> session : therapySessions) {

                ProgramResponseDTO programDTO = new ProgramResponseDTO();
                programDTO.setTherapyData(new ArrayList<>());

                List<Map<String, Object>> therapyDataList =
                        (List<Map<String, Object>>) session.get("therapyData");

                if (therapyDataList == null) continue;

                for (Map<String, Object> therapyData : therapyDataList) {

                    String therapyId = (String) therapyData.get("therapyId");
                    String therapyName = (String) therapyData.get("therapyName");

                    List<Map<String, Object>> exercises =
                            (List<Map<String, Object>>) therapyData.get("exercises");

                    if (exercises == null) continue;

                    List<ExerciseResponseDTO> exerciseList = new ArrayList<>();

                    for (Map<String, Object> ex : exercises) {

                        ExerciseResponseDTO exDTO = new ExerciseResponseDTO();

                        String exerciseId = ex.get("therapyExercisesId") != null
                                ? ex.get("therapyExercisesId").toString()
                                : "EX";

                        exDTO.setExerciseId(exerciseId);
                        exDTO.setExerciseName((String) ex.get("name"));

                        // ✅ frequency normalize
                        String frequency = ex.get("frequency") != null
                                ? ex.get("frequency").toString()
                                : "1day";

                        frequency = frequency.toLowerCase().replace(" ", "");

                        if (frequency.contains("times/day")) {
                            frequency = frequency.replace("times/day", "day");
                        }

                        exDTO.setFrequency(frequency);

                        // ✅ sets & reps
                        exDTO.setSets(ex.get("sets") != null
                                ? ((Number) ex.get("sets")).intValue() : 0);

                        exDTO.setRepetitions(ex.get("repetitions") != null
                                ? ((Number) ex.get("repetitions")).intValue() : 0);

                        // 🔥 session = total sessions
                        int totalSessions = (ex.get("session") != null &&
                                !ex.get("session").toString().isEmpty())
                                ? Integer.parseInt(ex.get("session").toString())
                                : 1;

                       
                        exDTO.setNoOfSessions(totalSessions);

                        List<SessionDTO> sessions =
                                generateSessions(startDate, frequency, totalSessions);

                        exDTO.setSessions(sessions);

                        exerciseList.add(exDTO);
                    }

                    TherapyResponseDTO therapyDTO = new TherapyResponseDTO();
                    therapyDTO.setTherapyId(therapyId);
                    therapyDTO.setTherapyName(therapyName);
                    therapyDTO.setExercises(exerciseList);

                    programDTO.getTherapyData().add(therapyDTO);
                }

                result.add(programDTO);
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

    // ===============================
    // SESSION GENERATION
    // ===============================
    private List<SessionDTO> generateSessions(LocalDate startDate,
                                             String frequency,
                                             int total) {

        List<SessionDTO> list = new ArrayList<>();
        int index = 1;

        String number = frequency.replaceAll("[^0-9]", "");
        String unit = frequency.replaceAll("[0-9]", "");

        int interval = Integer.parseInt(number);

        LocalDate date = startDate;

        while (list.size() < total) {

            SessionDTO s = new SessionDTO();

            s.setDate(date.getMonthValue() + "/" + date.getDayOfMonth() + "/" + date.getYear());
            s.setStatus("Pending");
            s.setPaymentStatus("unpaid");

            s.setSessionId(generateSessionId(date, index++));

            list.add(s);

            if (unit.equalsIgnoreCase("day") || unit.equalsIgnoreCase("days")) {
                date = date.plusDays(interval);
            } else if (unit.equalsIgnoreCase("week") || unit.equalsIgnoreCase("weeks")) {
                date = date.plusWeeks(interval);
            } else if (unit.equalsIgnoreCase("month") || unit.equalsIgnoreCase("months")) {
                date = date.plusMonths(interval);
            }
        }

        return list;
    }

    // ===============================
    // SESSION ID GENERATOR
    // ===============================
    private String generateSessionId(LocalDate date, int index) {

        String month = date.getMonth().toString().substring(0, 3);
        String day = String.format("%02d", date.getDayOfMonth());

        return "S" + String.format("%02d", index) + "-" + day + month + "-" + generateShortCode();
    }

    private String generateShortCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            int idx = (int) (Math.random() * chars.length());
            code.append(chars.charAt(idx));
        }

        return code.toString();
    }
}