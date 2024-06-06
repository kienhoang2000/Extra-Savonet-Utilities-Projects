package com.example.demo1.service;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;

public interface DeleteUser {
    void deleteUsers(MultipartFile[] file, HttpServletRequest request) throws ExecutionException, InterruptedException;

    void scheduleDeleteUsers();
}
