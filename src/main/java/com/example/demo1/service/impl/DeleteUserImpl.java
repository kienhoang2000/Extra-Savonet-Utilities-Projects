package com.example.demo1.service.impl;

import com.example.demo1.service.DeleteUser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


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
        // get list UserIds from file
        List<String> userIds = getUserIds(file);
        // process list UserIds
        processUserIds(userIds,token);
    }

    public void processUserIds(List<String> userIds, String token)  {
        int batchSize = 260;
        List<List<String>> batches = new ArrayList<>();

        for (int i = 0; i < userIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, userIds.size());
            batches.add(userIds.subList(i, end));
        }

           Executor executor = Executors.newFixedThreadPool(1000);
           List<CompletableFuture> futures = batches.stream()
                   .map(b -> CompletableFuture.runAsync(() -> callApi(b, token), executor))
                   .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
           // wait for all futures to complete
              CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }


    public void callApi(List<String> userIds, String token )  {
//         call api to delete users
        for (String userId : userIds) {
            String deleteUrl = url + userId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            try {
                ResponseEntity<String> response = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);
                log.info("user " + userId + " is deleted passed with status code " + response.getStatusCodeValue());
            } catch (Exception e) {
                log.error("user " + userId  + " : fail is : " + e.getMessage() );
            }
        }
    }

    private List<String> getUserIds(MultipartFile[] file) {
        List<String> UserIds = new ArrayList<>();
        for (MultipartFile f : file) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(f.getInputStream()))) {
                String UserId;
                while ((UserId = br.readLine()) != null) {
                    if(!ObjectUtils.isEmpty(UserId) ){
                        UserIds.add(UserId);
                    }
                }
            } catch (IOException e) {
                log.error("error when reading file " );
            }
        }
        return UserIds;
    }

    private String getToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);

        }
        return null;
    }
}
