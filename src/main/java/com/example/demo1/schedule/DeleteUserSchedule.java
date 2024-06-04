package com.example.demo1.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteUserSchedule {
    @Scheduled(cron = "*/30 * * * * *")
    public void deleteUser() {
        System.out.println("Delete user schedule");
    }
}
