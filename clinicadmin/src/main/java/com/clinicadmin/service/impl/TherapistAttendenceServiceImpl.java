package com.clinicadmin.service.impl;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.clinicadmin.dto.DailyReportResponse;
import com.clinicadmin.dto.MonthlyTherapistResponse;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.SessionData;
import com.clinicadmin.entity.Session;
import com.clinicadmin.entity.TherapistAttendance;
import com.clinicadmin.entity.TherapistRecord;
import com.clinicadmin.repository.TherapistAttendanceRepository;
import com.clinicadmin.repository.TherapistRecordRepository;
import com.clinicadmin.service.TherapistAttendenceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TherapistAttendenceServiceImpl implements TherapistAttendenceService {

    private final TherapistAttendanceRepository attendanceRepo;
    private final TherapistRecordRepository recordRepo;

    @Override
    public Response getDailyReport(String therapistId, String date) {

        Response response = new Response();

        try {

            TherapistAttendance attendance =
                    attendanceRepo.findByTherapistIdAndDate(therapistId, date);

            List<TherapistRecord> records =
                    recordRepo.findByTherapistIdAndCompletedDate(therapistId, date);

            List<SessionData> sessions = new ArrayList<>();

            // 🔹 ORIGINAL AUTO LOGIC (UNCHANGED)
            for (TherapistRecord r : records) {
                SessionData s = new SessionData();
                s.setSessionId(r.getSessionId());
                s.setActivity(r.getServiceType());
                s.setDuration(r.getDuration());
                s.setLocation(r.getLocation());
                sessions.add(s);
            }

            // 🔹 NEW: ADD MANUAL SESSIONS
            if (attendance != null && attendance.getSessions() != null) {

                for (Session s : attendance.getSessions()) {

                    // Only manual sessions
                    if (s.getSessionId() != null && s.getSessionId().startsWith("MANUAL")) {

                        SessionData sd = new SessionData();
                        sd.setSessionId(null); // ❌ hide manual sessionId
                        sd.setActivity(s.getActivity());
                        sd.setDuration(s.getDuration());
                        sd.setLocation(s.getLocation());

                        sessions.add(sd);
                    }
                }
            }

            String loginTime = null;
            String logoutTime = null;
            String logTime = null;

            if (attendance != null) {
                loginTime = attendance.getLoginTime();
                logoutTime = attendance.getLogoutTime();

                if (loginTime != null && logoutTime != null) {
                    logTime = calculateLogTime(loginTime, logoutTime);
                }
            }

            DailyReportResponse data = new DailyReportResponse();
            data.setDate(date);
            data.setLoginTime(loginTime);
            data.setLogoutTime(logoutTime);
            data.setLogTime(logTime);
            data.setSessions(sessions);

            response.setSuccess(true);
            response.setMessage("Daily report fetched successfully");
            response.setData(data);
            response.setStatus(200);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }

        return response;
    }
    @Override
    public Response updateAttendance(String therapistId,
                                     Map<String, String> body) {

        Response response = new Response();

        try {

            String date = body.get("completedDate");

            TherapistAttendance attendance =
                    attendanceRepo.findByTherapistIdAndDate(therapistId, date);

            if (attendance == null) {
                attendance = new TherapistAttendance();
                attendance.setTherapistId(therapistId);
                attendance.setDate(date);
            }

            // 🔹 dynamic update
            if (body.containsKey("loginTime")) {
                attendance.setLoginTime(body.get("loginTime"));
            }

            if (body.containsKey("logoutTime")) {
                attendance.setLogoutTime(body.get("logoutTime"));
            }

            // 🔹 calculate logTime
            if (attendance.getLoginTime() != null && attendance.getLogoutTime() != null) {

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

                LocalTime in = LocalTime.parse(attendance.getLoginTime(), formatter);
                LocalTime out = LocalTime.parse(attendance.getLogoutTime(), formatter);

                if (!out.isAfter(in)) {
                    throw new RuntimeException("Logout time must be after login time");
                }

                Duration d = Duration.between(in, out);

                attendance.setLogTime(d.toHours() + "h " + (d.toMinutes() % 60) + "m");
            }

            // 🔹 update records (same-date)
            List<TherapistRecord> records =
                    recordRepo.findByTherapistIdAndCompletedDate(therapistId, date);

            if (records == null || records.isEmpty()) {
                records = recordRepo.findByTherapistIdAndCompletedDateStartingWith(
                        therapistId, date.substring(0, 7));
            }

            // 🔹 MANUAL CHECK
            boolean isManual =
                    body.containsKey("location") &&
                    body.get("location") != null &&
                    !body.get("location").trim().isEmpty();

            for (TherapistRecord record : records) {

                if (!isManual) {

                    if (body.containsKey("activity")) {
                        record.setServiceType(body.get("activity"));
                    }

                    if (body.containsKey("duration")) {
                        record.setDuration(body.get("duration"));
                    }

                    if (body.containsKey("latitude")) {
                        record.setLatitude(body.get("latitude"));
                    }

                    if (body.containsKey("longitude")) {
                        record.setLongitude(body.get("longitude"));
                    }

                    // ❌ REMOVED LOCATION UPDATE FOR AUTO MODE
                    // (Original logic preserved, only this part removed)
                }
            }

            recordRepo.saveAll(records);

            // =========================================================
            // ✅ KEEP OLD SESSIONS + ADD NEW ONES
            // =========================================================

            List<Session> sessionList =
                    attendance.getSessions() != null
                    ? new ArrayList<>(attendance.getSessions())
                    : new ArrayList<>();

            // 🔹 ADD AUTO ONLY FIRST TIME
            if (sessionList.isEmpty()) {

                for (TherapistRecord r : records) {

                    Session s = new Session();
                    s.setSessionId(r.getSessionId());
                    s.setActivity(r.getServiceType());
                    s.setDuration(r.getDuration());
                    s.setLocation(r.getLocation());

                    sessionList.add(s);
                }
            }

            int totalMinutes = 0;

            // 🔹 calculate existing
            for (Session s : sessionList) {
                if (s.getDuration() != null) {
                    totalMinutes += convertToMinutes(s.getDuration());
                }
            }

            // 🔹 ADD MANUAL (NO LIMIT)
            if (isManual) {

                Session s = new Session();
                s.setSessionId("MANUAL_" + System.currentTimeMillis());
                s.setActivity(body.get("activity"));
                s.setDuration(body.get("duration"));
                s.setLocation(body.get("location"));

                sessionList.add(s);

                totalMinutes += convertToMinutes(s.getDuration());
            }

            // 🔹 save sessions
            attendance.setSessions(sessionList);

            // 🔹 working hours
            int workH = totalMinutes / 60;
            int workM = totalMinutes % 60;
            attendance.setWorkingHours(workH + "h " + workM + "m");

            // 🔹 idle time
            if (attendance.getLogTime() != null) {

                int logMinutes = convertToMinutes(attendance.getLogTime());
                int idle = logMinutes - totalMinutes;

                attendance.setIdleTime((idle / 60) + "h " + (idle % 60) + "m");
            }

            // 🔹 FINAL SAVE
            attendanceRepo.save(attendance);

            // 🔹 response
            Map<String, Object> data = new HashMap<>();
            data.put("therapistId", therapistId);
            data.put("date", date);
            data.put("updatedFields", new HashMap<>(body));
            
            Session manualSession = null; // ✅ declare outside

            if (isManual) {

                manualSession = new Session(); // assign here
                manualSession.setSessionId("MANUAL_" + System.currentTimeMillis());
                manualSession.setActivity(body.get("activity"));
                manualSession.setDuration(body.get("duration"));
                manualSession.setLocation(body.get("location"));

                sessionList.add(manualSession);

                totalMinutes += convertToMinutes(manualSession.getDuration());
            }

            response.setSuccess(true);
            response.setMessage("Attendance updated successfully");
            response.setData(data);
            response.setStatus(200);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }

        return response;
    }
    @Override
    public Response getMonthlyReport(String therapistId, String month) {

        Response response = new Response();

        try {

            List<TherapistAttendance> records =
                    attendanceRepo.findByTherapistIdAndDateStartingWith(therapistId, month);

            List<MonthlyTherapistResponse> data = records.stream().map(a -> {

                MonthlyTherapistResponse res = new MonthlyTherapistResponse();

                res.setDate(a.getDate());
                res.setInTime(a.getLoginTime());
                res.setOutTime(a.getLogoutTime());
                res.setLogTime(a.getLogTime());

                // 🔹 1. Get sessions for that date (ORIGINAL LOGIC)
                List<TherapistRecord> sessions =
                        recordRepo.findByTherapistIdAndCompletedDate(therapistId, a.getDate());

                int totalMinutes = 0;

                // 🔹 ORIGINAL: AUTO sessions
                for (TherapistRecord r : sessions) {
                    if (r.getDuration() != null) {
                        totalMinutes += convertToMinutes(r.getDuration());
                    }
                }

                // 🔹 NEW: ADD MANUAL SESSIONS (WITHOUT CHANGING ORIGINAL)
                if (a.getSessions() != null && !a.getSessions().isEmpty()) {

                    for (Session s : a.getSessions()) {

                        // Only count manual sessions
                        if (s.getSessionId() != null && s.getSessionId().startsWith("MANUAL")) {

                            if (s.getDuration() != null) {
                                totalMinutes += convertToMinutes(s.getDuration());
                            }
                        }
                    }
                }

                // 🔹 2. Working Hours
                int workH = totalMinutes / 60;
                int workM = totalMinutes % 60;
                String working = workH + "h " + workM + "m";
                res.setWorkingHours(working);

                // 🔹 3. Idle Time = logTime - working
                if (a.getLogTime() != null) {

                    int logMinutes = convertToMinutes(a.getLogTime());
                    int idle = logMinutes - totalMinutes;

                    int idleH = idle / 60;
                    int idleM = idle % 60;

                    res.setIdleTime(idleH + "h " + idleM + "m");
                }

                return res;

            }).toList();

            response.setSuccess(true);
            response.setMessage("Monthly report fetched successfully");
            response.setData(data);
            response.setStatus(200);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }

        return response;
    }

    private int convertToMinutes(String time) {

        time = time.toLowerCase();

        int hours = 0;
        int minutes = 0;

        if (time.contains("h")) {
            String[] parts = time.split("h");
            hours = Integer.parseInt(parts[0].trim());

            if (parts.length > 1 && parts[1].contains("m")) {
                minutes = Integer.parseInt(parts[1].replace("m", "").trim());
            }
        } else if (time.contains("minute")) {
            minutes = Integer.parseInt(time.replaceAll("[^0-9]", ""));
        }

        return (hours * 60) + minutes;
    }

	private String calculateLogTime(String loginTime, String logoutTime) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        LocalTime in = LocalTime.parse(loginTime, formatter);
        LocalTime out = LocalTime.parse(logoutTime, formatter);

        Duration d = Duration.between(in, out);

        return d.toHours() + "h " + (d.toMinutes() % 60) + "m";
    }
	public String getCityFromLatLong(String lat, String lon) {

	    try {
	        String url = "https://nominatim.openstreetmap.org/reverse?lat="
	                + lat + "&lon=" + lon + "&format=json";

	        RestTemplate restTemplate = new RestTemplate();

	        HttpHeaders headers = new HttpHeaders();
	        headers.set("User-Agent", "clinic-admin-app"); // IMPORTANT

	        HttpEntity<String> entity = new HttpEntity<>(headers);

	        ResponseEntity<Map> response = restTemplate.exchange(
	                url,
	                HttpMethod.GET,
	                entity,
	                Map.class
	        );

	        Map<String, Object> body = response.getBody();

	        if (body == null) return "Unknown";

	        Map<String, Object> address = (Map<String, Object>) body.get("address");

	        if (address == null) return "Unknown";

	        // Try multiple keys (important)
	        if (address.get("city") != null) return address.get("city").toString();
	        if (address.get("town") != null) return address.get("town").toString();
	        if (address.get("village") != null) return address.get("village").toString();
	        if (address.get("state_district") != null) return address.get("state_district").toString();

	        return "Unknown";

	    } catch (Exception e) {
	        return "Unknown";
	    }
	}
	@Override
	public Response deleteSession(String therapistId, String date, String sessionId) {

	    Response response = new Response();

	    try {

	        // 🔹 1. Check if MANUAL
	        if (sessionId != null && sessionId.startsWith("MANUAL")) {

	            TherapistAttendance attendance =
	                    attendanceRepo.findByTherapistIdAndDate(therapistId, date);

	            if (attendance != null && attendance.getSessions() != null) {

	                List<Session> updatedList = new ArrayList<>();

	                for (Session s : attendance.getSessions()) {

	                    // keep all except the one to delete
	                    if (!sessionId.equals(s.getSessionId())) {
	                        updatedList.add(s);
	                    }
	                }

	                attendance.setSessions(updatedList);

	                // 🔹 recalculate working + idle
	                int totalMinutes = 0;

	                for (Session s : updatedList) {
	                    if (s.getDuration() != null) {
	                        totalMinutes += convertToMinutes(s.getDuration());
	                    }
	                }

	                int workH = totalMinutes / 60;
	                int workM = totalMinutes % 60;
	                attendance.setWorkingHours(workH + "h " + workM + "m");

	                if (attendance.getLogTime() != null) {

	                    int logMinutes = convertToMinutes(attendance.getLogTime());
	                    int idle = logMinutes - totalMinutes;

	                    attendance.setIdleTime((idle / 60) + "h " + (idle % 60) + "m");
	                }

	                attendanceRepo.save(attendance);
	            }

	        } 
	        // 🔹 2. AUTO DELETE
	        else {

	            // delete from TherapistRecord
	            recordRepo.deleteBySessionId(sessionId);
	        }

	        // 🔹 response
	        Map<String, Object> data = new HashMap<>();
	        data.put("therapistId", therapistId);
	        data.put("date", date);
	        data.put("deletedSessionId", sessionId);

	        response.setSuccess(true);
	        response.setMessage("Session deleted successfully");
	        response.setData(data);
	        response.setStatus(200);

	    } catch (Exception e) {
	        response.setSuccess(false);
	        response.setMessage(e.getMessage());
	        response.setStatus(500);
	    }

	    return response;
	}
}