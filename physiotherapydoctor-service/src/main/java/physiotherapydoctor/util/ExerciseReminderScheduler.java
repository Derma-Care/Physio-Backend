package physiotherapydoctor.util;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import physiotherapydoctor.serviceImpl.ExerciseReminderService;

@Component
@RequiredArgsConstructor
public class ExerciseReminderScheduler {

    private final ExerciseReminderService exerciseReminderService;

    @Scheduled(cron = "0 0 7 * * ?")
    public void sendReminders() {

        exerciseReminderService.processExerciseReminders();
    }
}
