package com.example.demo1.schedule;

import com.example.demo1.dto.DemoDto;
import com.example.demo1.service.DeleteUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeleteUserSchedule {

    private final DeleteUser deleteUser;

    // 30 minutes of operation once
    @Scheduled(cron = "*/30 * * * * *")
    public void deleteUser() {
        deleteUser.scheduleDeleteUsers();
    }
}
