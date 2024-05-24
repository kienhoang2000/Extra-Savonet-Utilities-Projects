package com.example.demo1.service.impl;

import com.example.demo1.service.DeleteUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteUserImpl implements DeleteUser {
    private final RestTemplate restTemplate;

    @Value("${api.delete.users.url}")
    private String url;

    @Override
    public void deleteUsers(MultipartFile[] file, HttpServletRequest request) {
        //get token from request
        String token = getToken(request);
        // TODO: Insert condition if we don't have token, do not continue and return a alert!

        // get list UserIds from file
        List<String> userIds = getUserIds(file);
        // process list UserIds
        processUserIds(userIds, token);
    }

    public void processUserIds(List<String> userIds, String token) {
        Instant startTime = Instant.now();
        log.info("Start time: {}", startTime);

        int round = 1;
        List<String> successList = new ArrayList<>();
        List<String> failedList = new ArrayList<>();

        while (successList.size() < userIds.size()) {
            List<List<String>> batches = new ArrayList<>();
            List<String> listNeedToBeSeparate = userIds;
            if (!failedList.isEmpty()) {
                log.info("---Restart round {} with {} failed users at {}", ++round, failedList.size(), Instant.now());
                listNeedToBeSeparate = failedList;
            }
            int batchSize = listNeedToBeSeparate.size() / 4;
            for (int i = 0; i < listNeedToBeSeparate.size(); i += batchSize) {
                int end = Math.min(i + batchSize, listNeedToBeSeparate.size());
                batches.add(listNeedToBeSeparate.subList(i, end));
            }

            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<CompletableFuture> futures = batches.stream()
                    .map(b -> CompletableFuture.runAsync(() -> callApi(b, token, successList, failedList), executorService))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        Instant endTime = Instant.now();
        log.info("===>End time: {}", endTime);
        log.info("=====>Total duration: {}ms", Duration.between(startTime, endTime).toMillis());
    }

    public void callApi(List<String> userIds, String token, List<String> successList, List<String> failedList) throws RuntimeException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        Instant taskStartTime = Instant.now();

        for (String userId : userIds) {
            String fullURL = url + userId;
            int maxAttempts = 3;
            int attempts = 0;
            boolean success = false;

            while (!success && attempts < maxAttempts) {
                try {
                    ResponseEntity<String> response = restTemplate.exchange(fullURL, HttpMethod.DELETE, entity, String.class);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("User {} deletion passed with status code {}", userId, response.getStatusCodeValue());
                        successList.add(userId);
                        success = true;
                    } else {
                        throw new Exception("" + response.getStatusCodeValue());
                    }
                } catch (Exception e) {
                    log.error("Attempt {} failed for user {}: {}", attempts + 1, userId, e.getMessage());
                    if (attempts < maxAttempts - 1) {
                        try {
                            Thread.sleep(3000 + (long) (Math.random() * 2000)); // Random delay between 3000ms to 5000ms
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt(); // Restore interrupted status
                            log.error("Thread interrupted during retry delay", ie);
                        }
                    }
                }
                attempts++;
            }

            if (!success) {
                failedList.add(userId);
            }
        }
        Instant taskEndTime = Instant.now();
        log.info("----Task start time: {}, end time: {}, duration: {}ms", taskStartTime, taskEndTime, Duration.between(taskStartTime, taskEndTime).toMillis());
    }

    private List<String> getUserIds(MultipartFile[] file) {
        List<String> userIds = new ArrayList<>();
        for (MultipartFile f : file) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(f.getInputStream()))) {
                String UserId;
                while ((UserId = br.readLine()) != null) {
                    if (!ObjectUtils.isEmpty(UserId)) {
                        userIds.add(UserId);
                    }
                }
            } catch (IOException e) {
                log.error("error when reading file ");
            }
        }
        return userIds;
    }

    private String getToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
