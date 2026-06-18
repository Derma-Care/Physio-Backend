package physiotherapydoctor.scheduler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.entity.PhysiotherapyRecord;
import physiotherapydoctor.repository.PhysiotherapydoctorRespository;
import physiotherapydoctor.serviceImpl.FollowUpWhatsAppService;

@Component
@Slf4j
public class FollowUpReminderScheduler {

    @Autowired
    private PhysiotherapydoctorRespository repository;

    @Autowired
    private FollowUpWhatsAppService followUpWhatsAppService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // =====================================================
    // RUNS EVERY DAY AT 7:00 AM IST
    // =====================================================

    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Kolkata")
    public void sendFollowUpReminders() {

        log.info("FollowUp reminder scheduler started at 7 AM");

        try {

            // Tomorrow's date = nextVisitDate we're looking for
            String tomorrowDate = LocalDate.now()
                    .plusDays(1)
                    .format(FORMATTER);

            log.info("Looking for follow-ups scheduled on: {}", tomorrowDate);

            // Fetch all records whose nextVisitDate == tomorrow
            List<PhysiotherapyRecord> records =
                    repository.findByFollowUpNextVisitDate(tomorrowDate);

            if (records == null || records.isEmpty()) {
                log.info("No follow-up reminders to send for {}", tomorrowDate);
                return;
            }

            log.info("Found {} follow-up(s) to remind for {}",
                    records.size(), tomorrowDate);

            for (PhysiotherapyRecord record : records) {
                try {

                    followUpWhatsAppService.sendFollowUpReminder(
                            record,
                            tomorrowDate
                    );

                    log.info("Reminder sent for bookingId={}",
                            record.getBookingId());

                } catch (Exception e) {
                    // One failure must NOT stop the rest
                    log.error("Reminder failed for bookingId={} : {}",
                            record.getBookingId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("FollowUp scheduler error: {}", e.getMessage(), e);
        }
    }
}