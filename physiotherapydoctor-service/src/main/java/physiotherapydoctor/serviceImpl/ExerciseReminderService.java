package physiotherapydoctor.serviceImpl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.ExerciseInfo;
import physiotherapydoctor.dto.HomeExercise;
import physiotherapydoctor.dto.response.PaymentRecordResponse;
import physiotherapydoctor.entity.PaymentRecord;
import physiotherapydoctor.entity.PhysiotherapyRecord;
import physiotherapydoctor.feign.NotificationFeign;
import physiotherapydoctor.repository.PaymentRepository;
import physiotherapydoctor.repository.PhysiotherapydoctorRespository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseReminderService {

    private final PaymentRepository paymentRepository;
    private final PhysiotherapydoctorRespository physiotherapyRecordRepository;
    private final NotificationFeign notificationFeign;

    public void processExerciseReminders() {

        LocalDate today = LocalDate.now();
        LocalDate oneYearAgo = today.minusYears(1);

       List<ExerciseInfo> reminders = new ArrayList<>();

        List<PaymentRecord> payments = paymentRepository.findAll()
                .stream()
                .filter(payment -> {

                    if (payment.getSessionStartDate() == null
                            || payment.getSessionEndDate() == null) {
                        return false;
                    }

                    LocalDate startDate =
                            LocalDate.parse(payment.getSessionStartDate());

                    LocalDate endDate =
                            LocalDate.parse(payment.getSessionEndDate());

                    return !startDate.isBefore(oneYearAgo)
                            && !startDate.isAfter(today)
                            && !endDate.isBefore(today);
                })
                .toList();

        for (PaymentRecord payment : payments) {

            Optional<PhysiotherapyRecord> physioOpt =
                    physiotherapyRecordRepository.findByTherapistRecordId(
                            payment.getTherapistRecordId());

            if (physioOpt.isEmpty()) {
                continue;
            }

            PhysiotherapyRecord record = physioOpt.get();

            if (record.getExercisePlan() == null
                    || record.getExercisePlan().getHomeExercises() == null
                    || record.getExercisePlan().getHomeExercises().isEmpty()) {
                continue;
            }

            LocalDate sessionStartDate =
                    LocalDate.parse(payment.getSessionStartDate());

            LocalDate sessionEndDate =
                    LocalDate.parse(payment.getSessionEndDate());

            for (HomeExercise exercise :
                    record.getExercisePlan().getHomeExercises()) {

                ExerciseInfo info = ExerciseInfo.builder()
                        .patientId(payment.getPatientId())
                        .exerciseId(exercise.getId())
                        .exerciseName(exercise.getName())
                        .frequency(exercise.getFrequency())
                        .sessionStartDate(sessionStartDate)
                        .sessionEndDate(sessionEndDate)
                        .build();

                if (shouldSendReminder(info)) {
                    reminders.add(info);
                }
            }
        }

        if (!reminders.isEmpty()) {

            notificationFeign.sendBulkExerciseReminders(reminders);

            log.info("Exercise reminders sent : {}", reminders.size());

        } else {

            log.info("No exercise reminders for today");
        }
    }

    private boolean shouldSendReminder(ExerciseInfo info) {

        LocalDate today = LocalDate.now();

        LocalDate startDate = info.getSessionStartDate();
        LocalDate endDate = info.getSessionEndDate();

        if (today.isBefore(startDate) || today.isAfter(endDate)) {
            return false;
        }

        String frequency = info.getFrequency();

        if (frequency == null || frequency.isBlank()) {
            return false;
        }

        frequency = frequency.toLowerCase().trim();

        Matcher matcher =
                Pattern.compile("(\\d+)").matcher(frequency);

        if (!matcher.find()) {
            return false;
        }

        int count = Integer.parseInt(matcher.group());

        // DAY
        if (frequency.contains("day")) {
            return handleDailyFrequency(
                    count,
                    startDate,
                    today);
        }

        // WEEK
        if (frequency.contains("week")) {
            return handleWeeklyFrequency(
                    count,
                    startDate,
                    today);
        }

        // MONTH
        if (frequency.contains("month")) {
            return handleMonthlyFrequency(
                    count,
                    startDate,
                    today);
        }

        return false;
    }

    private boolean handleDailyFrequency(
            int count,
            LocalDate startDate,
            LocalDate today) {

        long daysBetween =
                ChronoUnit.DAYS.between(startDate, today);

        if (count <= 1) {
            return true;
        }

        return daysBetween % count == 0;
    }

    private boolean handleWeeklyFrequency(
            int count,
            LocalDate startDate,
            LocalDate today) {

        long totalDays =
                ChronoUnit.DAYS.between(startDate, today);

        long dayInWeekCycle = totalDays % 7;

        if (count == 1) {

            return dayInWeekCycle == 0;
        }

        /*
         * Examples
         *
         * 2/week -> days 3,6
         * 3/week -> days 2,4,6
         * 4/week -> days 1,3,5,6
         */

        Set<Long> reminderDays = new HashSet<>();

        for (int i = 1; i <= count; i++) {

            long reminderDay =
                    Math.round((i * 7.0) / count);

            if (reminderDay >= 7) {
                reminderDay = 6;
            }

            reminderDays.add(reminderDay);
        }

        return reminderDays.contains(dayInWeekCycle);
    }

    private boolean handleMonthlyFrequency(
            int count,
            LocalDate startDate,
            LocalDate today) {

        long monthsBetween =
                ChronoUnit.MONTHS.between(
                        YearMonth.from(startDate),
                        YearMonth.from(today));

        if (count <= 1) {

            return startDate.getDayOfMonth()
                    == today.getDayOfMonth();
        }

        /*
         * 2/month
         * 3/month
         */

        int daysInMonth = today.lengthOfMonth();

        Set<Integer> reminderDays = new HashSet<>();

        for (int i = 1; i <= count; i++) {

            int reminderDay =
                    (int) Math.round(
                            (i * daysInMonth) / (double) count);

            reminderDays.add(reminderDay);
        }

        return monthsBetween >= 0
                && reminderDays.contains(
                        today.getDayOfMonth());
    }
}
