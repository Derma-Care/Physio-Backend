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
import com.clinicadmin.dto.TimeLocationDTO;
import com.clinicadmin.entity.Session;
import com.clinicadmin.entity.TherapistAttendance;
import com.clinicadmin.entity.TherapistRecord;
import com.clinicadmin.entity.TimeLocation;
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
    public Response addManualSession(String therapistId, Map<String, String> body) {

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

            List<Session> sessionList =
                    attendance.getSessions() != null
                    ? new ArrayList<>(attendance.getSessions())
                    : new ArrayList<>();

            // ✅ create manual session
            Session s = new Session();
            s.setSessionId("MANUAL_" + System.currentTimeMillis());
            s.setActivity(body.get("activity"));
            s.setDuration(body.get("duration"));
            s.setDescription(body.get("description"));
            s.setLocation(body.getOrDefault("location", null));

            sessionList.add(s);

            // ✅ IMPORTANT LINE (YOU MAY BE MISSING THIS)
            attendance.setSessions(sessionList);

            // ✅ SAVE
            attendanceRepo.save(attendance);

            response.setSuccess(true);
            response.setMessage("Manual session added successfully");
            response.setData(s);
            response.setStatus(200);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(500);
        }

        return response;
    }
    @Override
    public Response getDailyReport(String therapistId, String date) {

        Response response = new Response();

        try {

            TherapistAttendance attendance =
                    attendanceRepo.findByTherapistIdAndDate(therapistId, date);

            List<TherapistRecord> records =
                    recordRepo.findByTherapistIdAndCompletedDate(therapistId, date);

            List<SessionData> sessions = new ArrayList<>();

            // 🔹 AUTO sessions
            for (TherapistRecord r : records) {
                SessionData s = new SessionData();
                s.setSessionId(r.getSessionId());
                s.setActivity(r.getServiceType());
                s.setDuration(r.getDuration());
                s.setLocation(r.getLocation());
                s.setLocation(r.getLocation());
                s.setDescription(r.getDescription());
                sessions.add(s);
            }

            // 🔹 MANUAL sessions
            if (attendance != null && attendance.getSessions() != null) {

                for (Session s : attendance.getSessions()) {

                    if (s.getSessionId() != null && s.getSessionId().startsWith("MANUAL")) {

                        SessionData sd = new SessionData();
                        sd.setSessionId(s.getSessionId());
                        sd.setActivity(s.getActivity());
                        sd.setDuration(s.getDuration());
                        sd.setLocation(s.getLocation());
                        sd.setLocation(s.getLocation());
                        sd.setDescription(s.getDescription());

                        sessions.add(sd);
                    }
                }
            }

            // 🔹 login/logout/logTime
            String loginTime = null;
            String logoutTime = null;
            String logTime = null;
            String loginLocation = null;
            String logoutLocation = null;

            if (attendance != null) {

                if (attendance.getLogin() != null) {
                    loginTime = attendance.getLogin().getTime();
                    loginLocation = attendance.getLogin().getLocation();
                }

                if (attendance.getLogout() != null) {
                    logoutTime = attendance.getLogout().getTime();
                    logoutLocation = attendance.getLogout().getLocation();
                }

                if (loginTime != null && logoutTime != null) {
                    logTime = calculateLogTime(loginTime, logoutTime);
                }
            }

            // =========================================================
            // ✅ NEW OBJECT STRUCTURE
            // =========================================================

            TimeLocationDTO loginObj = new TimeLocationDTO();
            loginObj.setTime(loginTime);
            loginObj.setLocation(loginLocation);

            TimeLocationDTO logoutObj = new TimeLocationDTO();
            logoutObj.setTime(logoutTime);
            logoutObj.setLocation(logoutLocation);

            // 🔹 response
            DailyReportResponse data = new DailyReportResponse();
            data.setDate(date);
            data.setLogin(loginObj);       // ✅ changed
            data.setLogout(logoutObj);     // ✅ changed
            data.setLogTime(logTime);
            data.setSessions(sessions);
            data.setStatus(attendance != null ? attendance.getStatus() : null);

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

            // =========================================================
            // 🔹 LOGIN / LOGOUT (OBJECT STRUCTURE)
            // =========================================================

            // 🔹 login update
            if (body.containsKey("loginTime")) {

                TimeLocation login = attendance.getLogin() != null
                        ? attendance.getLogin()
                        : new TimeLocation();

                login.setTime(body.get("loginTime"));

                if (body.containsKey("loginLocation")) {
                    login.setLocation(body.get("loginLocation"));
                }

                attendance.setLogin(login);
                attendance.setStatus("LOGIN");
            }

         // 🔹 login update
            if (body.containsKey("loginTime")) {

                TimeLocation login = attendance.getLogin() != null
                        ? attendance.getLogin()
                        : new TimeLocation();

                login.setTime(body.get("loginTime"));

                if (body.containsKey("loginLocation")) {
                    login.setLocation(body.get("loginLocation"));
                }

                attendance.setLogin(login);

                // ✅ UPDATED STATUS
                attendance.setStatus("LOGGED_IN");
            }

            // 🔹 logout update
            if (body.containsKey("logoutTime")) {

                TimeLocation logout = attendance.getLogout() != null
                        ? attendance.getLogout()
                        : new TimeLocation();

                logout.setTime(body.get("logoutTime"));

                if (body.containsKey("logoutLocation")) {
                    logout.setLocation(body.get("logoutLocation"));
                }

                attendance.setLogout(logout);

                // ✅ UPDATED STATUS
                attendance.setStatus("LOGGED_OUT");
            }

            // =========================================================
            // 🔹 calculate logTime
            // =========================================================
            if (attendance.getLogin() != null && attendance.getLogout() != null &&
                attendance.getLogin().getTime() != null &&
                attendance.getLogout().getTime() != null) {

            	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
                LocalTime in = LocalTime.parse(attendance.getLogin().getTime(), formatter);
                LocalTime out = LocalTime.parse(attendance.getLogout().getTime(), formatter);

                if (!out.isAfter(in)) {
                    throw new RuntimeException("Logout time must be after login time");
                }

                Duration d = Duration.between(in, out);

                attendance.setLogTime(d.toHours() + "h " + (d.toMinutes() % 60) + "m");
            }

            // =========================================================
            // 🔹 update therapist records (AUTO ONLY)
            // =========================================================
            List<TherapistRecord> records =
                    recordRepo.findByTherapistIdAndCompletedDate(therapistId, date);

            if (records == null) {
                records = new ArrayList<>();
            }

            for (TherapistRecord record : records) {

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
            }

            recordRepo.saveAll(records);

            // =========================================================
            // ✅ SYNC AUTO SESSIONS + KEEP MANUAL
            // =========================================================

            List<Session> existingSessions =
                    attendance.getSessions() != null
                    ? new ArrayList<>(attendance.getSessions())
                    : new ArrayList<>();

            List<Session> manualSessions = new ArrayList<>();

            for (Session s : existingSessions) {
                if (s.getSessionId() != null && s.getSessionId().startsWith("MANUAL")) {
                    manualSessions.add(s);
                }
            }

            List<Session> finalSessions = new ArrayList<>(manualSessions);

            for (TherapistRecord r : records) {

                Session s = new Session();
                s.setSessionId(r.getSessionId());
                s.setActivity(r.getServiceType());
                s.setDuration(r.getDuration());
                s.setLocation(r.getLocation());
                s.setDescription(r.getDescription());


                finalSessions.add(s);
            }

            attendance.setSessions(finalSessions);

            // =========================================================
            // 🔹 save attendance
            // =========================================================
            attendanceRepo.save(attendance);

            // =========================================================
            // 🔹 response
            // =========================================================
            Map<String, Object> data = new HashMap<>();
            data.put("therapistId", therapistId);
            data.put("date", date);
            data.put("updatedFields", new HashMap<>(body));

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
                res.setInTime(
                        a.getLogin() != null ? a.getLogin().getTime() : null
                );

                res.setOutTime(
                        a.getLogout() != null ? a.getLogout().getTime() : null
                );
                res.setLogTime(a.getLogTime());

                int totalMinutes = 0;

                // ✅ ONLY use attendance.sessions
                if (a.getSessions() != null && !a.getSessions().isEmpty()) {

                    for (Session s : a.getSessions()) {

                        if (s.getDuration() != null) {
                            totalMinutes += convertToMinutes(s.getDuration());
                        }
                    }
                }

                // 🔹 Working Hours
                int workH = totalMinutes / 60;
                int workM = totalMinutes % 60;
                res.setWorkingHours(workH + "h " + workM + "m");

                // 🔹 Idle Time
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

        if (time == null || time.trim().isEmpty()) return 0;

        time = time.toLowerCase().trim();

        int totalMinutes = 0;

        // 🔹 extract hours
        java.util.regex.Matcher hourMatcher =
                java.util.regex.Pattern.compile("(\\d+)\\s*(h|hr|hrs|hour|hours)")
                        .matcher(time);

        while (hourMatcher.find()) {
            int hours = Integer.parseInt(hourMatcher.group(1));
            totalMinutes += hours * 60;
        }

        // 🔹 extract minutes
        java.util.regex.Matcher minuteMatcher =
                java.util.regex.Pattern.compile("(\\d+)\\s*(m|min|mins|minute|minutes)")
                        .matcher(time);

        while (minuteMatcher.find()) {
            int minutes = Integer.parseInt(minuteMatcher.group(1));
            totalMinutes += minutes;
        }

        // 🔹 fallback: if only number (like "30")
        if (totalMinutes == 0 && time.matches("\\d+")) {
            totalMinutes = Integer.parseInt(time);
        }

        return totalMinutes;
    }

	private String calculateLogTime(String loginTime, String logoutTime) {

	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm"); 

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
	
	@Override
	public Response getReportByClinicBranch(
	        String clinicId,
	        String branchId,
	        String therapistId,
	        String date) {

	    Response response = new Response();

	    try {

	    	TherapistAttendance attendance =
	    	        attendanceRepo.findByClinicIdAndBranchIdAndTherapistIdAndDate(
	    	                clinicId,
	    	                branchId,
	    	                therapistId,
	    	                date);

	        List<SessionData> sessions = new ArrayList<>();

	        // 🔹 Get ALL sessions
	        if (attendance != null && attendance.getSessions() != null) {

	            for (Session s : attendance.getSessions()) {

	                SessionData sd = new SessionData();
	                sd.setSessionId(s.getSessionId());
	                sd.setActivity(s.getActivity());
	                sd.setDuration(s.getDuration());
	                sd.setLocation(s.getLocation());
	                
	                sd.setDescription(
	                        s.getDescription() != null
	                                && !s.getDescription().trim().isEmpty()
	                        ? s.getDescription()
	                        : "N/A"
	                    );


	                sessions.add(sd);
	            }
	        }

	        // 🔹 Response
	        Map<String, Object> data = new HashMap<>();
	        data.put("clinicId", clinicId);
	        data.put("branchId", branchId);
	        data.put("therapistId", therapistId);
	        data.put("date", date);
	        data.put("sessions", sessions);

	        response.setSuccess(true);
	        response.setMessage("Filtered report fetched successfully");
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