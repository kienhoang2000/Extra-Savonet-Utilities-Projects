package com.example.demo1.service.impl;

import com.example.demo1.service.DeleteUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteUserImpl implements DeleteUser {
    private final RestTemplate restTemplate;

    @Value("${api.delete.users.url}")
    private String url;

    @Override
    public void deleteUsers(MultipartFile[] file, HttpServletRequest request) {
//        //get token from request
//        String token = getToken(request);
//        // get list UserIds from file
//        List<String> userIds = getUserIds(file);
//        // process list UserIds
//        processUserIds(userIds,token);
    }

    @Override
    public void scheduleDeleteUsers() {
        // folder containing files
        String directoryPath = "C:\\Project\\fileTest";

        File directory = new File(directoryPath);
        // get all files in folder
        File[] files = directory.listFiles();
        // get token from file
        String token = getToken(files);
        // check if token is empty
        if(ObjectUtils.isEmpty(token)){
            log.error("Token is empty");
            return;
        }
        // get list UserIds from file
        List<String> userIds = getUserIds(files);

        processDeleteUser(userIds, token , directoryPath);

    }

    private void deleteFiles(String directoryPath, boolean error401, List<String> userIdFail) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(directoryPath))) {
            for (Path entry : stream) {
                BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                // if error 401, delete all files in folder
                if(error401 ) {
                    if (attrs.isRegularFile()) {
                        Files.delete(entry);
                    }
                }
                // if error 429, delete all files in folder except file contain token
                else if (!CollectionUtils.isEmpty(userIdFail)){
                    if (attrs.isRegularFile() && !entry.getFileName().toString().contains("token")) {
                        Files.delete(entry);
                    }
                }
                // if success, delete all files in folder
                else {
                    if (attrs.isRegularFile()) {
                        Files.delete(entry);
                    }
                }
            }
            log.info("All files in folder " + directoryPath + " have been deleted.");
        } catch (IOException e) {
            log.info("Error while deleting files in folder: " + e.getMessage());
        }
    }

    private List<String> getUserIds(File[] files) {
        List<String> userId = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.getName().contains("token") ){
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            userId.add(line);
                        }

                    } catch (IOException e) {
                        log.error("error when reading file ");
                    }
                }
            }
        }
        return userId;
    }

    private String getToken(File[] files) {
        for (File file : files) {
            if (file.isFile()) {
                if (file.getName().contains("token")) {
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            return line;
                        }

                    } catch (IOException e) {
                        log.error("error when reading file ");
                    }
                }
                break;
            }
        }
        return  "";
    }

    public void processDeleteUser(List<String> userIds, String token , String directoryPath)  {
        // split list UserIds into batches
            int batchSize = 1000;
            List<List<String>> batches = new ArrayList<>();
            List<String> userIdFail = new ArrayList<>();
            AtomicBoolean error401 = new AtomicBoolean(false);
            for (int i = 0; i < userIds.size(); i += batchSize) {
                int end = Math.min(i + batchSize, userIds.size());
                batches.add(userIds.subList(i, end));
            }
        // create a thread pool with 10 threads
           Executor executor = Executors.newFixedThreadPool(10);
            //

           List<CompletableFuture> deleteUser = batches.stream()
                   .map(b -> CompletableFuture.runAsync(() -> callApi(b, token,userIdFail,error401), executor))
                   .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
           // wait for all futures to complete
              CompletableFuture.allOf(deleteUser.toArray(new CompletableFuture[0])).join();
            if(error401.get() && !CollectionUtils.isEmpty(userIdFail)){
                log.error("error 401");
                // when error 401 and userIdFail is not empty, delete all files in folder
                deleteFiles(directoryPath , error401.get(), userIdFail);
                // after that write userIdFail to csv file in folder
                writeListToCsv(userIdFail);
            }else if (!error401.get() && !CollectionUtils.isEmpty(userIdFail)){
                // when error 429 and userIdFail is not empty, delete all files in folder except file contain token
                deleteFiles(directoryPath , error401.get(), userIdFail);
                writeListToCsv(userIdFail);
            }else if (CollectionUtils.isEmpty(userIdFail)){
                // when success, delete all files in folder
                deleteFiles(directoryPath , error401.get() , userIdFail);
            }
    }

    private void writeListToCsv(List<String> userIdFail) {
        String filePath = "C:\\Project\\fileTest\\userIdFail401.csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : userIdFail) {
                if(!ObjectUtils.isEmpty(line)) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            log.info("Write to CSV file successfully");
        } catch (IOException e) {
            log.info("Error writing to CSV file: " + e.getMessage());
        }
    }




    public void callApi(List<String> userIds, String token,List<String> userIdFail , AtomicBoolean error401 )  {
        for (int i = 0; i <=  userIds.size() ; i++) {
            // setup delete url
            String deleteUrl = url + userIds.get(i);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            try {
                // call delete api
                ResponseEntity<String> response = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);
                log.info("user " + userIds.get(i) + " is deleted passed with status code " + response.getStatusCodeValue());
            } catch (Exception e) {
                    // check if status code is 429 or 401
                    Integer statusCode = ((HttpClientErrorException) e).getRawStatusCode();
                    if(statusCode == 429) {
                        if(!ObjectUtils.isEmpty((userIds.get(i))))
                            userIdFail.add(userIds.get(i));
                    }else if (statusCode == 401) {
                        error401.set(true);
                        if(!ObjectUtils.isEmpty((userIds.get(i))))
                            userIdFail.addAll(userIds.subList(i, userIds.size()));
                        break;
                    }
                   log.error("user " + userIds.get(i)  + " : fail is : " + e.getMessage() );
                }
        }
    }

//    private List<String> getUserIds(MultipartFile[] file) {
//        List<String> UserIds = new ArrayList<>();
//        for (MultipartFile f : file) {
//            try (BufferedReader br = new BufferedReader(new InputStreamReader(f.getInputStream()))) {
//                String UserId;
//                while ((UserId = br.readLine()) != null) {
//                    if(!ObjectUtils.isEmpty(UserId) ){
//                        UserIds.add(UserId);
//                    }
//                }
//            } catch (IOException e) {
//                log.error("error when reading file " );
//            }
//        }
//        return UserIds;
//    }

//    private String getToken(HttpServletRequest request) {
//        String bearerToken = request.getHeader("Authorization");
//        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){
//            return bearerToken.substring(7);
//
//        }
//        return null;
//    }
}
