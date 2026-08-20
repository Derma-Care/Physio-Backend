package com.chiselon.clinicadmin.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.chiselon.clinicadmin.dto.ActivityDTO;
import com.chiselon.clinicadmin.dto.AttendanceDTO;
import com.chiselon.clinicadmin.dto.DailyAllUsersResponseDTO;
import com.chiselon.clinicadmin.dto.DailyAttendanceResponseDTO;
import com.chiselon.clinicadmin.dto.MonthlyAttendanceResponseDTO;
import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.dto.TimeLocationDTO;
import com.chiselon.clinicadmin.entity.Activity;
import com.chiselon.clinicadmin.entity.Attendance;
import com.chiselon.clinicadmin.entity.LoginEntity;
import com.chiselon.clinicadmin.entity.TherapistAttendance;
import com.chiselon.clinicadmin.entity.TimeLocation;
import com.chiselon.clinicadmin.repository.AttendanceRepository;
import com.chiselon.clinicadmin.repository.LoginRepository;
import com.chiselon.clinicadmin.repository.TherapistAttendanceRepository;
import com.chiselon.clinicadmin.service.AttendanceService;
import com.chiselon.clinicadmin.utils.FeignImpl;
import com.chiselon.clinicadmin.utils.KeyCloakTokenStore;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository repo;
    
    private final FeignImpl adminServiceClient;
    
    private final TherapistAttendanceRepository therapistAttendanceRepo;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
    
    @Autowired
    private LoginRepository credentialsRepository;

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "saveFallback")
	public Response save(AttendanceDTO dto) {

        log.info("Attendance save started userId={} date={}", dto.getUserId(), dto.getDate());

        Response response = new Response();

        try {

            if (dto.getUserId() == null || dto.getDate() == null) {
                throw new RuntimeException("userId and date are required");
            }

            // ✅ DOCTOR — only clinic check, skip branch & distance
            if ("doctor".equalsIgnoreCase(dto.getRole())) {

                validateLoginDistance(
                        dto.getClinicId(),
                        dto.getBranchId(),
                        dto.getRole(),
                        null,
                        null
                );

            } else {

                // ✅ OTHERS — full validation (clinic + branch + distance)
                if (dto.getLogin() != null
                        && dto.getLogin().getLatitude() != null
                        && !dto.getLogin().getLatitude().isBlank()
                        && dto.getLogin().getLongitude() != null
                        && !dto.getLogin().getLongitude().isBlank()) {

                    validateLoginDistance(
                            dto.getClinicId(),
                            dto.getBranchId(),
                            dto.getRole(),
                            dto.getLogin().getLatitude(),
                            dto.getLogin().getLongitude()
                    );
                }
            }

            Optional<Attendance> existingOpt =
                    repo.findByUserIdAndDate(dto.getUserId(), dto.getDate());

            Attendance entity;

            if (existingOpt.isPresent()) {

                entity = existingOpt.get();

            } else {

                entity = new Attendance();
                mapDtoToEntity(dto, entity);
                entity.setActivities(new ArrayList<>());

                // 🔥 FIXED STATUS LOGIC
                if (dto.getLogin() != null) {
                    entity.setStatus("LOGGED_IN");
                } else if (dto.getLogout() != null) {
                    entity.setStatus("LOGGED_OUT");
                } else {
                    entity.setStatus(null);
                }
            }

            // 🔥 ACTIVITIES
            if (dto.getActivities() != null && !dto.getActivities().isEmpty()) {

                if (entity.getActivities() == null) {
                    entity.setActivities(new ArrayList<>());
                }

                for (ActivityDTO a : dto.getActivities()) {

                    Activity act = new Activity();

                    act.setActivityId("ACT_" + System.nanoTime());
                    act.setActivity(a.getActivity());
                    act.setDuration(a.getDuration());
                    act.setDescription(a.getDescription());
                    // 🔥 SAVE LATITUDE & LONGITUDE
                    act.setLatitude(a.getLatitude());
                    act.setLongitude(a.getLongitude());
                    // 🔥 AUTO LOCATION FROM LAT LONG
                    if (a.getLatitude() != null
                            && !a.getLatitude().isBlank()
                            && a.getLongitude() != null
                            && !a.getLongitude().isBlank()) {

                        String location = getCityFromLatLong(
                                a.getLatitude(),
                                a.getLongitude()
                        );

                        act.setLocation(location);

                    } else {

                        act.setLocation(a.getLocation());
                    }

                    entity.getActivities().add(act);
                }
            }
            if ("PHYSIOTHERAPIST".equalsIgnoreCase(dto.getRole())) {

                TherapistAttendance attendance =
                        therapistAttendanceRepo.findByTherapistIdAndDate(
                                entity.getUserId(),
                                entity.getDate()
                        );

                if (attendance == null) {

                    attendance = new TherapistAttendance();
                    attendance.setTherapistId(entity.getUserId());
                    attendance.setDate(entity.getDate());
                }

                // ✅ Clinic & Branch
                attendance.setClinicId(entity.getClinicId());
                attendance.setBranchId(entity.getBranchId());

                // ✅ Login / Logout
                attendance.setStatus(entity.getStatus());
                attendance.setLogin(entity.getLogin());
                attendance.setLogout(entity.getLogout());

                attendance.setLogTime(entity.getLogTime());
                attendance.setWorkingHours(entity.getWorkingHours());
                attendance.setIdleTime(entity.getIdleTime());

                TherapistAttendance savedAttendance =
                          therapistAttendanceRepo.save(attendance);
                log.info("Saving therapist attendance therapistId={} date={}", attendance.getTherapistId(), attendance.getDate());
                
                response.setSuccess(true);
                response.setMessage("Therapist attendance saved successfully");
                response.setData(savedAttendance);
                response.setStatus(201);

                return response;
            }
            log.info("Saving attendance userId={} date={}", entity.getUserId(), entity.getDate());
            repo.save(entity);
            response.setSuccess(true);
            
            response.setMessage(
                    existingOpt.isPresent()
                            ? "Activity added to existing attendance"
                            : "Attendance created successfully"
            );
            response.setData(entity);
            response.setStatus(existingOpt.isPresent() ? 200 : 201);

        } catch (Exception e) {

            log.error("Error while saving attendance", e);
            response.setSuccess(false);  
            response.setMessage(e.getMessage());
            response.setStatus(200);     
        }

        return response;
    }
    
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "updateActivityFallback")
	public Response updateActivity(AttendanceDTO dto) {

        log.info("Attendance updateActivity started userId={} date={}", dto.getUserId(), dto.getDate());

        Response response = new Response();

        try {

            // 🔥 VALIDATION UPDATED
            if (dto.getUserId() == null || dto.getDate() == null) {
                throw new RuntimeException("userId and date are required");
            }

            Optional<Attendance> optional =
                    repo.findByUserIdAndDate(dto.getUserId(), dto.getDate());

            Attendance entity;

            if (optional.isPresent()) {
                entity = optional.get();
            } else {
                throw new RuntimeException("Attendance not found for update");
            }
            boolean updated = false;
            
//            if (dto.getDescription() != null
//                    && !dto.getDescription().isBlank()) {
//
//                entity.setDescription(dto.getDescription());
                updated = true;
//            }

         // ✅ LOGIN UPDATE
            if (dto.getLoginTime() != null
                    || dto.getLoginLocation() != null
                    || dto.getLoginLatitude() != null
                    || dto.getLoginLongitude() != null) {

                if (entity.getLogin() == null) {
                    entity.setLogin(new TimeLocation());
                }

                if (dto.getLoginTime() != null) {
                    entity.getLogin().setTime(dto.getLoginTime());
                }

                // 🔥 SAVE LATITUDE
                if (dto.getLoginLatitude() != null) {
                    entity.getLogin().setLatitude(dto.getLoginLatitude());
                }

                // 🔥 SAVE LONGTITUDE
                if (dto.getLoginLongitude() != null) {
                    entity.getLogin().setLongitude(dto.getLoginLongitude());
                }

                // 🔥 AUTO LOCATION FROM LAT LONG
                if (dto.getLoginLatitude() != null
                        && dto.getLoginLongitude() != null) {

                    String location = getCityFromLatLong(
                            dto.getLoginLatitude(),
                            dto.getLoginLongitude()
                    );

                    entity.getLogin().setLocation(location);

                } else if (dto.getLoginLocation() != null) {

                    entity.getLogin().setLocation(dto.getLoginLocation());
                }

                entity.setStatus("LOGGED_IN");
                updated = true;
            }
         // ✅ LOGOUT UPDATE
            if (dto.getLogoutTime() != null
                    || dto.getLogoutLocation() != null
                    || dto.getLogoutLatitude() != null
                    || dto.getLogoutLongitude() != null) {

                if (entity.getLogout() == null) {
                    entity.setLogout(new TimeLocation());
                }

                if (dto.getLogoutTime() != null) {
                    entity.getLogout().setTime(dto.getLogoutTime());
                }

                // 🔥 SAVE LATITUDE
                if (dto.getLogoutLatitude() != null) {
                    entity.getLogout().setLatitude(dto.getLogoutLatitude());
                }

                // 🔥 SAVE LONGTITUDE
                if (dto.getLogoutLongitude() != null) {
                    entity.getLogout().setLongitude(dto.getLogoutLongitude());
                }

                // 🔥 AUTO LOCATION FROM LAT LONG
                if (dto.getLogoutLatitude() != null
                        && dto.getLogoutLongitude() != null) {

                    String location = getCityFromLatLong(
                            dto.getLogoutLatitude(),
                            dto.getLogoutLongitude()
                    );

                    entity.getLogout().setLocation(location);

                } else if (dto.getLogoutLocation() != null) {

                    entity.getLogout().setLocation(dto.getLogoutLocation());
                }

                entity.setStatus("LOGGED_OUT");
                updated = true;
            }

            // 🔥 ACTIVITY UPDATE (UNCHANGED LOGIC)
            if (dto.getActivities() != null && !dto.getActivities().isEmpty()) {

                if (entity.getActivities() != null && !entity.getActivities().isEmpty()) {

                    for (ActivityDTO incoming : dto.getActivities()) {

                        if (incoming.getActivityId() == null) {
                            throw new RuntimeException("activityId is required");
                        }

                        for (Activity existing : entity.getActivities()) {

                            if (existing.getActivityId().equals(incoming.getActivityId())) {

                                if (incoming.getActivity() != null)
                                    existing.setActivity(incoming.getActivity());

                                if (incoming.getDuration() != null)
                                    existing.setDuration(incoming.getDuration());
                                if (incoming.getDescription() != null)
                                    existing.setDescription(incoming.getDescription());
                                if (incoming.getLocation() != null)
                                    existing.setLocation(incoming.getLocation());

                                updated = true;

                                // 🔥 ORIGINAL CALCULATION (UNCHANGED)
                                if (entity.getLogin() != null && entity.getLogout() != null) {

                                    int loginMin = parseTimeToMinutes(entity.getLogin().getTime());
                                    int logoutMin = parseTimeToMinutes(entity.getLogout().getTime());

                                    int total = logoutMin - loginMin;
                                    entity.setLogTime(formatMinutes(total));

                                    int workingMinutes = 0;

                                    for (Activity a : entity.getActivities()) {
                                        if (a.getDuration() != null) {
                                            workingMinutes += parseTimeToMinutes(a.getDuration());
                                        }
                                    }

                                    entity.setWorkingHours(formatMinutes(workingMinutes));

                                    int idle = total - workingMinutes;
                                    if (idle < 0) idle = 0;

                                    entity.setIdleTime(formatMinutes(idle));
                                }

                                break;
                            }
                        }

                        if (updated) break;
                    }
                }

                // 🔥 EXISTING BLOCK (UNCHANGED)
                if (entity.getLogin() != null && entity.getLogout() != null) {

                    int loginMin = parseTimeToMinutes(entity.getLogin().getTime());
                    int logoutMin = parseTimeToMinutes(entity.getLogout().getTime());

                    int total = logoutMin - loginMin;
                    entity.setLogTime(formatMinutes(total));

                    int workingMinutes = 0;

                    if (entity.getActivities() != null) {
                        for (Activity a : entity.getActivities()) {
                            if (a.getDuration() != null) {
                                workingMinutes += parseTimeToMinutes(a.getDuration());
                            }
                        }
                    }

                    entity.setWorkingHours(formatMinutes(workingMinutes));

                    int idle = total - workingMinutes;
                    if (idle < 0) idle = 0;

                    entity.setIdleTime(formatMinutes(idle));
                }
            }

            // 🔥 FINAL CALCULATION (UNCHANGED)
            if (entity.getLogin() != null && entity.getLogout() != null) {

                int loginMin = parseTimeToMinutes(entity.getLogin().getTime());
                int logoutMin = parseTimeToMinutes(entity.getLogout().getTime());

                int total = logoutMin - loginMin;
                entity.setLogTime(formatMinutes(total));

                int workingMinutes = 0;

                if (entity.getActivities() != null) {
                    for (Activity a : entity.getActivities()) {
                        if (a.getDuration() != null) {
                            workingMinutes += parseTimeToMinutes(a.getDuration());
                        }
                    }
                }

                entity.setWorkingHours(formatMinutes(workingMinutes));

                int idle = total - workingMinutes;
                if (idle < 0) idle = 0;

                entity.setIdleTime(formatMinutes(idle));
            }

            if (updated) {
                repo.save(entity);
            } else {
                throw new RuntimeException("No matching update found");
            }

            response.setSuccess(true);
            response.setMessage("Attendance updated successfully");
            response.setStatus(200);

        } catch (Exception e) {
        	 log.error("{}", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(400);
        }

        return response;
    }
    
    
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getDailyFallback")
	public Response getDaily(String userId, String date) {

        log.info("Fetching daily attendance userId={} date={}", userId, date);

        Response response = new Response();

        try {

            // ✅ FIND ATTENDANCE FOR PASSED DATE
            Optional<Attendance> optional =
                    repo.findByUserIdAndDate(userId, date);

            //  IF NO RECORD FOUND
            // RETURN EMPTY RESPONSE
            if (!optional.isPresent()) {

                DailyAttendanceResponseDTO emptyDto =
                        new DailyAttendanceResponseDTO();

                emptyDto.setDate(date);
                emptyDto.setStatus("Not Logged In");
                emptyDto.setLogTime(null);
                emptyDto.setLogin(null);
                emptyDto.setLogout(null);
                emptyDto.setActivities(new ArrayList<>());

                response.setSuccess(true);
                response.setMessage("No attendance found");
                response.setData(emptyDto);
                response.setStatus(200);

                return response;
            }

            Attendance entity = repo.findByUserIdAndDate(userId, date)
                    .orElseThrow(() -> new RuntimeException("No data found"));

            DailyAttendanceResponseDTO dto = new DailyAttendanceResponseDTO();

            dto.setDate(entity.getDate());
            dto.setLogTime(entity.getLogTime());
            dto.setStatus(entity.getStatus());
//            dto.setDescription(entity.getDescription());
            // 🔹 LOGIN
            if (entity.getLogin() != null) {

                TimeLocationDTO login = new TimeLocationDTO();

                login.setTime(entity.getLogin().getTime());

                // 🔥 LAT LONG
                login.setLatitude(entity.getLogin().getLatitude());
                login.setLongitude(entity.getLogin().getLongitude());

                // 🔥 AUTO LOCATION FROM LAT LONG
                if (entity.getLogin().getLatitude() != null
                        && entity.getLogin().getLongitude() != null) {

                    login.setLocation(
                            getCityFromLatLong(
                                    entity.getLogin().getLatitude(),
                                    entity.getLogin().getLongitude()
                            )
                    );

                } else {

                    login.setLocation(entity.getLogin().getLocation());
                }

                dto.setLogin(login);
            }

            // 🔹 LOGOUT
            if (entity.getLogout() != null) {

                TimeLocationDTO logout = new TimeLocationDTO();

                logout.setTime(entity.getLogout().getTime());

                // 🔥 LAT LONG
                logout.setLatitude(entity.getLogout().getLatitude());
                logout.setLongitude(entity.getLogout().getLongitude());

                // 🔥 AUTO LOCATION FROM LAT LONG
                if (entity.getLogout().getLatitude() != null
                        && entity.getLogout().getLongitude() != null) {

                    logout.setLocation(
                            getCityFromLatLong(
                                    entity.getLogout().getLatitude(),
                                    entity.getLogout().getLongitude()
                            )
                    );

                } else {

                    logout.setLocation(entity.getLogout().getLocation());
                }

                dto.setLogout(logout);
            }

            // 🔹 ACTIVITIES
            if (entity.getActivities() != null) {

                List<ActivityDTO> activities = entity.getActivities()
                        .stream()
                        .map(a -> {

                            ActivityDTO ad = new ActivityDTO();

                            ad.setActivityId(a.getActivityId());
                            ad.setActivity(a.getActivity());
                            ad.setDuration(a.getDuration());
                            ad.setDescription(a.getDescription());

                            // 🔥 LAT LONG
                            ad.setLatitude(a.getLatitude());
                           ad.setLongitude(a.getLongitude());

                            // 🔥 AUTO LOCATION FROM LAT LONG
                            if (a.getLatitude() != null
                                    && a.getLongitude() != null) {

                                ad.setLocation(
                                        getCityFromLatLong(
                                                a.getLatitude(),
                                                a.getLongitude()
                                        )
                                );

                            } else {

                                ad.setLocation(a.getLocation());
                            }

                            return ad;

                        }).collect(Collectors.toList());

                dto.setActivities(activities);
            }

            response.setSuccess(true);
            response.setMessage("Daily report fetched successfully");
            response.setData(dto);
            response.setStatus(200);

        } catch (Exception e) {
       	 log.error("{}", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(404);
        }

        return response;
    }

    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getMonthlyReportFallback")
	public Response getMonthlyReport(String userId, String month) {

        log.info("Fetching monthly report userId={} month={}", userId, month);

        Response response = new Response();

        try {

            // 🔥 VALIDATION
            if (userId == null || month == null || month.length() != 7) {
                throw new RuntimeException("Invalid month format. Use yyyy-MM");
            }

            // 🔥 CONVERT month → date range
            String[] parts = month.split("-");
            int year = Integer.parseInt(parts[0]);
            int mon = Integer.parseInt(parts[1]);

            String startDate = month + "-01";

            int lastDay = java.time.YearMonth.of(year, mon).lengthOfMonth();
            String endDate = month + "-" + (lastDay < 10 ? "0" + lastDay : lastDay);

            // 🔥 ORIGINAL QUERY (UNCHANGED)
            List<Attendance> list =
            	    repo.findByUserIdAndDateStartingWith(userId, month);

            List<MonthlyAttendanceResponseDTO> result = list.stream().map(att -> {

                MonthlyAttendanceResponseDTO dto = new MonthlyAttendanceResponseDTO();

                dto.setDate(att.getDate());

                if (att.getLogin() != null) {
                    dto.setInTime(att.getLogin().getTime());
                }

                if (att.getLogout() != null) {
                    dto.setOutTime(att.getLogout().getTime());
                }

                dto.setLogTime(att.getLogTime());
                dto.setWorkingHours(att.getWorkingHours());
                dto.setIdleTime(att.getIdleTime());

                return dto;

            }).toList();

            response.setSuccess(true);
            response.setMessage("Monthly report fetched successfully");
            response.setData(result);
            response.setStatus(200);

        } catch (Exception e) {
       	 log.error("{}", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(400);
        }

        return response;
    }
    private void mapDtoToEntity(AttendanceDTO dto, Attendance entity) {

        entity.setUserId(dto.getUserId());
        entity.setRole(dto.getRole());
        entity.setClinicId(dto.getClinicId());
        entity.setBranchId(dto.getBranchId());
        entity.setDate(dto.getDate());
//        entity.setDescription(dto.getDescription());
        entity.setRole(
        	    dto.getRole() != null
        	        ? dto.getRole().trim().toUpperCase()
        	        : null
        	);

        // 🔹 LOGIN
        if (dto.getLogin() != null) {

            TimeLocation login = new TimeLocation();

            login.setTime(dto.getLogin().getTime());

            // 🔥 SAVE LAT LONG
            login.setLatitude(dto.getLogin().getLatitude());
            login.setLongitude(dto.getLogin().getLongitude());

            // 🔥 AUTO LOCATION FROM LAT LONG
            if (dto.getLogin().getLatitude() != null
                    && !dto.getLogin().getLatitude().isBlank()
                    && dto.getLogin().getLongitude() != null
                    && !dto.getLogin().getLongitude().isBlank()) {

                login.setLocation(
                        getCityFromLatLong(
                                dto.getLogin().getLatitude(),
                                dto.getLogin().getLongitude()
                        )
                );

            } else {

                login.setLocation(dto.getLogin().getLocation());
            }

            entity.setLogin(login);
        }

        // 🔹 LOGOUT
        if (dto.getLogout() != null) {

            TimeLocation logout = new TimeLocation();

            logout.setTime(dto.getLogout().getTime());

            // 🔥 SAVE LAT LONG
            logout.setLatitude(dto.getLogout().getLatitude());
            logout.setLongitude(dto.getLogout().getLongitude());

            // 🔥 AUTO LOCATION FROM LAT LONG
            if (dto.getLogout().getLatitude() != null
                    && !dto.getLogout().getLatitude().isBlank()
                    && dto.getLogout().getLongitude() != null
                    && !dto.getLogout().getLongitude().isBlank()) {

                logout.setLocation(
                        getCityFromLatLong(
                                dto.getLogout().getLatitude(),
                                dto.getLogout().getLongitude()
                        )
                );

            } else {

                logout.setLocation(dto.getLogout().getLocation());
            }

            entity.setLogout(logout);
        }
    }
    private AttendanceDTO mapEntityToDto(Attendance entity) {

        AttendanceDTO dto = new AttendanceDTO();

        dto.setUserId(entity.getUserId());
        dto.setRole(entity.getRole());
        dto.setClinicId(entity.getClinicId());
        dto.setBranchId(entity.getBranchId());
        dto.setDate(entity.getDate());

        // 🔹 LOGIN
        if (entity.getLogin() != null) {

            TimeLocationDTO login = new TimeLocationDTO();

            login.setTime(entity.getLogin().getTime());

            // 🔥 AUTO CONVERT LOGIN LOCATION
            if (entity.getLogin().getLatitude() != null
                    && entity.getLogin().getLongitude() != null) {

                login.setLatitude(entity.getLogin().getLatitude());
                login.setLongitude(entity.getLogin().getLongitude());

                login.setLocation(
                        getCityFromLatLong(
                                entity.getLogin().getLatitude(),
                                entity.getLogin().getLongitude()
                        )
                );

            } else {

                login.setLocation(entity.getLogin().getLocation());
            }

            dto.setLogin(login);
        }

        // 🔹 LOGOUT
        if (entity.getLogout() != null) {

            TimeLocationDTO logout = new TimeLocationDTO();

            logout.setTime(entity.getLogout().getTime());

            // 🔥 AUTO CONVERT LOGOUT LOCATION
            if (entity.getLogout().getLatitude() != null
                    && entity.getLogout().getLongitude() != null) {

                logout.setLatitude(entity.getLogout().getLatitude());
                logout.setLongitude(entity.getLogout().getLongitude());

                logout.setLocation(
                        getCityFromLatLong(
                                entity.getLogout().getLatitude(),
                                entity.getLogout().getLongitude()
                        )
                );

            } else {

                logout.setLocation(entity.getLogout().getLocation());
            }

            dto.setLogout(logout);
        }

        // 🔹 ACTIVITIES
        if (entity.getActivities() != null && !entity.getActivities().isEmpty()) {

            List<ActivityDTO> activities = entity.getActivities().stream().map(a -> {

                ActivityDTO act = new ActivityDTO();

                act.setActivityId(a.getActivityId());
                act.setActivity(a.getActivity());
                act.setDuration(a.getDuration());

                // 🔥 LAT LONG
                act.setLatitude(a.getLatitude());
                act.setLongitude(a.getLongitude());

                // 🔥 AUTO CONVERT LOCATION
                if (a.getLatitude() != null
                        && a.getLongitude() != null) {

                    act.setLocation(
                            getCityFromLatLong(
                                    a.getLatitude(),
                                    a.getLongitude()
                            )
                    );

                } else {

                    act.setLocation(a.getLocation());
                }

                return act;

            }).toList();

            dto.setActivities(activities);
        }

        return dto;
    }

    private int parseTimeToMinutes(String input) {

        if (input == null || input.trim().isEmpty()) return 0;

        input = input.toLowerCase().replaceAll("\\s+", "");

        int hours = 0;
        int minutes = 0;

        try {

            // 🔹 Format: HH:mm (09:30)
            if (input.contains(":")) {
                String[] parts = input.split(":");
                hours = Integer.parseInt(parts[0]);
                minutes = Integer.parseInt(parts[1]);
            }

            // 🔹 Format: 2h30m / 2hr30min / 2hrs30minutes
            else if (input.contains("h")) {
                String[] parts = input.split("h");

                // hours
                hours = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));

                // minutes
                if (parts.length > 1) {
                    String minPart = parts[1].replaceAll("[^0-9]", "");
                    if (!minPart.isEmpty()) {
                        minutes = Integer.parseInt(minPart);
                    }
                }
            }

            // 🔹 Format: 150m or 150
            else {
                minutes = Integer.parseInt(input.replaceAll("[^0-9]", ""));
            }

        } catch (Exception e) {
            return 0;
        }

        return hours * 60 + minutes;
    }
    private String formatMinutes(int total) {
        return (total / 60) + "h " + (total % 60) + "m";
    }

    private String getCityFromLatLong(String lat, String lon) {

        try {

            String url = "https://nominatim.openstreetmap.org/reverse?lat="
                    + lat + "&lon=" + lon + "&format=json";

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "clinic-admin-app");

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

            // 🔥 Extract exact fields
            String road = (String) address.getOrDefault("road", "");
            String area = (String) address.getOrDefault("suburb",
                            address.getOrDefault("neighbourhood", ""));
            String city = (String) address.getOrDefault("city",
                            address.getOrDefault("town",
                            address.getOrDefault("village", "")));
            String state = (String) address.getOrDefault("state", "");
            String country = (String) address.getOrDefault("country", "");

            // 🔥 Build clean format (no nulls, no extra commas)
            return Stream.of(road, area, city, state, country)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(", "));

        } catch (Exception e) {
       	 log.error("{}", e);
            return "Unknown";
        }
    }

    private void validateLoginDistance(
            String clinicId,
            String branchId,
            String role, 
            String userLatitude,
            String userLongitude) {
    	
      
            // 🔥 Get complete clinic details
            Response responseEntity =
                    adminServiceClient.getClinicById(keyCloakTokenStore.getAccess_token(),clinicId);
            if (responseEntity == null
                    || responseEntity == null
                    || responseEntity.getData() == null) {
                throw new RuntimeException("Clinic location not found");
            }
            
            try {
    	       
            	// 🔥 DOCTOR & PHYSIOTHERAPIST — skip all distance validation
            	if ("doctor".equalsIgnoreCase(role)
            	        || "physiotherapist".equalsIgnoreCase(role)) {
            	    return;
            	}
				
				  // ✅ BRANCH ID CHECK — required for non-doctors
		        if (branchId == null || branchId.isBlank()) {
		            throw new RuntimeException("branchId is required");
		        }

            // 🔥 Convert clinic data to Map
            Map<String, Object> clinic =
                    (Map<String, Object>) responseEntity.getData();

            // 🔥 Get branches array from clinic
            List<Map<String, Object>> branches =
                    (List<Map<String, Object>>) clinic.get("branches");

            if (branches == null || branches.isEmpty()) {
                throw new RuntimeException("No branches found for this clinic");
            }

            // 🔥 Find matching branch by branchId
            Map<String, Object> branch = null;

            for (Map<String, Object> b : branches) {
                if (branchId.equals(String.valueOf(b.get("branchId")))) {
                    branch = b;
                    break;
                }
            }

            if (branch == null) {
                throw new RuntimeException(
                        "Branch not found for clinicId: "
                                + clinicId
                                + " and branchId: "
                                + branchId
                );
            }

            // 🔥 Check latitude and longitude
            if (branch.get("latitude") == null
                    || branch.get("longitude") == null) {
                throw new RuntimeException(
                        "Branch latitude/longitude not configured"
                );
            }

            // 🔥 Read branch coordinates
            double branchLat =
                    Double.parseDouble(
                            branch.get("latitude").toString().trim()
                    );
            double branchLon =
                    Double.parseDouble(
                            branch.get("longitude").toString().trim()
                    );

            // 🔥 Read user coordinates
            double userLat =
                    Double.parseDouble(userLatitude.trim());
            double userLon =
                    Double.parseDouble(userLongitude.trim());

            // 🔥 Quick equality check for identical coordinates
            if (Math.abs(branchLat - userLat) < 0.000001
                    && Math.abs(branchLon - userLon) < 0.000001) {
                return;
            }

            // 🔥 Calculate distance in meters
            double distance = calculateDistanceMeters(
                    branchLat,
                    branchLon,
                    userLat,
                    userLon
            );

            // 🔥 Allow login only within 20 meters
            if (distance > 20) {
                throw new RuntimeException(
                        "Login denied. You must be within 20 meters of the branch. Current distance: "
                                + Math.round(distance) + " meters."
                );
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
       	 log.error("{}", e);
            throw new RuntimeException(
                    "Unable to validate branch location: " + e.getMessage()
            );
        }
    }
    
    private double calculateDistanceMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        final double EARTH_RADIUS = 6371000; // meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        return EARTH_RADIUS * c;
    }
    
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getDailyByClinicAndBranchFallback")
	public Response getDailyByClinicAndBranch(

            String clinicId,
            String branchId,
            String date) {

        Response response = new Response();

        try {

            // =========================================================
            // GET ALL USERS FROM LOGIN CREDENTIALS
            // BASED ON clinicId + branchId
            // This ensures every user is returned even if they have not
            // logged in today.
            // =========================================================
            List<LoginEntity> users =
                    credentialsRepository.findByHospitalIdAndBranchId(
                            clinicId,
                            branchId
                    );
//
//            if (users == null || users.isEmpty()) {
//                throw new RuntimeException("No users found for this clinic and branch");
//            }

            List<DailyAllUsersResponseDTO> result = new ArrayList<>();


      // =========================================================
      // ADD BRANCH ADMIN
      // =========================================================
      try {

          Response branchResponse =
                  adminServiceClient.getAllBranches(keyCloakTokenStore.getAccess_token());

          if (branchResponse != null
                  && branchResponse.getData() != null) {

              // ✅ BUILD clinicId → clinicName MAP from getAllClinics()
              Map<String, String> clinicNameMap = new HashMap<>();
              try {
                  Response clinicRes =
                          adminServiceClient.getAllClinics(keyCloakTokenStore.getAccess_token());

                  if (clinicRes != null
                          && clinicRes.getData() != null) {
                      List<Map<String, Object>> cls =
                              (List<Map<String, Object>>) clinicRes.getData();
                      for (Map<String, Object> c : cls) {
                          String hId = c.get("hospitalId") != null
                                  ? c.get("hospitalId").toString() : "";
                          String hName = c.get("name") != null
                                  ? c.get("name").toString() : "";
                          clinicNameMap.put(hId, hName);
                      }
                  }
              } catch (Exception e) {
                  System.out.println("clinicNameMap build error: "
                          + e.getMessage());
              }

              List<Map<String, Object>> branches =
                      (List<Map<String, Object>>) branchResponse.getData();

              for (Map<String, Object> branch : branches) {

                  String bClinicId = branch.get("clinicId") != null
                          ? branch.get("clinicId").toString() : "";
                  String bBranchId = branch.get("branchId") != null
                          ? branch.get("branchId").toString() : "";

                  // ✅ FILTER — only process matching clinicId + branchId
                  if (!bClinicId.equals(clinicId) || !bBranchId.equals(branchId)) {
                      continue;
                  }

                  DailyAllUsersResponseDTO branchDto =
                          new DailyAllUsersResponseDTO();

                  // BASIC DETAILS
                  branchDto.setUserId(
                          branch.get("branchId") != null
                                  ? branch.get("branchId").toString()
                                  : "");

                  // ✅ HOSPITAL NAME instead of branch name
                  branchDto.setName(clinicNameMap.getOrDefault(bClinicId, ""));

                  branchDto.setRole(
                          branch.get("role") != null
                                  ? branch.get("role").toString()
                                  : "ADMIN");

                  branchDto.setClinicId(
                          branch.get("clinicId") != null
                                  ? branch.get("clinicId").toString()
                                  : "");

                  branchDto.setBranchId(
                          branch.get("branchId") != null
                                  ? branch.get("branchId").toString()
                                  : "");

                  branchDto.setDate(date);

                  // DEFAULT VALUES
                  branchDto.setStatus("Not Logged In");
                  branchDto.setLogTime(null);
                  branchDto.setWorkingHours("00:00");
                  branchDto.setIdleTime("00:00");
                  branchDto.setLogin(null);
                  branchDto.setLogout(null);

                  // FETCH ATTENDANCE
                  Optional<Attendance> attendanceOpt =
                          repo.findByClinicIdAndBranchIdAndUserIdAndDate(
                                  branchDto.getClinicId(),
                                  branchDto.getBranchId(),
                                  branchDto.getUserId(),
                                  date
                          );

                  // FALLBACK
                  if (!attendanceOpt.isPresent()) {
                      attendanceOpt = repo.findByUserIdAndDate(
                              branchDto.getUserId(),
                              date
                      );
                  }

                  // MAP ATTENDANCE
                  if (attendanceOpt.isPresent()) {

                      Attendance entity = attendanceOpt.get();

                      branchDto.setStatus(
                              entity.getStatus() != null
                                      ? entity.getStatus()
                                      : "Not Logged In"
                      );

                      branchDto.setLogTime(entity.getLogTime());

                      branchDto.setWorkingHours(
                              entity.getWorkingHours() != null
                                      ? entity.getWorkingHours()
                                      : "00:00"
                      );

                      branchDto.setIdleTime(
                              entity.getIdleTime() != null
                                      ? entity.getIdleTime()
                                      : "00:00"
                      );

                      // LOGIN
                      if (entity.getLogin() != null) {

                          TimeLocationDTO login = new TimeLocationDTO();
                          login.setTime(entity.getLogin().getTime());
                          login.setLatitude(entity.getLogin().getLatitude());
                          login.setLongitude(entity.getLogin().getLongitude());
                          login.setLocation(
                                  getCityFromLatLong(
                                          entity.getLogin().getLatitude(),
                                          entity.getLogin().getLongitude()
                                  )
                          );
                          branchDto.setLogin(login);
                      }

                      // LOGOUT
                      if (entity.getLogout() != null) {

                          TimeLocationDTO logout = new TimeLocationDTO();
                          logout.setTime(entity.getLogout().getTime());
                          logout.setLatitude(entity.getLogout().getLatitude());
                          logout.setLongitude(entity.getLogout().getLongitude());
                          logout.setLocation(
                                  getCityFromLatLong(
                                          entity.getLogout().getLatitude(),
                                          entity.getLogout().getLongitude()
                                  )
                          );
                          branchDto.setLogout(logout);
                      }
                  }

                  result.add(branchDto);
              }
          }

      } catch (Exception e) {
     	 log.error("{}", e);
      }
            // =========================================================
            // LOOP THROUGH ALL USERS
            // =========================================================
            for (LoginEntity user : users) {

                DailyAllUsersResponseDTO dto =
                        new DailyAllUsersResponseDTO();

                // =====================================================
                // BASIC USER DETAILS — always from credentials (live)
                // ✅ Name and role always reflect latest DB value
                // =====================================================
                dto.setUserId(user.getStaffId());
                dto.setName(user.getStaffName());
                dto.setRole(user.getRole());
                dto.setClinicId(clinicId);
                dto.setBranchId(branchId);
                dto.setDate(date);

                // =====================================================
                // DEFAULT VALUES
                // If user has not logged in today, these will be returned
                // =====================================================
                dto.setStatus("Not Logged In");
                dto.setLogTime(null);
                dto.setWorkingHours("00:00");
                dto.setIdleTime("00:00");
                dto.setLogin(null);
                dto.setLogout(null);
                
                // =====================================================
                // THERAPIST ATTENDANCE
                // =====================================================
                if ("physiotherapist".equalsIgnoreCase(user.getRole())) {

                    TherapistAttendance therapistAttendance =
                            therapistAttendanceRepo.findByTherapistIdAndDate(
                                    user.getStaffId(),
                                    date
                            );

                    if (therapistAttendance != null) {

                        dto.setStatus(
                                therapistAttendance.getStatus() != null
                                        ? therapistAttendance.getStatus()
                                        : "Not Logged In"
                        );

                        dto.setLogTime(therapistAttendance.getLogTime());

                        dto.setWorkingHours(
                                therapistAttendance.getWorkingHours() != null
                                        ? therapistAttendance.getWorkingHours()
                                        : "00:00"
                        );

                        dto.setIdleTime(
                                therapistAttendance.getIdleTime() != null
                                        ? therapistAttendance.getIdleTime()
                                        : "00:00"
                        );

                        // LOGIN
                        if (therapistAttendance.getLogin() != null) {

                            TimeLocationDTO login = new TimeLocationDTO();

                            login.setTime(
                                    therapistAttendance.getLogin().getTime()
                            );

                            login.setLatitude(
                                    therapistAttendance.getLogin().getLatitude()
                            );

                            login.setLongitude(
                                    therapistAttendance.getLogin().getLongitude()
                            );

                            if (therapistAttendance.getLogin().getLatitude() != null
                                    && therapistAttendance.getLogin().getLongitude() != null) {

                                login.setLocation(
                                        getCityFromLatLong(
                                                therapistAttendance.getLogin().getLatitude(),
                                                therapistAttendance.getLogin().getLongitude()
                                        )
                                );
                            }

                            dto.setLogin(login);
                        }

                        // LOGOUT
                        if (therapistAttendance.getLogout() != null) {

                            TimeLocationDTO logout = new TimeLocationDTO();

                            logout.setTime(
                                    therapistAttendance.getLogout().getTime()
                            );

                            logout.setLatitude(
                                    therapistAttendance.getLogout().getLatitude()
                            );

                            logout.setLongitude(
                                    therapistAttendance.getLogout().getLongitude()
                            );

                            if (therapistAttendance.getLogout().getLatitude() != null
                                    && therapistAttendance.getLogout().getLongitude() != null) {

                                logout.setLocation(
                                        getCityFromLatLong(
                                                therapistAttendance.getLogout().getLatitude(),
                                                therapistAttendance.getLogout().getLongitude()
                                        )
                                );
                            }

                            dto.setLogout(logout);
                        }
                    }

                    result.add(dto);
                    continue;
                } // =====================================================
                // THERAPIST ATTENDANCE
                // =====================================================
                if ("physiotherapist".equalsIgnoreCase(user.getRole())) {

                    TherapistAttendance therapistAttendance =
                            therapistAttendanceRepo.findByTherapistIdAndDate(
                                    user.getStaffId(),
                                    date
                            );

                    if (therapistAttendance != null) {

                        dto.setStatus(
                                therapistAttendance.getStatus() != null
                                        ? therapistAttendance.getStatus()
                                        : "Not Logged In"
                        );

                        dto.setLogTime(therapistAttendance.getLogTime());

                        dto.setWorkingHours(
                                therapistAttendance.getWorkingHours() != null
                                        ? therapistAttendance.getWorkingHours()
                                        : "00:00"
                        );

                        dto.setIdleTime(
                                therapistAttendance.getIdleTime() != null
                                        ? therapistAttendance.getIdleTime()
                                        : "00:00"
                        );

                        // LOGIN
                        if (therapistAttendance.getLogin() != null) {

                            TimeLocationDTO login = new TimeLocationDTO();

                            login.setTime(
                                    therapistAttendance.getLogin().getTime()
                            );

                            login.setLatitude(
                                    therapistAttendance.getLogin().getLatitude()
                            );

                            login.setLongitude(
                                    therapistAttendance.getLogin().getLongitude()
                            );

                            if (therapistAttendance.getLogin().getLatitude() != null
                                    && therapistAttendance.getLogin().getLongitude() != null) {

                                login.setLocation(
                                        getCityFromLatLong(
                                                therapistAttendance.getLogin().getLatitude(),
                                                therapistAttendance.getLogin().getLongitude()
                                        )
                                );
                            }

                            dto.setLogin(login);
                        }

                        // LOGOUT
                        if (therapistAttendance.getLogout() != null) {

                            TimeLocationDTO logout = new TimeLocationDTO();

                            logout.setTime(
                                    therapistAttendance.getLogout().getTime()
                            );

                            logout.setLatitude(
                                    therapistAttendance.getLogout().getLatitude()
                            );

                            logout.setLongitude(
                                    therapistAttendance.getLogout().getLongitude()
                            );

                            if (therapistAttendance.getLogout().getLatitude() != null
                                    && therapistAttendance.getLogout().getLongitude() != null) {

                                logout.setLocation(
                                        getCityFromLatLong(
                                                therapistAttendance.getLogout().getLatitude(),
                                                therapistAttendance.getLogout().getLongitude()
                                        )
                                );
                            }

                            dto.setLogout(logout);
                        }
                    }

                    result.add(dto);
                    continue;
                }

                // =====================================================
                // STEP 1: TRY FULL QUERY — clinicId + branchId + userId + date
                // =====================================================
                Optional<Attendance> attendanceOpt =
                        repo.findByClinicIdAndBranchIdAndUserIdAndDate(
                                clinicId,
                                branchId,
                                user.getStaffId(),
                                date
                        );

                // =====================================================
                // STEP 2: FALLBACK — userId + date only
                // Handles old records saved without clinicId/branchId
                // =====================================================
                if (!attendanceOpt.isPresent()) {
                    attendanceOpt = repo.findByUserIdAndDate(
                            user.getStaffId(),
                            date
                    );
                }

                // =====================================================
                // STEP 3: IF ATTENDANCE FOUND — MAP ALL FIELDS
                // =====================================================
                if (attendanceOpt.isPresent()) {

                    Attendance entity = attendanceOpt.get();

                    // -------------------------------------------------
                    // BASIC ATTENDANCE DATA
                    // ✅ Null-safe defaults
                    // -------------------------------------------------
                    dto.setStatus(
                            entity.getStatus() != null
                                    ? entity.getStatus()
                                    : "Not Logged In"
                    );

                    dto.setLogTime(entity.getLogTime());

                    dto.setWorkingHours(
                            entity.getWorkingHours() != null
                                    ? entity.getWorkingHours()
                                    : "00:00"
                    );

                    dto.setIdleTime(
                            entity.getIdleTime() != null
                                    ? entity.getIdleTime()
                                    : "00:00"
                    );

                    // -------------------------------------------------
                    // LOGIN
                    // ✅ Always re-resolve location from lat/long
                    //    so location name changes reflect immediately
                    // -------------------------------------------------
                    if (entity.getLogin() != null) {

                        TimeLocationDTO login = new TimeLocationDTO();
                        login.setTime(entity.getLogin().getTime());
                        login.setLatitude(entity.getLogin().getLatitude());
                        login.setLongitude(entity.getLogin().getLongitude());

                        if (entity.getLogin().getLatitude() != null
                                && !entity.getLogin().getLatitude().isBlank()
                                && entity.getLogin().getLongitude() != null
                                && !entity.getLogin().getLongitude().isBlank()) {

                            // ✅ Live resolve — always fresh location name
                            login.setLocation(
                                    getCityFromLatLong(
                                            entity.getLogin().getLatitude(),
                                            entity.getLogin().getLongitude()
                                    )
                            );

                        } else {

                            // Fallback: stored string if no lat/long available
                            login.setLocation(entity.getLogin().getLocation());
                        }

                        dto.setLogin(login);
                    }

                    // -------------------------------------------------
                    // LOGOUT
                    // ✅ Always re-resolve location from lat/long
                    // -------------------------------------------------
                    if (entity.getLogout() != null) {

                        TimeLocationDTO logout = new TimeLocationDTO();
                        logout.setTime(entity.getLogout().getTime());
                        logout.setLatitude(entity.getLogout().getLatitude());
                        logout.setLongitude(entity.getLogout().getLongitude());

                        if (entity.getLogout().getLatitude() != null
                                && !entity.getLogout().getLatitude().isBlank()
                                && entity.getLogout().getLongitude() != null
                                && !entity.getLogout().getLongitude().isBlank()) {

                            // ✅ Live resolve — always fresh location name
                            logout.setLocation(
                                    getCityFromLatLong(
                                            entity.getLogout().getLatitude(),
                                            entity.getLogout().getLongitude()
                                    )
                            );

                        } else {

                            // Fallback: stored string if no lat/long available
                            logout.setLocation(entity.getLogout().getLocation());
                        }

                        dto.setLogout(logout);
                    }

                    // -------------------------------------------------
                    // SELF-HEAL: Patch missing clinicId/branchId
                    // ✅ Old records get fixed automatically on first fetch
                    //    so future 4-field queries work correctly
                    // -------------------------------------------------
                    boolean needsUpdate = false;

                    if (entity.getClinicId() == null
                            || entity.getClinicId().isBlank()) {
                        entity.setClinicId(clinicId);
                        needsUpdate = true;
                    }

                    if (entity.getBranchId() == null
                            || entity.getBranchId().isBlank()) {
                        entity.setBranchId(branchId);
                        needsUpdate = true;
                    }

                    if (needsUpdate) {
                        // ✅ Silently patch and save — no impact on response
                        repo.save(entity);
                    }
                }

                // =====================================================
                // ADD TO RESULT LIST
                // =====================================================
                result.add(dto);
            }

            // =========================================================
            // SUCCESS RESPONSE
            // =========================================================
            response.setSuccess(true);
            response.setMessage("Today's attendance for all users fetched successfully");
            response.setData(result);
            response.setStatus(200);

        } catch (Exception e) {
       	 log.error("{}", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(404);
        }

        return response;
    }
    
    @Override
    @Secured("ROLE_CLINICADMIN")
    @RateLimiter(name = "clinicAdminService", fallbackMethod = "getMonthlyByClinicAndBranchFallback")
	public Response getMonthlyByClinicAndBranch(

            String clinicId,
            String branchId,
            String userId,
            String startDate,
            String endDate) {

        Response response = new Response();

        try {

            // =========================================================
            // VALIDATION
            // =========================================================
            if (clinicId == null || clinicId.isBlank()
                    || branchId == null || branchId.isBlank()
                    || userId == null || userId.isBlank()
                    || startDate == null || startDate.isBlank()
                    || endDate == null || endDate.isBlank()) {
                throw new RuntimeException(
                        "clinicId, branchId, userId, startDate and endDate are required"
                );
            }

            if (startDate.compareTo(endDate) > 0) {
                throw new RuntimeException(
                        "startDate must not be after endDate"
                );
            }

            // =========================================================
            // FETCH ATTENDANCE BETWEEN startDate AND endDate
            // =========================================================
            List<Attendance> attendanceList =
                    repo.findByClinicIdAndBranchIdAndUserIdAndDateBetween(
                            clinicId,
                            branchId,
                            userId,
                            startDate,
                            endDate
                    );

            // ---------------------------------------------------------
            // FALLBACK — old records without clinicId/branchId
            // ---------------------------------------------------------
            if (attendanceList == null || attendanceList.isEmpty()) {
                attendanceList =
                        repo.findByUserIdAndDateBetween(
                                userId,
                                startDate,
                                endDate
                        );
            }

            // =========================================================
            // MAP ONLY REQUIRED FIELDS
            // =========================================================
            List<MonthlyAttendanceResponseDTO> result = new ArrayList<>();

            for (Attendance att : attendanceList) {

                MonthlyAttendanceResponseDTO dto =
                        new MonthlyAttendanceResponseDTO();

                dto.setDate(att.getDate());

                if (att.getLogin() != null) {
                    dto.setInTime(att.getLogin().getTime());
                }

                if (att.getLogout() != null) {
                    dto.setOutTime(att.getLogout().getTime());
                }

                dto.setLogTime(att.getLogTime());
                dto.setWorkingHours(att.getWorkingHours());
                dto.setIdleTime(att.getIdleTime());

                result.add(dto);
            }

            // =========================================================
            // SUCCESS RESPONSE
            // =========================================================
            response.setSuccess(true);
            response.setMessage(
                    "Attendance report for user " + userId
                            + " from " + startDate + " to " + endDate
                            + " fetched successfully"
            );
            response.setData(result);
            response.setStatus(200);

        } catch (Exception e) {
       	 log.error("{}", e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(400);
        }

        return response;
    }
    private void saveTherapistAttendance(Attendance attendance) {

        if (attendance == null
                || attendance.getRole() == null
                || !"PHYSIOTHERAPIST".equalsIgnoreCase(attendance.getRole())) {
            return;
        }

        TherapistAttendance therapistAttendance =
                therapistAttendanceRepo.findByTherapistIdAndDate(
                        attendance.getUserId(),
                        attendance.getDate()
                );

        if (therapistAttendance == null) {

            therapistAttendance = new TherapistAttendance();
            therapistAttendance.setTherapistId(attendance.getUserId());
            therapistAttendance.setDate(attendance.getDate());
        }

        therapistAttendance.setStatus(attendance.getStatus());
        therapistAttendance.setLogTime(attendance.getLogTime());
        therapistAttendance.setWorkingHours(attendance.getWorkingHours());
        therapistAttendance.setIdleTime(attendance.getIdleTime());

        if (attendance.getLogin() != null) {
            therapistAttendance.setLogin(attendance.getLogin());
        }

        if (attendance.getLogout() != null) {
            therapistAttendance.setLogout(attendance.getLogout());
        }

        therapistAttendanceRepo.save(therapistAttendance);
    }
   


    // ================= RATE LIMIT FALLBACK METHODS =================

   public Response saveFallback(AttendanceDTO dto, Exception ex) {
        log.error("Rate limiter triggered in save", ex);
        return buildResponse();
    }

    public Response updateActivityFallback(AttendanceDTO dto, Exception ex) {
        log.error("Rate limiter triggered in updateActivity", ex);
        return buildResponse();
    }

    public Response getDailyFallback(String userId, String date, Exception ex) {
        log.error("Rate limiter triggered in getDaily userId={} date={}", userId, date, ex);
        return buildResponse();
    }

    public Response getMonthlyReportFallback(String userId, String month, Exception ex) {
        log.error("Rate limiter triggered in getMonthlyReport userId={} month={}", userId, month, ex);
        return buildResponse();
    }

    public Response getDailyByClinicAndBranchFallback(
            String clinicId,
            String branchId,
            String date,
            Exception ex) {
        log.error("Rate limiter triggered in getDailyByClinicAndBranch clinicId={} branchId={} date={}",
                clinicId, branchId, date, ex);
        return buildResponse();
    }

    public Response getMonthlyByClinicAndBranchFallback(
            String clinicId,
            String branchId,
            String userId,
            String startDate,
            String endDate,
            Exception ex) {
        log.error("Rate limiter triggered in getMonthlyByClinicAndBranch clinicId={} branchId={} userId={}",
                clinicId, branchId, userId, ex);
        return buildResponse();
    }

    private Response buildResponse() {
        Response response = new Response();
        response.setSuccess(false);
        response.setStatus(429);
        response.setMessage("Too many requests. Please try again later.");
        return response;
    }
}