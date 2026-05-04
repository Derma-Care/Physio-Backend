package com.clinicadmin.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.clinicadmin.dto.ActivityDTO;
import com.clinicadmin.dto.AttendanceDTO;
import com.clinicadmin.dto.DailyAttendanceResponseDTO;
import com.clinicadmin.dto.MonthlyAttendanceResponseDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.TimeLocationDTO;
import com.clinicadmin.entity.Activity;
import com.clinicadmin.entity.Attendance;
import com.clinicadmin.entity.TimeLocation;
import com.clinicadmin.repository.AttendanceRepository;
import com.clinicadmin.service.AttendanceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository repo;

    @Override
    public Response save(AttendanceDTO dto) {

        Response response = new Response();

        try {

            if (dto.getUserId() == null || dto.getDate() == null) {
                throw new RuntimeException("userId and date are required");
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
                    act.setLocation(a.getLocation());

                    entity.getActivities().add(act);
                }
            }

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

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(400);
        }

        return response;
    }

    @Override
    public Response updateActivity(AttendanceDTO dto) {

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

            // ✅ LOGIN UPDATE (FLAT FIELDS)
            if (dto.getLoginTime() != null || dto.getLoginLocation() != null) {

                if (entity.getLogin() == null) entity.setLogin(new TimeLocation());

                if (dto.getLoginTime() != null)
                    entity.getLogin().setTime(dto.getLoginTime());

                if (dto.getLoginLocation() != null)
                    entity.getLogin().setLocation(dto.getLoginLocation());

                entity.setStatus("LOGGED_IN");
                updated = true;
            }

            if (dto.getLogoutTime() != null || dto.getLogoutLocation() != null) {

                if (entity.getLogout() == null) entity.setLogout(new TimeLocation());

                if (dto.getLogoutTime() != null)
                    entity.getLogout().setTime(dto.getLogoutTime());

                if (dto.getLogoutLocation() != null)
                    entity.getLogout().setLocation(dto.getLogoutLocation());

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

            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(400);
        }

        return response;
    }
    @Override
    public Response getDaily(String userId, String date) {

        Response response = new Response();

        try {

            Attendance entity = repo.findByUserIdAndDate(userId, date)
                    .orElseThrow(() -> new RuntimeException("No data found"));

            DailyAttendanceResponseDTO dto = new DailyAttendanceResponseDTO();

            dto.setDate(entity.getDate());
            dto.setLogTime(entity.getLogTime());
            dto.setStatus(entity.getStatus());

            if (entity.getLogin() != null) {
                TimeLocationDTO login = new TimeLocationDTO();
                login.setTime(entity.getLogin().getTime());
                login.setLocation(entity.getLogin().getLocation());
                dto.setLogin(login);
            }

            if (entity.getLogout() != null) {
                TimeLocationDTO logout = new TimeLocationDTO();
                logout.setTime(entity.getLogout().getTime());
                logout.setLocation(entity.getLogout().getLocation());
                dto.setLogout(logout);
            }

            if (entity.getActivities() != null) {
                List<ActivityDTO> activities = entity.getActivities()
                        .stream()
                        .map(a -> {
                            ActivityDTO ad = new ActivityDTO();
                            ad.setActivityId(a.getActivityId()); 
                            ad.setActivity(a.getActivity());
                            ad.setDuration(a.getDuration());
                            ad.setLocation(a.getLocation());
                            return ad;
                        }).collect(Collectors.toList());

                dto.setActivities(activities);
            }

            response.setSuccess(true);
            response.setMessage("Daily report fetched successfully");
            response.setData(dto);
            response.setStatus(200); // ✅ FIX

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setStatus(404); // ✅ FIX
        }

        return response;
    }

    @Override
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

        if (dto.getLogin() != null) {
            TimeLocation login = new TimeLocation();
            login.setTime(dto.getLogin().getTime());
            login.setLocation(dto.getLogin().getLocation());
            entity.setLogin(login);
        }

        if (dto.getLogout() != null) {
            TimeLocation logout = new TimeLocation();
            logout.setTime(dto.getLogout().getTime());
            logout.setLocation(dto.getLogout().getLocation());
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
            login.setLocation(entity.getLogin().getLocation());
            dto.setLogin(login);
        }

        // 🔹 LOGOUT
        if (entity.getLogout() != null) {
            TimeLocationDTO logout = new TimeLocationDTO();
            logout.setTime(entity.getLogout().getTime());
            logout.setLocation(entity.getLogout().getLocation());
            dto.setLogout(logout);
        }

        
        if (entity.getActivities() != null && !entity.getActivities().isEmpty()) {

            List<ActivityDTO> activities = entity.getActivities().stream().map(a -> {

                ActivityDTO act = new ActivityDTO();

                act.setActivityId(a.getActivityId());
                act.setActivity(a.getActivity());
                act.setDuration(a.getDuration());
                act.setLocation(a.getLocation());

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

   
}