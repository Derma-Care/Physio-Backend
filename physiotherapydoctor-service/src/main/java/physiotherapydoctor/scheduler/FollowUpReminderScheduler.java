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
 // RUNS EVERY DAY AT 09:00 AM IST
 // =====================================================

 @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
 public void sendFollowUpReminders() {

     log.info("FollowUp reminder scheduler started at 9:00 AM");

     try {

         String tomorrowDate = LocalDate.now()
                 .plusDays(1)
                 .format(FORMATTER);

         log.info("Looking for follow-ups scheduled on: {}", tomorrowDate);

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
                 log.error("Reminder failed for bookingId={} : {}",
                         record.getBookingId(), e.getMessage());
             }
         }

     } catch (Exception e) {
         log.error("FollowUp scheduler error: {}", e.getMessage(), e);
     }
 }
}