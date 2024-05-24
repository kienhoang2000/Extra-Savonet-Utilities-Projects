package com.example.demo1.controller;

import com.example.demo1.service.DeleteUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;


@RestController
@RequiredArgsConstructor
public class DeleteUserController {


    private final DeleteUser deleteUser;
    @PostMapping("/deleteUser")
    public String callApi(@RequestParam("file") MultipartFile[] file, HttpServletRequest request) throws ExecutionException, InterruptedException {
        deleteUser.deleteUsers(file,request);
        return "success";
    }
}

