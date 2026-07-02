package com.clinicadmin.service.impl;

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

import com.clinicadmin.dto.ActivityDTO;
import com.clinicadmin.dto.AttendanceDTO;
import com.clinicadmin.dto.DailyAllUsersResponseDTO;
import com.clinicadmin.dto.DailyAttendanceResponseDTO;
import com.clinicadmin.dto.MonthlyAttendanceResponseDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.Session;
import com.clinicadmin.dto.TimeLocationDTO;
import com.clinicadmin.entity.Activity;
import com.clinicadmin.entity.Attendance;
import com.clinicadmin.entity.DoctorLoginCredentials;
import com.clinicadmin.entity.TherapistAttendance;
import com.clinicadmin.entity.TimeLocation;
import com.clinicadmin.feignclient.AdminServiceClient;
import com.clinicadmin.repository.AttendanceRepository;
import com.clinicadmin.repository.DoctorLoginCredentialsRepository;
import com.clinicadmin.repository.TherapistAttendanceRepository;
import com.clinicadmin.service.AttendanceService;
import com.clinicadmin.utils.KeyCloakTokenStore;
import lombok.RequiredArgsConstructor;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository repo;
    
    private final AdminServiceClient adminServiceClient;
    
    private final TherapistAttendanceRepository therapistAttendanceRepo;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
    
 
    @Autowired
    private DoctorLoginCredentialsRepository credentialsRepository;

    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response save(AttendanceDTO dto) {

        log.info("Attendance save request received. UserId: {}, Role: {}, ClinicId: {}, BranchId: {}, Date: {}",
                dto.getUserId(), dto.getRole(), dto.getClinicId(), dto.getBranchId(), dto.getDate());

        Response response = new Response();

        try {

            if (dto.getUserId() == null || dto.getDate() == null) {

                log.warn("Attendance save validation failed. UserId or Date is missing.");

                throw new RuntimeException("userId and date are required");
            }

            // Doctor validation
            if ("doctor".equalsIgnoreCase(dto.getRole())) {

                log.info("Validating doctor login. UserId: {}", dto.getUserId());

                validateLoginDistance(
                        dto.getClinicId(),
                        dto.getBranchId(),
                        dto.getRole(),
                        null,
                        null
                );

            } else {

                log.info("Validating login location. UserId: {}, Role: {}",
                        dto.getUserId(), dto.getRole());

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

                log.info("Attendance already exists. Updating existing attendance. UserId: {}",
                        dto.getUserId());

                entity = existingOpt.get();

            } else {

                log.info("Creating new attendance. UserId: {}", dto.getUserId());

                entity = new Attendance();
                mapDtoToEntity(dto, entity);
                entity.setActivities(new ArrayList<>());

                if (dto.getLogin() != null) {
                    entity.setStatus("LOGGED_IN");
                } else if (dto.getLogout() != null) {
                    entity.setStatus("LOGGED_OUT");
                } else {
                    entity.setStatus(null);
                }
            }

            if (dto.getActivities() != null && !dto.getActivities().isEmpty()) {

                log.info("Adding {} activities for UserId: {}",
                        dto.getActivities().size(),
                        dto.getUserId());

                if (entity.getActivities() == null) {
                    entity.setActivities(new ArrayList<>());
                }

                for (ActivityDTO a : dto.getActivities()) {

                    log.debug("Processing activity: {}", a.getActivity());

                    Activity act = new Activity();

                    act.setActivityId("ACT_" + System.nanoTime());
                    act.setActivity(a.getActivity());
                    act.setDuration(a.getDuration());
                    act.setDescription(a.getDescription());
                    act.setLatitude(a.getLatitude());
                    act.setLongitude(a.getLongitude());

                    if (a.getLatitude() != null
                            && !a.getLatitude().isBlank()
                            && a.getLongitude() != null
                            && !a.getLongitude().isBlank()) {

                        String location = getCityFromLatLong(
                                a.getLatitude(),
                                a.getLongitude());

                        act.setLocation(location);

                    } else {

                        act.setLocation(a.getLocation());
                    }

                    entity.getActivities().add(act);
                }
            }

            if ("PHYSIOTHERAPIST".equalsIgnoreCase(dto.getRole())) {

                log.info("Saving therapist attendance. TherapistId: {}",
                        entity.getUserId());

                TherapistAttendance attendance =
                        therapistAttendanceRepo.findByTherapistIdAndDate(
                                entity.getUserId(),
                                entity.getDate());

                if (attendance == null) {

                    log.info("Creating new therapist attendance record.");

                    attendance = new TherapistAttendance();
                    attendance.setTherapistId(entity.getUserId());
                    attendance.setDate(entity.getDate());
                }

                attendance.setClinicId(entity.getClinicId());
                attendance.setBranchId(entity.getBranchId());
                attendance.setStatus(entity.getStatus());
                attendance.setLogin(entity.getLogin());
                attendance.setLogout(entity.getLogout());
                attendance.setLogTime(entity.getLogTime());
                attendance.setWorkingHours(entity.getWorkingHours());
                attendance.setIdleTime(entity.getIdleTime());

                TherapistAttendance savedAttendance =
                        therapistAttendanceRepo.save(attendance);

                log.info("Therapist attendance saved successfully. TherapistId: {}",
                        entity.getUserId());

                response.setSuccess(true);
                response.setMessage("Therapist attendance saved successfully");
                response.setData(savedAttendance);
                response.setStatus(201);

                return response;
            }

            log.info("Saving attendance record. UserId: {}", entity.getUserId());

            repo.save(entity);

            log.info("Attendance saved successfully. UserId: {}, Status: {}",
                    entity.getUserId(),
                    entity.getStatus());

            response.setSuccess(true);
            response.setMessage(
                    existingOpt.isPresent()
                            ? "Activity added to existing attendance"
                            : "Attendance created successfully"
            );
            response.setData(entity);
            response.setStatus(existingOpt.isPresent() ? 200 : 201);

        } catch (Exception e) {

            log.error("Attendance save failed. UserId: {}, ClinicId: {}, Date: {}, Error: {}",
                    dto.getUserId(),
                    dto.getClinicId(),
                    dto.getDate(),
                    e.getMessage(),
                    e);

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(200);
        }

        return response;
    }
    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response updateActivity(AttendanceDTO dto) {

        log.info("Update attendance request received. UserId: {}, Date: {}",
                dto.getUserId(), dto.getDate());

        Response response = new Response();

        try {

            // Validation
            if (dto.getUserId() == null || dto.getDate() == null) {

                log.warn("Attendance update failed. UserId or Date is missing.");

                throw new RuntimeException("userId and date are required");
            }

            Optional<Attendance> optional =
                    repo.findByUserIdAndDate(dto.getUserId(), dto.getDate());

            Attendance entity;

            if (optional.isPresent()) {

                entity = optional.get();

                log.info("Attendance found for UserId: {}", dto.getUserId());

            } else {

                log.warn("Attendance not found for UserId: {}, Date: {}",
                        dto.getUserId(), dto.getDate());

                throw new RuntimeException("Attendance not found for update");
            }

            boolean updated = false;

            // LOGIN UPDATE
            if (dto.getLoginTime() != null
                    || dto.getLoginLocation() != null
                    || dto.getLoginLatitude() != null
                    || dto.getLoginLongitude() != null) {

                log.info("Updating login details for UserId: {}", dto.getUserId());

                if (entity.getLogin() == null) {
                    entity.setLogin(new TimeLocation());
                }

                if (dto.getLoginTime() != null) {
                    entity.getLogin().setTime(dto.getLoginTime());
                }

                if (dto.getLoginLatitude() != null) {
                    entity.getLogin().setLatitude(dto.getLoginLatitude());
                }

                if (dto.getLoginLongitude() != null) {
                    entity.getLogin().setLongitude(dto.getLoginLongitude());
                }

                if (dto.getLoginLatitude() != null
                        && dto.getLoginLongitude() != null) {

                    String location = getCityFromLatLong(
                            dto.getLoginLatitude(),
                            dto.getLoginLongitude());

                    entity.getLogin().setLocation(location);

                } else if (dto.getLoginLocation() != null) {

                    entity.getLogin().setLocation(dto.getLoginLocation());
                }

                entity.setStatus("LOGGED_IN");
                updated = true;

                log.debug("Login details updated successfully.");
            }

            // LOGOUT UPDATE
            if (dto.getLogoutTime() != null
                    || dto.getLogoutLocation() != null
                    || dto.getLogoutLatitude() != null
                    || dto.getLogoutLongitude() != null) {

                log.info("Updating logout details for UserId: {}", dto.getUserId());

                if (entity.getLogout() == null) {
                    entity.setLogout(new TimeLocation());
                }

                if (dto.getLogoutTime() != null) {
                    entity.getLogout().setTime(dto.getLogoutTime());
                }

                if (dto.getLogoutLatitude() != null) {
                    entity.getLogout().setLatitude(dto.getLogoutLatitude());
                }

                if (dto.getLogoutLongitude() != null) {
                    entity.getLogout().setLongitude(dto.getLogoutLongitude());
                }

                if (dto.getLogoutLatitude() != null
                        && dto.getLogoutLongitude() != null) {

                    String location = getCityFromLatLong(
                            dto.getLogoutLatitude(),
                            dto.getLogoutLongitude());

                    entity.getLogout().setLocation(location);

                } else if (dto.getLogoutLocation() != null) {

                    entity.getLogout().setLocation(dto.getLogoutLocation());
                }

                entity.setStatus("LOGGED_OUT");
                updated = true;

                log.debug("Logout details updated successfully.");
            }

            // ACTIVITY UPDATE
            if (dto.getActivities() != null && !dto.getActivities().isEmpty()) {

                log.info("Updating activities for UserId: {}", dto.getUserId());

                if (entity.getActivities() != null && !entity.getActivities().isEmpty()) {

                    for (ActivityDTO incoming : dto.getActivities()) {

                        if (incoming.getActivityId() == null) {

                            log.warn("Activity update failed. ActivityId is missing.");

                            throw new RuntimeException("activityId is required");
                        }

                        for (Activity existing : entity.getActivities()) {

                            if (existing.getActivityId().equals(incoming.getActivityId())) {

                                log.debug("Updating ActivityId: {}", incoming.getActivityId());

                                if (incoming.getActivity() != null)
                                    existing.setActivity(incoming.getActivity());

                                if (incoming.getDuration() != null)
                                    existing.setDuration(incoming.getDuration());

                                if (incoming.getDescription() != null)
                                    existing.setDescription(incoming.getDescription());

                                if (incoming.getLocation() != null)
                                    existing.setLocation(incoming.getLocation());

                                updated = true;

                                log.info("Activity updated successfully. ActivityId: {}",
                                        incoming.getActivityId());

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

                        if (updated) {
                            break;
                        }
                    }
                }

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

            // FINAL CALCULATION
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

                log.info("Saving updated attendance for UserId: {}", entity.getUserId());

                repo.save(entity);

                log.info("Attendance updated successfully for UserId: {}", entity.getUserId());

            } else {

                log.warn("No matching update found for UserId: {}", dto.getUserId());

                throw new RuntimeException("No matching update found");
            }

            response.setSuccess(true);
            response.setMessage("Attendance updated successfully");
            response.setStatus(200);

        } catch (Exception e) {

            log.error("Failed to update attendance. UserId: {}, Date: {}, Error: {}",
                    dto.getUserId(),
                    dto.getDate(),
                    e.getMessage(),
                    e);

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(400);
        }

        return response;
    }
    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response getDaily(String userId, String date) {

        log.info("Fetching daily attendance. UserId: {}, Date: {}", userId, date);

        Response response = new Response();

        try {

            // Find attendance for passed date
            Optional<Attendance> optional =
                    repo.findByUserIdAndDate(userId, date);

            // No attendance found
            if (!optional.isPresent()) {

                log.warn("No attendance found for UserId: {}, Date: {}", userId, date);

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

            log.info("Attendance record found for UserId: {}, Date: {}", userId, date);

            Attendance entity = optional.get();

            DailyAttendanceResponseDTO dto = new DailyAttendanceResponseDTO();

            dto.setDate(entity.getDate());
            dto.setLogTime(entity.getLogTime());
            dto.setStatus(entity.getStatus());

            // LOGIN
            if (entity.getLogin() != null) {

                log.debug("Mapping login details.");

                TimeLocationDTO login = new TimeLocationDTO();

                login.setTime(entity.getLogin().getTime());
                login.setLatitude(entity.getLogin().getLatitude());
                login.setLongitude(entity.getLogin().getLongitude());

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

            // LOGOUT
            if (entity.getLogout() != null) {

                log.debug("Mapping logout details.");

                TimeLocationDTO logout = new TimeLocationDTO();

                logout.setTime(entity.getLogout().getTime());
                logout.setLatitude(entity.getLogout().getLatitude());
                logout.setLongitude(entity.getLogout().getLongitude());

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

            // ACTIVITIES
            if (entity.getActivities() != null) {

                log.debug("Mapping {} activities.", entity.getActivities().size());

                List<ActivityDTO> activities = entity.getActivities()
                        .stream()
                        .map(a -> {

                            ActivityDTO ad = new ActivityDTO();

                            ad.setActivityId(a.getActivityId());
                            ad.setActivity(a.getActivity());
                            ad.setDuration(a.getDuration());
                            ad.setDescription(a.getDescription());
                            ad.setLatitude(a.getLatitude());
                            ad.setLongitude(a.getLongitude());

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

            log.info("Daily attendance fetched successfully for UserId: {}", userId);

            response.setSuccess(true);
            response.setMessage("Daily report fetched successfully");
            response.setData(dto);
            response.setStatus(200);

        } catch (Exception e) {

            log.error("Failed to fetch daily attendance. UserId: {}, Date: {}, Error: {}",
                    userId,
                    date,
                    e.getMessage(),
                    e);

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(404);
        }

        return response;
    }
    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response getMonthlyReport(String userId, String month) {

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
            ResponseEntity<Response> responseEntity =
                    adminServiceClient.getClinicById(keyCloakTokenStore.getAccess_token(),clinicId);

            if (responseEntity == null
                    || responseEntity.getBody() == null
                    || responseEntity.getBody().getData() == null) {
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
                    (Map<String, Object>) responseEntity.getBody().getData();

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
            List<DoctorLoginCredentials> users =
                    credentialsRepository.findByHospitalIdAndBranchId(
                            clinicId,
                            branchId
                    );
//
//            if (users == null || users.isEmpty()) {
//                throw new RuntimeException("No users found for this clinic and branch");
//            }

            List<DailyAllUsersResponseDTO> result = new ArrayList<>();
            
//         // =========================================================
//         // ADD CLINIC ADMIN
//         // =========================================================
//         try {
//
//             ResponseEntity<Response> clinicResponse =
//                     adminServiceClient.getAllClinics();
//
//             if (clinicResponse.getBody() != null
//                     && clinicResponse.getBody().getData() != null) {
//
//                 List<Map<String, Object>> clinics =
//                         (List<Map<String, Object>>) clinicResponse.getBody().getData();
//
//                 for (Map<String, Object> clinic : clinics) {
//
//                     // ✅ FILTER — only process matching clinicId
//                     String hospitalId = clinic.get("hospitalId") != null
//                             ? clinic.get("hospitalId").toString() : "";
//                     if (!hospitalId.equals(clinicId)) {
//                         continue;
//                     }
//
//                     DailyAllUsersResponseDTO clinicDto =
//                             new DailyAllUsersResponseDTO();
//
//                     // BASIC DETAILS
//                     clinicDto.setUserId(
//                             clinic.get("hospitalId") != null
//                                     ? clinic.get("hospitalId").toString()
//                                     : "");
//
//                     clinicDto.setName(
//                             clinic.get("name") != null
//                                     ? clinic.get("name").toString()
//                                     : "");
//
//                     clinicDto.setRole(
//                             clinic.get("role") != null
//                                     ? clinic.get("role").toString()
//                                     : "ADMIN");
//
//                     clinicDto.setClinicId(
//                             clinic.get("hospitalId") != null
//                                     ? clinic.get("hospitalId").toString()
//                                     : "");
//
//                     clinicDto.setBranchId(
//                             clinic.get("branch") != null
//                                     ? clinic.get("branch").toString()
//                                     : "");
//
//                     clinicDto.setDate(date);
//
//                     // DEFAULT VALUES
//                     clinicDto.setStatus("Not Logged In");
//                     clinicDto.setLogTime(null);
//                     clinicDto.setWorkingHours("00:00");
//                     clinicDto.setIdleTime("00:00");
//                     clinicDto.setLogin(null);
//                     clinicDto.setLogout(null);
//
//                     // FETCH ATTENDANCE
//                     Optional<Attendance> attendanceOpt =
//                             repo.findByClinicIdAndBranchIdAndUserIdAndDate(
//                                     clinicDto.getClinicId(),
//                                     clinicDto.getBranchId(),
//                                     clinicDto.getUserId(),
//                                     date
//                             );
//
//                     // FALLBACK
//                     if (!attendanceOpt.isPresent()) {
//
//                         attendanceOpt = repo.findByUserIdAndDate(
//                                 clinicDto.getUserId(),
//                                 date
//                         );
//                     }
//
//                     // MAP ATTENDANCE
//                     if (attendanceOpt.isPresent()) {
//
//                         Attendance entity = attendanceOpt.get();
//
//                         clinicDto.setStatus(
//                                 entity.getStatus() != null
//                                         ? entity.getStatus()
//                                         : "Not Logged In"
//                         );
//
//                         clinicDto.setLogTime(entity.getLogTime());
//
//                         clinicDto.setWorkingHours(
//                                 entity.getWorkingHours() != null
//                                         ? entity.getWorkingHours()
//                                         : "00:00"
//                         );
//
//                         clinicDto.setIdleTime(
//                                 entity.getIdleTime() != null
//                                         ? entity.getIdleTime()
//                                         : "00:00"
//                         );
//
//                         // LOGIN
//                         if (entity.getLogin() != null) {
//
//                             TimeLocationDTO login =
//                                     new TimeLocationDTO();
//
//                             login.setTime(entity.getLogin().getTime());
//                             login.setLatitude(entity.getLogin().getLatitude());
//                             login.setLongtitude(entity.getLogin().getLongtitude());
//
//                             login.setLocation(
//                                     getCityFromLatLong(
//                                             entity.getLogin().getLatitude(),
//                                             entity.getLogin().getLongtitude()
//                                     )
//                             );
//
//                             clinicDto.setLogin(login);
//                         }
//
//                         // LOGOUT
//                         if (entity.getLogout() != null) {
//
//                             TimeLocationDTO logout =
//                                     new TimeLocationDTO();
//
//                             logout.setTime(entity.getLogout().getTime());
//                             logout.setLatitude(entity.getLogout().getLatitude());
//                             logout.setLongtitude(entity.getLogout().getLongtitude());
//
//                             logout.setLocation(
//                                     getCityFromLatLong(
//                                             entity.getLogout().getLatitude(),
//                                             entity.getLogout().getLongtitude()
//                                     )
//                             );
//
//                             clinicDto.setLogout(logout);
//                         }
//                     }
//
//                     result.add(clinicDto);
//                 }
//             }
//
//         } catch (Exception e) {
//
//             System.out.println("Clinic admin attendance error: "
//                     + e.getMessage());
//         }

      // =========================================================
      // ADD BRANCH ADMIN
      // =========================================================
      try {

          ResponseEntity<Response> branchResponse =
                  adminServiceClient.getAllBranches(keyCloakTokenStore.getAccess_token());

          if (branchResponse.getBody() != null
                  && branchResponse.getBody().getData() != null) {

              // ✅ BUILD clinicId → clinicName MAP from getAllClinics()
              Map<String, String> clinicNameMap = new HashMap<>();
              try {
                  ResponseEntity<Response> clinicRes =
                          adminServiceClient.getAllClinics(keyCloakTokenStore.getAccess_token());
                  if (clinicRes.getBody() != null
                          && clinicRes.getBody().getData() != null) {
                      List<Map<String, Object>> cls =
                              (List<Map<String, Object>>) clinicRes.getBody().getData();
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
                      (List<Map<String, Object>>) branchResponse.getBody().getData();

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
          System.out.println("Branch admin attendance error: "
                  + e.getMessage());
      }
            // =========================================================
            // LOOP THROUGH ALL USERS
            // =========================================================
            for (DoctorLoginCredentials user : users) {

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

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(404);
        }

        return response;
    }
    
    @Override
    @Secured("ROLE_CLINICADMIN")
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
   
}